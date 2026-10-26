#!/usr/bin/env python3
"""Analyze native vs 48 kHz TTS evidence without Java/scipy dependencies.

The script intentionally does not generate evidence. Run it after the test has
populated audio-evidence/wav and the corresponding MP3 directories.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

import numpy as np

COMMON_RATE = 8000
DURATION_TOLERANCE_SEC = 0.5 / 48000.0
CORR_THRESHOLD = 0.99
F0_CENTS_THRESHOLD = 5.0
LAG_THRESHOLD_SAMPLES = 1  # one sample at the common physical rate (8 kHz)


def run(cmd: list[str], *, input_bytes: bytes | None = None) -> subprocess.CompletedProcess:
    return subprocess.run(cmd, input=input_bytes, stdout=subprocess.PIPE,
                          stderr=subprocess.PIPE, check=True)


def ffprobe(path: Path) -> dict:
    p = run(["ffprobe", "-v", "error", "-select_streams", "a:0",
             "-show_entries", "stream=sample_rate,channels,sample_fmt,duration",
             "-of", "json", str(path)])
    data = json.loads(p.stdout.decode("utf-8"))
    streams = data.get("streams", [])
    if not streams:
        raise RuntimeError(f"no audio stream: {path}")
    return streams[0]


def wav_info(path: Path) -> dict:
    with wave.open(str(path), "rb") as w:
        return {"sample_rate": w.getframerate(), "channels": w.getnchannels(),
                "sample_width": w.getsampwidth(), "frames": w.getnframes(),
                "duration_sec": w.getnframes() / w.getframerate()}


def decode_8k(path: Path) -> np.ndarray:
    p = run(["ffmpeg", "-v", "error", "-i", str(path), "-ac", "1", "-ar",
             str(COMMON_RATE), "-f", "f32le", "pipe:1"])
    x = np.frombuffer(p.stdout, dtype="<f4").astype(np.float64)
    return np.nan_to_num(x)


def stem_key(path: Path) -> str:
    s = path.stem.lower()
    s = re.sub(r"(?:[-_.](?:native|on|48k|48000))$", "", s)
    return s


def variant(path: Path) -> str | None:
    text = (path.stem + " " + " ".join(path.parts)).lower()
    if re.search(r"(?:^|[-_. /])(?:48k|48000)(?:$|[-_. /])", text):
        return "48k"
    if re.search(r"(?:^|[-_. /])(?:native|on)(?:$|[-_. /])", text):
        return "native"
    return None


def collect(directory: Path, suffix: str) -> dict[str, Path]:
    out: dict[str, Path] = {}
    for p in sorted(directory.rglob(f"*{suffix}")) if directory.exists() else []:
        v = variant(p)
        if v:
            out[f"{stem_key(p)}::{v}"] = p
    return out


def pair_files(wav_dir: Path, mp3_root: Path) -> list[dict]:
    wavs = collect(wav_dir, ".wav")
    mp3s = collect(mp3_root, ".mp3")
    keys = sorted({k.split("::", 1)[0] for k in wavs} | {k.split("::", 1)[0] for k in mp3s})
    return [{"key": k, "wav_native": wavs.get(k + "::native"),
             "wav_48k": wavs.get(k + "::48k"), "mp3_native": mp3s.get(k + "::native"),
             "mp3_48k": mp3s.get(k + "::48k")} for k in keys]


def normalized_corr_lag(a: np.ndarray, b: np.ndarray) -> tuple[float | None, int | None]:
    n = min(len(a), len(b))
    if n < 32:
        return None, None
    a, b = a[:n], b[:n]
    a -= np.mean(a); b -= np.mean(b)
    den = np.linalg.norm(a) * np.linalg.norm(b)
    if den == 0:
        return None, None
    # Search a conservative small window: a timing offset greater than this is
    # reported as non-compliant rather than hidden by arbitrary alignment.
    best = (-1.0, 0)
    for lag in range(-LAG_THRESHOLD_SAMPLES - 1, LAG_THRESHOLD_SAMPLES + 2):
        if lag >= 0:
            x, y = a[lag:], b[:n-lag] if lag else b
        else:
            x, y = a[:n+lag], b[-lag:]
        d = np.linalg.norm(x) * np.linalg.norm(y)
        if d:
            c = float(np.dot(x, y) / d)
            if c > best[0]: best = (c, lag)
    return best


def f0_frames(x: np.ndarray) -> tuple[np.ndarray, np.ndarray, int]:
    frame = int(0.030 * COMMON_RATE); hop = int(0.010 * COMMON_RATE)
    indexes: list[int] = []
    values: list[float] = []
    reliable = 0
    if len(x) < frame: return np.array([], dtype=int), np.array([], dtype=float), 0
    # Speech F0 range and conservative voicing gates; autocorrelation peak must
    # be strong and not merely a low-frequency/noise maximum.
    lo, hi = int(COMMON_RATE / 400), int(COMMON_RATE / 60)
    for start in range(0, len(x) - frame + 1, hop):
        z = x[start:start + frame].astype(float)
        z -= z.mean(); rms = float(np.sqrt(np.mean(z*z)))
        if rms < 1e-4: continue
        ac = np.correlate(z, z, mode="full")[frame-1:]
        energy = float(ac[0])
        ac[0] = 0
        segment = ac[lo:hi + 1]
        if not len(segment): continue
        lag = lo + int(np.argmax(segment)); strength = float(ac[lag] / (ac[0] if ac[0] else 1))
        strength = float(ac[lag] / energy) if energy > 0 else 0.0
        if strength < 0.35: continue
        # Parabolic interpolation around the autocorrelation peak.
        delta = 0.0
        if 1 <= lag < len(ac)-1:
            den = ac[lag-1] - 2*ac[lag] + ac[lag+1]
            if den != 0: delta = 0.5 * (ac[lag-1] - ac[lag+1]) / den
        f0 = COMMON_RATE / (lag + delta)
        if 60 <= f0 <= 400:
            indexes.append(start // hop)
            values.append(float(f0)); reliable += 1
    return np.asarray(indexes, dtype=int), np.asarray(values), reliable


def analyze_pair(pair: dict) -> dict:
    result = {"id": pair["key"], "files": {k: str(v) if v else None for k, v in pair.items() if k != "key"}, "checks": {}}
    if not pair["wav_native"] or not pair["wav_48k"]:
        result["status"] = "missing_wav"
        return result
    wn, w48 = wav_info(pair["wav_native"]), wav_info(pair["wav_48k"])
    result["wav_info"] = {"native": wn, "48k": w48}
    result["checks"]["wav_rates"] = w48["sample_rate"] == 48000
    dd = abs(wn["duration_sec"] - w48["duration_sec"])
    result["duration_difference_sec"] = dd
    result["checks"]["duration_within_half_48k_sample"] = dd <= DURATION_TOLERANCE_SEC + 1e-12
    a, b = decode_8k(pair["wav_native"]), decode_8k(pair["wav_48k"])
    corr, lag = normalized_corr_lag(a, b)
    result["waveform_8k"] = {"correlation": corr, "lag_samples_at_8000": lag, "lag_seconds": (lag / COMMON_RATE if lag is not None else None)}
    result["checks"]["correlation_ge_0_99"] = corr is not None and corr >= CORR_THRESHOLD
    result["checks"]["latency_within_one_common_sample"] = lag is not None and abs(lag) <= LAG_THRESHOLD_SAMPLES
    ina, fna, rn = f0_frames(a); i48, f48, r48 = f0_frames(b)
    f0 = None
    common_indexes, native_positions, f48_positions = np.intersect1d(ina, i48, return_indices=True)
    n = len(common_indexes)
    result["f0_reliable_frame_counts"] = {"native": int(rn), "48k": int(r48), "paired_intersection": int(n), "minimum_paired_required": 10}
    if n >= 10:
        native_f0 = fna[native_positions]; paired_48k_f0 = f48[f48_positions]
        cents = 1200 * np.log2(native_f0 / paired_48k_f0)
        f0 = {"reliable_voiced_frames_compared": int(n), "reliable_frame_indexes_compared": common_indexes.tolist(), "native_f0_hz_per_frame": native_f0.tolist(), "48k_f0_hz_per_frame": paired_48k_f0.tolist(), "native_median_hz": float(np.median(native_f0)), "48k_median_hz": float(np.median(paired_48k_f0)), "median_absolute_cents_deviation": float(np.median(np.abs(cents))), "reliable_native_frames": rn, "reliable_48k_frames": r48}
    result["f0_status"] = "measured" if f0 is not None else "unavailable"
    result["f0_autocorrelation"] = f0
    result["checks"]["f0_median_abs_cents_le_5_when_measured"] = (f0 is None or f0["median_absolute_cents_deviation"] <= F0_CENTS_THRESHOLD)
    result["status"] = "pass" if all(result["checks"].values()) else "fail"
    return result


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--evidence-dir", type=Path, default=Path("/workspace/old-tts-1.4.1-delivery/audio-evidence"))
    ap.add_argument("--wav-dir", type=Path, default=None)
    ap.add_argument("--mp3-dir", type=Path, default=None)
    ap.add_argument("--json", type=Path, default=None)
    args = ap.parse_args()
    wav_dir = args.wav_dir or args.evidence_dir / "wav"
    mp3_dir = args.mp3_dir or args.evidence_dir
    if not wav_dir.is_dir():
        ap.error(f"WAV directory does not exist yet: {wav_dir} (run only after evidence is produced)")
    pairs = pair_files(wav_dir, mp3_dir)
    # ffprobe every discovered MP3, even if its pair is incomplete, as requested.
    mp3_probe = []
    for p in sorted(mp3_dir.rglob("*.mp3")) if mp3_dir.exists() else []:
        try: info = ffprobe(p); info["path"] = str(p); mp3_probe.append(info)
        except Exception as e: mp3_probe.append({"path": str(p), "error": str(e)})
    results = [analyze_pair(p) for p in pairs]
    for r in results:
        for field in ("mp3_native", "mp3_48k"):
            path = r["files"].get(field)
            if path:
                try: r.setdefault("mp3_probe", {})[field] = ffprobe(Path(path))
                except Exception as e: r.setdefault("mp3_probe", {})[field] = {"error": str(e)}
        probes = r.get("mp3_probe", {})
        wav_info = r.get("wav_info")
        native_rate = (wav_info or {}).get("native", {}).get("sample_rate")
        expected_native_mp3_rate = 11025 if native_rate == 10000 else native_rate
        r["mp3_format_note"] = "Klatt PCM is 10000 Hz; MP3 cannot represent 10000 Hz. LAME resamples it to 11025 Hz without reinterpreting its original rate." if native_rate == 10000 else None
        r["checks"]["mp3_rates_match_wavs"] = bool(wav_info) and all((k in probes and "error" not in probes[k] and int(probes[k].get("sample_rate", 0)) == (expected_native_mp3_rate if k == "mp3_native" else 48000)) for k in ("mp3_native", "mp3_48k"))
        if r["status"] == "pass" and not r["checks"]["mp3_rates_match_wavs"]: r["status"] = "fail"
    summary = {"pair_count": len(results), "expected_pair_count": 29, "all_29_present": len(results) == 29, "pass": sum(r["status"] == "pass" for r in results), "fail": sum(r["status"] == "fail" for r in results), "missing_wav": sum(r["status"] == "missing_wav" for r in results), "limits": {"duration_seconds": DURATION_TOLERANCE_SEC, "correlation": CORR_THRESHOLD, "f0_median_absolute_cents": F0_CENTS_THRESHOLD, "latency_common_samples": LAG_THRESHOLD_SAMPLES, "common_rate_hz": COMMON_RATE}, "f0_note": "No F0 pass/fail penalty when no reliable voiced frames exist; F0 is autocorrelation-based and can be unavailable for unvoiced/noisy material."}
    output = {"summary": summary, "mp3_ffprobe_all": mp3_probe, "pairs": results}
    out = args.json or args.evidence_dir / "analysis-48k-audio.json"
    out.parent.mkdir(parents=True, exist_ok=True); out.write_text(json.dumps(output, ensure_ascii=False, indent=2) + "\n")
    print(f"29-pair summary: discovered={len(results)} pass={summary['pass']} fail={summary['fail']} missing_wav={summary['missing_wav']}")
    for r in results: print(f"{r['id']}: {r['status']}")
    print(f"JSON: {out}")
    return 0 if summary["all_29_present"] and summary["fail"] == 0 and summary["missing_wav"] == 0 else 2


if __name__ == "__main__":
    raise SystemExit(main())

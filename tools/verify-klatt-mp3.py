#!/usr/bin/env python3
"""Verify Klatt 10 kHz PCM -> LAME MP3 evidence without human-listening claims.

The report is deliberately limited to machine-measurable format, timing,
waveform-correlation, speed-drift, and autocorrelation-F0 checks.
"""
from __future__ import annotations

import argparse
import json
import subprocess
import sys
import wave
from pathlib import Path

import numpy as np

COMMON_RATE = 8000
WAV_RATE = 10000
MP3_RATE = 11025
MAX_ENCODER_DELAY_SEC = 0.3
MAX_LAG_SAMPLES = int(COMMON_RATE * MAX_ENCODER_DELAY_SEC)
CORR_MIN = 0.95
MAX_DRIFT_SAMPLES = 2
F0_CENTS_MAX = 5.0


def run(cmd: list[str]) -> subprocess.CompletedProcess:
    return subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True)


def probe(path: Path) -> dict:
    p = run(["ffprobe", "-v", "error", "-select_streams", "a:0",
             "-show_entries", "stream=sample_rate,channels,sample_fmt,duration",
             "-of", "json", str(path)])
    streams = json.loads(p.stdout.decode()).get("streams", [])
    if not streams:
        raise RuntimeError(f"ffprobe found no audio stream: {path}")
    s = streams[0]
    if "sample_rate" in s:
        s["sample_rate"] = int(s["sample_rate"])
    return s


def wav_info(path: Path) -> dict:
    with wave.open(str(path), "rb") as w:
        return {"sample_rate": w.getframerate(), "channels": w.getnchannels(),
                "sample_width": w.getsampwidth(), "frames": w.getnframes(),
                "duration_sec": w.getnframes() / w.getframerate()}


def decode_8k(path: Path) -> np.ndarray:
    p = run(["ffmpeg", "-v", "error", "-i", str(path), "-ac", "1", "-ar",
             str(COMMON_RATE), "-f", "f32le", "pipe:1"])
    return np.nan_to_num(np.frombuffer(p.stdout, dtype="<f4").astype(np.float64))


def corr_for_lag(a: np.ndarray, b: np.ndarray, lag: int) -> float | None:
    if lag >= 0:
        x, y = a[lag:], b[:len(a) - lag] if lag else b
    else:
        x, y = a[:len(a) + lag], b[-lag:]
    n = min(len(x), len(y))
    if n < 64:
        return None
    x, y = x[:n] - np.mean(x[:n]), y[:n] - np.mean(y[:n])
    den = float(np.linalg.norm(x) * np.linalg.norm(y))
    return float(np.dot(x, y) / den) if den else None


def best_corr(a: np.ndarray, b: np.ndarray, max_lag: int = MAX_LAG_SAMPLES) -> tuple[float | None, int | None]:
    """FFT-compatible bounded scan; bounded scan is robust at these short clips."""
    n = min(len(a), len(b))
    if n < 64:
        return None, None
    a, b = a[:n], b[:n]
    best: tuple[float | None, int | None] = (None, None)
    for lag in range(-min(max_lag, n - 64), min(max_lag, n - 64) + 1):
        c = corr_for_lag(a, b, lag)
        if c is not None and (best[0] is None or c > best[0]):
            best = (c, lag)
    return best


def aligned(a: np.ndarray, b: np.ndarray, lag: int) -> tuple[np.ndarray, np.ndarray]:
    if lag >= 0:
        n = min(len(a) - lag, len(b)); return a[lag:lag+n], b[:n]
    n = min(len(a), len(b) + lag); return a[:n], b[-lag:-lag+n]


def f0_frames(x: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    frame, hop = int(.030 * COMMON_RATE), int(.010 * COMMON_RATE)
    lo, hi = int(COMMON_RATE / 400), int(COMMON_RATE / 60)
    indexes, values = [], []
    for start in range(0, max(0, len(x) - frame + 1), hop):
        z = x[start:start+frame].astype(float); z -= z.mean()
        energy = float(np.dot(z, z))
        if energy <= 1e-8: continue
        ac = np.correlate(z, z, mode="full")[frame-1:]
        seg = ac[lo:hi+1]
        if not len(seg): continue
        k = lo + int(np.argmax(seg))
        if ac[k] / energy < .35: continue
        delta = 0.0
        if 1 <= k < len(ac)-1:
            den = ac[k-1] - 2*ac[k] + ac[k+1]
            if den: delta = .5 * (ac[k-1] - ac[k+1]) / den
        f0 = COMMON_RATE / (k + delta)
        if 60 <= f0 <= 400:
            indexes.append(start // hop); values.append(float(f0))
    return np.asarray(indexes, dtype=int), np.asarray(values, dtype=float)


def error_result(pair: str, wav: Path, mp3: Path, error: str) -> dict:
    return {"id": pair, "files": {"wav": str(wav), "mp3": str(mp3)},
            "status": "error", "error": error, "checks": {}}


def analyze(pair: str, wav: Path, mp3: Path) -> dict:
    result = {"id": pair, "files": {"wav": str(wav), "mp3": str(mp3)}, "checks": {}}
    try:
        wi, mi = wav_info(wav), probe(mp3)
        result["wav"] = wi; result["mp3_ffprobe"] = mi
        result["checks"]["wav_pcm_10000_hz"] = wi["sample_rate"] == WAV_RATE
        result["checks"]["mp3_ffprobe_11025_hz"] = mi.get("sample_rate") == MP3_RATE
        a, b = decode_8k(wav), decode_8k(mp3)
        result["decoded_8k"] = {"wav_samples": len(a), "mp3_samples": len(b),
                                "wav_duration_sec": len(a)/COMMON_RATE,
                                "mp3_duration_sec": len(b)/COMMON_RATE}
        duration_delta = abs(len(a) - len(b)) / COMMON_RATE
        result["duration_difference_sec"] = duration_delta
        result["checks"]["mp3_padding_difference_le_0_3_sec"] = duration_delta <= MAX_ENCODER_DELAY_SEC
        corr, lag = best_corr(a, b)
        result["waveform_common_duration"] = {"correlation": corr,
            "lag_samples_at_8000": lag, "lag_seconds": lag / COMMON_RATE if lag is not None else None,
            "search_limit_samples": MAX_LAG_SAMPLES}
        result["checks"]["encoder_delay_offset_0_to_0_3_sec"] = lag is not None and abs(lag) <= MAX_LAG_SAMPLES
        # Probe speech-sized chunks at the beginning, middle, and end. Their
        # lag is a direct speed-change test, not a listening judgment.
        n = min(len(a), len(b)); chunk = max(256, n // 5)
        chunks = []
        for name, frac in (("beginning", .10), ("middle", .50), ("end", .90)):
            start = min(max(0, int(n * frac - chunk / 2)), max(0, n - chunk))
            c, l = best_corr(a[start:start+chunk], b[start:start+chunk])
            chunks.append({"name": name, "start_sample": start, "correlation": c, "lag_samples": l})
        result["speech_chunks"] = chunks
        valid = [q for q in chunks if q["lag_samples"] is not None and q["correlation"] is not None]
        combined_best = max((q["correlation"] for q in valid), default=None)
        lags = [q["lag_samples"] for q in valid]
        drift = max(lags) - min(lags) if lags else None
        result["speed_check"] = {"combined_best_chunk_correlation": combined_best,
                                  "lag_drift_samples_at_8000": drift}
        result["checks"]["chunk_combined_best_correlation_ge_0_95"] = combined_best is not None and combined_best >= CORR_MIN
        result["checks"]["chunk_lag_drift_le_2_common_samples"] = drift is not None and drift <= MAX_DRIFT_SAMPLES
        if lag is not None:
            aa, bb = aligned(a, b, lag)
            ia, fa = f0_frames(aa); ib, fb = f0_frames(bb)
            common, pa, pb = np.intersect1d(ia, ib, return_indices=True)
            cents = 1200 * np.log2(fa[pa] / fb[pb]) if len(common) else np.array([])
            result["f0_autocorrelation"] = {"paired_frames": int(len(common)),
                "minimum_frames_for_check": 10,
                "median_absolute_cents": float(np.median(np.abs(cents))) if len(common) else None}
            result["checks"]["f0_median_abs_cents_le_5_when_10_frames_available"] = (len(common) < 10 or float(np.median(np.abs(cents))) <= F0_CENTS_MAX)
        else:
            result["f0_autocorrelation"] = {"paired_frames": 0, "median_absolute_cents": None}
            result["checks"]["f0_median_abs_cents_le_5_when_10_frames_available"] = False
        result["status"] = "pass" if all(result["checks"].values()) else "fail"
    except Exception as exc:
        result["status"], result["error"] = "error", f"{type(exc).__name__}: {exc}"
    return result


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--evidence-dir", type=Path, default=Path("/workspace/old-tts-1.4.1-delivery/audio-evidence"))
    ap.add_argument("--json", type=Path, default=None)
    args = ap.parse_args()
    wav_dir, mp3_dir = args.evidence_dir / "wav", args.evidence_dir / "mp3"
    pairs = []
    for kind in ("impulsive", "natural"):
        w, m = wav_dir / f"klatt-{kind}-native.wav", mp3_dir / f"klatt-{kind}-native.mp3"
        if not w.exists() or not m.exists():
            pairs.append(error_result(kind, w, m, "required evidence file is missing"))
        else:
            pairs.append(analyze(kind, w, m))
    summary = {"pair_count": 2, "pass": sum(r["status"] == "pass" for r in pairs),
               "fail": sum(r["status"] == "fail" for r in pairs), "error": sum(r["status"] == "error" for r in pairs),
               "limits": {"wav_rate_hz": WAV_RATE, "expected_mp3_rate_hz": MP3_RATE,
                          "common_decode_rate_hz": COMMON_RATE, "encoder_delay_seconds": MAX_ENCODER_DELAY_SEC,
                          "correlation_min": CORR_MIN, "lag_drift_common_samples_max": MAX_DRIFT_SAMPLES,
                          "f0_median_absolute_cents_max_when_10_frames": F0_CENTS_MAX},
               "method_note": "ffprobe/ffmpeg/numpy measurements only; no human-listening claim. MP3 duration padding is allowed within 0.3 s."}
    output = {"summary": summary, "pairs": pairs}
    out = args.json or args.evidence_dir / "verify-klatt-mp3.json"
    out.parent.mkdir(parents=True, exist_ok=True); out.write_text(json.dumps(output, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps(summary, ensure_ascii=False, indent=2)); print(f"JSON: {out}")
    return 0 if summary["pass"] == 2 and summary["fail"] == 0 and summary["error"] == 0 else 2


if __name__ == "__main__":
    raise SystemExit(main())

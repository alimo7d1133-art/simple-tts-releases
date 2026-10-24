ClArTTS Arabic Flite voice (clartts_ar.flitevox) reproduction notes.
Data: MBZUAI/ClArTTS (CC BY 4.0), 2100 clips (about 2.47 h) selected from train shards 00-05, 2-10 s each.
extract_unicode.py writes etc/txt.done.data using diacritized Arabic Unicode text (Flite grapheme voices take Arabic Unicode at run time).
train.sh runs Festvox Clustergen (setup_cg, build_prompts, label, f0/mcep/str, cluster, dur); afterwards: ./bin/build_flite; cd flite; make; make voicedump.
mcd.out is the Clustergen resynthesis MCD report (mean 5.55).
Tools: Festvox, Flite, speech_tools, SPTK (BSD-style licences).

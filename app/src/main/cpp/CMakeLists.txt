cmake_minimum_required(VERSION 3.22.1)
project(fliteretro C CXX)
set(F ${CMAKE_CURRENT_SOURCE_DIR}/../../../../flite)
set(CMAKE_C_STANDARD 99)
set(SOURCES
 ${F}/src/audio/auclient.c
 ${F}/src/audio/auserver.c
 ${F}/src/audio/audio.c
 ${F}/src/audio/au_streaming.c
 ${F}/src/audio/au_none.c
 ${F}/src/cg/cst_cg.c
 ${F}/src/cg/cst_mlsa.c
 ${F}/src/cg/cst_mlpg.c
 ${F}/src/cg/cst_vc.c
 ${F}/src/cg/cst_cg_load_voice.c
 ${F}/src/cg/cst_cg_dump_voice.c
 ${F}/src/cg/cst_cg_map.c
 ${F}/src/cg/cst_spamf0.c
 ${F}/src/hrg/cst_utterance.c
 ${F}/src/hrg/cst_relation.c
 ${F}/src/hrg/cst_item.c
 ${F}/src/hrg/cst_ffeature.c
 ${F}/src/hrg/cst_rel_io.c
 ${F}/src/lexicon/cst_lexicon.c
 ${F}/src/lexicon/cst_lts.c
 ${F}/src/lexicon/cst_lts_rewrites.c
 ${F}/src/regex/cst_regex.c
 ${F}/src/regex/regexp.c
 ${F}/src/regex/regsub.c
 ${F}/src/speech/cst_wave.c
 ${F}/src/speech/cst_wave_io.c
 ${F}/src/speech/cst_track.c
 ${F}/src/speech/cst_track_io.c
 ${F}/src/speech/cst_wave_utils.c
 ${F}/src/speech/cst_lpcres.c
 ${F}/src/speech/rateconv.c
 ${F}/src/speech/g721.c
 ${F}/src/speech/g72x.c
 ${F}/src/speech/g723_24.c
 ${F}/src/speech/g723_40.c
 ${F}/src/stats/cst_cart.c
 ${F}/src/stats/cst_viterbi.c
 ${F}/src/stats/cst_ss.c
 ${F}/src/synth/cst_synth.c
 ${F}/src/synth/cst_utt_utils.c
 ${F}/src/synth/cst_voice.c
 ${F}/src/synth/cst_phoneset.c
 ${F}/src/synth/cst_ffeatures.c
 ${F}/src/synth/cst_ssml.c
 ${F}/src/synth/flite.c
 ${F}/src/utils/cst_alloc.c
 ${F}/src/utils/cst_error.c
 ${F}/src/utils/cst_string.c
 ${F}/src/utils/cst_wchar.c
 ${F}/src/utils/cst_tokenstream.c
 ${F}/src/utils/cst_val.c
 ${F}/src/utils/cst_features.c
 ${F}/src/utils/cst_endian.c
 ${F}/src/utils/cst_socket.c
 ${F}/src/utils/cst_val_const.c
 ${F}/src/utils/cst_val_user.c
 ${F}/src/utils/cst_args.c
 ${F}/src/utils/cst_url.c
 ${F}/src/utils/cst_mmap_posix.c
 ${F}/src/utils/cst_file_stdio.c
 ${F}/src/wavesynth/cst_units.c
 ${F}/src/wavesynth/cst_clunits.c
 ${F}/src/wavesynth/cst_diphone.c
 ${F}/src/wavesynth/cst_sigpr.c
 ${F}/src/wavesynth/cst_sts.c
 ${F}/src/wavesynth/cst_reflpc.c
 ${F}/lang/cmulex/cmu_lts_rules.c
 ${F}/lang/cmulex/cmu_lts_model.c
 ${F}/lang/cmulex/cmu_lex.c
 ${F}/lang/cmulex/cmu_lex_entries.c
 ${F}/lang/cmulex/cmu_lex_data.c
 ${F}/lang/cmulex/cmu_postlex.c
 ${F}/lang/cmu_grapheme_lex/cmu_grapheme_lex.c
 ${F}/lang/cmu_grapheme_lex/grapheme_unitran_tables.c
 ${F}/lang/cmu_grapheme_lang/cmu_grapheme_lang.c
 ${F}/lang/cmu_grapheme_lang/cmu_grapheme_phoneset.c
 ${F}/lang/cmu_grapheme_lang/cmu_grapheme_phrasing_cart.c
 ${F}/lang/cmu_grapheme_lang/graph_gpos.c
 ${F}/lang/usenglish/us_int_accent_cart.c
 ${F}/lang/usenglish/us_int_tone_cart.c
 ${F}/lang/usenglish/us_f0_model.c
 ${F}/lang/usenglish/us_dur_stats.c
 ${F}/lang/usenglish/us_durz_cart.c
 ${F}/lang/usenglish/us_f0lr.c
 ${F}/lang/usenglish/us_phoneset.c
 ${F}/lang/usenglish/us_ffeatures.c
 ${F}/lang/usenglish/us_phrasing_cart.c
 ${F}/lang/usenglish/us_gpos.c
 ${F}/lang/usenglish/us_text.c
 ${F}/lang/usenglish/us_expand.c
 ${F}/lang/usenglish/us_nums_cart.c
 ${F}/lang/usenglish/us_aswd.c
 ${F}/lang/usenglish/usenglish.c
 ${F}/lang/usenglish/us_pos_cart.c
 ${F}/lang/cmu_us_kal/cmu_us_kal_diphone.c
 ${F}/lang/cmu_us_kal/cmu_us_kal.c
 ${F}/lang/cmu_us_kal/cmu_us_kal_lpc.c
 ${F}/lang/cmu_us_kal/cmu_us_kal_res.c
 ${F}/lang/cmu_us_kal/cmu_us_kal_residx.c
 ${F}/lang/cmu_us_kal/cmu_us_kal_ressize.c
 ${F}/lang/cmu_us_kal16/cmu_us_kal16_diphone.c
 ${F}/lang/cmu_us_kal16/cmu_us_kal16.c
 ${F}/lang/cmu_us_kal16/cmu_us_kal16_lpc.c
 ${F}/lang/cmu_us_kal16/cmu_us_kal16_res.c
 ${F}/lang/cmu_us_kal16/cmu_us_kal16_residx.c
 ${F}/lang/cmu_us_awb/cmu_us_awb.c
 ${F}/lang/cmu_us_awb/cmu_us_awb_cg_single_mcep_trees.c
 ${F}/lang/cmu_us_awb/cmu_us_awb_cg.c
 ${F}/lang/cmu_us_awb/cmu_us_awb_cg_single_params.c
 ${F}/lang/cmu_us_awb/cmu_us_awb_cg_durmodel.c
 ${F}/lang/cmu_us_awb/cmu_us_awb_cg_phonestate.c
 ${F}/lang/cmu_us_awb/cmu_us_awb_cg_f0_trees.c
 ${F}/lang/cmu_us_awb/cmu_us_awb_spamf0_accent_params.c
 ${F}/lang/cmu_us_awb/cmu_us_awb_spamf0_phrase.c
 ${F}/lang/cmu_us_awb/cmu_us_awb_spamf0_accent.c
 ${F}/lang/cmu_us_rms/cmu_us_rms.c
 ${F}/lang/cmu_us_rms/cmu_us_rms_cg_single_mcep_trees.c
 ${F}/lang/cmu_us_rms/cmu_us_rms_cg.c
 ${F}/lang/cmu_us_rms/cmu_us_rms_cg_single_params.c
 ${F}/lang/cmu_us_rms/cmu_us_rms_cg_durmodel.c
 ${F}/lang/cmu_us_rms/cmu_us_rms_cg_phonestate.c
 ${F}/lang/cmu_us_rms/cmu_us_rms_cg_f0_trees.c
 ${F}/lang/cmu_us_rms/cmu_us_rms_spamf0_accent_params.c
 ${F}/lang/cmu_us_rms/cmu_us_rms_spamf0_phrase.c
 ${F}/lang/cmu_us_rms/cmu_us_rms_spamf0_accent.c
 ${F}/lang/cmu_us_slt/cmu_us_slt.c
 ${F}/lang/cmu_us_slt/cmu_us_slt_cg_single_mcep_trees.c
 ${F}/lang/cmu_us_slt/cmu_us_slt_cg.c
 ${F}/lang/cmu_us_slt/cmu_us_slt_cg_single_params.c
 ${F}/lang/cmu_us_slt/cmu_us_slt_cg_durmodel.c
 ${F}/lang/cmu_us_slt/cmu_us_slt_cg_phonestate.c
 ${F}/lang/cmu_us_slt/cmu_us_slt_cg_f0_trees.c
 ${F}/lang/cmu_us_slt/cmu_us_slt_spamf0_accent.c
 ${F}/lang/cmu_us_slt/cmu_us_slt_spamf0_phrase.c
 ${F}/lang/cmu_us_slt/cmu_us_slt_spamf0_accent_params.c
)
add_library(fliteretro SHARED ${SOURCES} bridge.c)
target_include_directories(fliteretro PRIVATE ${F}/include ${F}/lang/usenglish ${F}/lang/cmulex ${F}/lang/cmu_grapheme_lang ${F}/lang/cmu_grapheme_lex)
target_compile_definitions(fliteretro PRIVATE CST_AUDIO_NONE)
target_compile_options(fliteretro PRIVATE -Wno-error=implicit-function-declaration -Wno-error=incompatible-pointer-types -fno-strict-aliasing)
target_link_libraries(fliteretro m)
if(ANDROID)
 target_link_options(fliteretro PRIVATE -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384)
endif()

if(ANDROID)
# Independent SVOX Pico engine
add_subdirectory(pico)

# Independent rsynth formant engine
add_subdirectory(rsynth)

# MBROLA diphone engine, with Flite phoneme frontend
add_subdirectory(mbrola)

# GnuSpeechSA articulatory synthesis engine
add_subdirectory(${CMAKE_CURRENT_LIST_DIR}/../../../../gnuspeech/android gnuspeech-android)

add_subdirectory(klatt)

endif()

add_subdirectory(mp3)

if(ANDROID)
 add_subdirectory(festival)
 add_subdirectory(openjtalk)
 add_subdirectory(flitehts)
 include(EkhoCMake.cmake)
endif()

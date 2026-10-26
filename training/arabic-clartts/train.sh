#!/bin/bash
set -x
cd /workspace/arabic-train-clartts
export FESTVOXDIR=$PWD/tools/festvox ESTDIR=$PWD/tools/speech_tools FLITEDIR=$PWD/tools/flite SPTKDIR=$PWD/tools/SPTK
export PATH=$ESTDIR/bin:$PWD/tools/festival/bin:$PATH
mkdir ofl_ar_clartts_ar && cd ofl_ar_clartts_ar
$FESTVOXDIR/src/clustergen/setup_cg ofl ar clartts_ar </dev/null
./bin/get_wavs ../wav/*.wav
cp ../txt.done.unicode etc/txt.done.data
$FESTVOXDIR/src/grapheme/make_cg_grapheme
./bin/do_build parallel build_prompts etc/txt.done.data
./bin/do_build label etc/txt.done.data
./bin/do_clustergen parallel build_utts etc/txt.done.data
./bin/do_clustergen generate_statenames
./bin/do_clustergen generate_filters
./bin/do_clustergen parallel f0_v_sptk
./bin/do_clustergen parallel mcep_sptk
sed -i 's/cg:mixed_excitation nil/cg:mixed_excitation t/' festvox/clustergen.scm
./bin/do_clustergen parallel str_sptk
./bin/do_clustergen parallel combine_coeffs_me
./bin/traintest etc/txt.done.data
./bin/do_clustergen parallel cluster etc/txt.done.data.train
./bin/do_clustergen dur etc/txt.done.data.train
$FESTVOXDIR/src/clustergen/cg_test resynth cgp > mcd.out
./bin/do_clustergen festvox_dist
echo TRAIN_STAGE_DONE

#!/bin/bash
# usage: train_voice.sh <lang ar|us> <inst> <name> <datadir> ; datadir has wav/ and txt.done.data (ar: txt.done.unicode)
LANGC=$1; INST=$2; NAME=$3; DATA=$4
T=/workspace/arabic-train-clartts/tools
export FESTVOXDIR=$T/festvox ESTDIR=$T/speech_tools FLITEDIR=$T/flite SPTKDIR=$T/SPTK
export PATH=$ESTDIR/bin:$T/festival/bin:$PATH
set -x
cd $DATA
rm -rf ${INST}_${LANGC}_${NAME}; mkdir ${INST}_${LANGC}_${NAME} && cd ${INST}_${LANGC}_${NAME}
$FESTVOXDIR/src/clustergen/setup_cg $INST $LANGC $NAME </dev/null
./bin/get_wavs ../wav/*.wav
if [ $LANGC = ar ]; then cp ../txt.done.unicode etc/txt.done.data; $FESTVOXDIR/src/grapheme/make_cg_grapheme; else cp ../txt.done.data etc/txt.done.data; fi
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
/workspace/voice-queue/convert_voice.sh $PWD $LANGC $INST $NAME

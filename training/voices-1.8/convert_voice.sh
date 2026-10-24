#!/bin/bash
# usage: convert_voice.sh <voicedir> <lang> <inst> <name>   (run after TRAIN_STAGE_DONE)
D=$1; LANGC=$2; INST=$3; NAME=$4
T=/workspace/arabic-train-clartts/tools
export FESTVOXDIR=$T/festvox ESTDIR=$T/speech_tools FLITEDIR=$T/flite SPTKDIR=$T/SPTK
export PATH=$ESTDIR/bin:$T/festival/bin:$PATH
cd $D
$FLITEDIR/tools/setup_flite > /tmp/sf_$NAME.log 2>&1
./bin/build_flite > /tmp/bf_$NAME.log 2>&1
cd flite && (make > /tmp/mk_$NAME.log 2>&1; make voicedump >> /tmp/mk_$NAME.log 2>&1)
ls -la *.flitevox
OUT=/workspace/voices-out/${INST}_${LANGC}_${NAME}; mkdir -p $OUT
cp *.flitevox $OUT/; cp ../mcd.out $OUT/; cp ../etc/txt.done.data $OUT/txt.done.data
[ -f flite_${INST}_${LANGC}_${NAME} ] && cp flite_${INST}_${LANGC}_${NAME} $OUT/
echo VOICE_DONE

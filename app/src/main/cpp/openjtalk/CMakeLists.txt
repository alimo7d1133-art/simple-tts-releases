cmake_minimum_required(VERSION 3.22.1)
project(openjtalk LANGUAGES C CXX)

set(U "${CMAKE_CURRENT_LIST_DIR}/upstream")
set(OJ "${U}/open_jtalk-1.11")
set(HTS "${U}/hts_engine_API-1.10")

# Open JTalk's C front end and all HTS engine implementation units.
file(GLOB_RECURSE OJ_SOURCES CONFIGURE_DEPENDS "${OJ}/*.c")
list(FILTER OJ_SOURCES EXCLUDE REGEX "/bin/open_jtalk\\.c$")
file(GLOB HTS_SOURCES CONFIGURE_DEPENDS "${HTS}/lib/*.c")

# This is the MeCab library list from mecab/src/Makefile.am.  Keep the
# dictionary tools and CLI translation units out: they introduce duplicate
# entry points and are not needed by Open JTalk.
set(MECAB_SOURCES
    ${OJ}/mecab/src/char_property.cpp
    ${OJ}/mecab/src/connector.cpp
    ${OJ}/mecab/src/context_id.cpp
    ${OJ}/mecab/src/dictionary.cpp
    ${OJ}/mecab/src/dictionary_rewriter.cpp
    ${OJ}/mecab/src/eval.cpp
    ${OJ}/mecab/src/feature_index.cpp
    ${OJ}/mecab/src/iconv_utils.cpp
    ${OJ}/mecab/src/lbfgs.cpp
    ${OJ}/mecab/src/learner.cpp
    ${OJ}/mecab/src/learner_tagger.cpp
    ${OJ}/mecab/src/libmecab.cpp
    ${OJ}/mecab/src/mecab.cpp
    ${OJ}/mecab/src/nbest_generator.cpp
    ${OJ}/mecab/src/param.cpp
    ${OJ}/mecab/src/string_buffer.cpp
    ${OJ}/mecab/src/tagger.cpp
    ${OJ}/mecab/src/tokenizer.cpp
    ${OJ}/mecab/src/utils.cpp
    ${OJ}/mecab/src/viterbi.cpp
    ${OJ}/mecab/src/writer.cpp)

add_library(openjtalk SHARED ${OJ_SOURCES} ${HTS_SOURCES} ${MECAB_SOURCES} openjtalk_jni.cpp)
target_include_directories(openjtalk PRIVATE
    "${CMAKE_CURRENT_LIST_DIR}" "${OJ}/mecab/src" "${OJ}/text2mecab"
    "${OJ}/mecab2njd" "${OJ}/njd" "${OJ}/njd_set_pronunciation"
    "${OJ}/njd_set_digit" "${OJ}/njd_set_accent_phrase" "${OJ}/njd_set_accent_type"
    "${OJ}/njd_set_unvoiced_vowel" "${OJ}/njd_set_long_vowel" "${OJ}/njd2jpcommon"
    "${OJ}/jpcommon" "${HTS}/include" "${HTS}/lib")
target_compile_definitions(openjtalk PRIVATE HAVE_CONFIG_H CHARSET_UTF_8)
# The bundled MeCab 0.996 sources use register and std::binary_function;
# C++14 is the newest mode they support without source mutation.
target_compile_features(openjtalk PRIVATE cxx_std_14)
target_compile_options(openjtalk PRIVATE -fno-strict-aliasing -Wno-unused-parameter -Wno-register
    $<$<COMPILE_LANGUAGE:CXX>:-std=c++14>)
target_link_libraries(openjtalk PRIVATE m log android)
if(ANDROID)
    target_link_options(openjtalk PRIVATE -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384 -Wl,--no-undefined)
endif()

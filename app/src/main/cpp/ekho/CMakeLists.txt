cmake_minimum_required(VERSION 3.22.1)
project(ekho LANGUAGES C CXX)
# Independent Ekho shared library.  Keep this target separate from the app
# aggregate: it deliberately excludes the CLI, GTK, server, English and
# optional neural/third-party engines.
set(EKHO_ROOT ${CMAKE_CURRENT_LIST_DIR})
set(EKHO_CORE
  ${EKHO_ROOT}/audio.cpp
  ${EKHO_ROOT}/number.cpp
  ${EKHO_ROOT}/disabled_features.cpp
  ${EKHO_ROOT}/ekho_dict.cpp
  ${EKHO_ROOT}/libekho.cpp
  ${EKHO_ROOT}/libekho_impl.cpp
  ${EKHO_ROOT}/musicxml.cpp
  ${EKHO_ROOT}/phonetic_symbol.cpp
  ${EKHO_ROOT}/ssml.cpp
  ${EKHO_ROOT}/symbol_array.cpp
  ${EKHO_ROOT}/synthesize.cpp
  ${EKHO_ROOT}/word.cpp
  ${EKHO_ROOT}/zh_symbol_map.cpp
  ${EKHO_ROOT}/zhy_symbol_map.cpp
  ${EKHO_ROOT}/sr-convert/dsp.cpp
  ${EKHO_ROOT}/sonic/sonic.c
  ${EKHO_ROOT}/ekho_jni.cpp)
file(GLOB EKHO_SNDFILE ${EKHO_ROOT}/libsndfile-src/*.c)
file(GLOB EKHO_SNDFILE_GSM ${EKHO_ROOT}/libsndfile-src/GSM610/*.c)
file(GLOB EKHO_SNDFILE_G72X ${EKHO_ROOT}/libsndfile-src/G72x/*.c)
list(FILTER EKHO_SNDFILE EXCLUDE REGEX "/(test_[^/]*|[^/]*_test)\\.c$")
list(FILTER EKHO_SNDFILE_GSM EXCLUDE REGEX "/(test_[^/]*|[^/]*_test)\\.c$")
list(FILTER EKHO_SNDFILE_G72X EXCLUDE REGEX "/(test_[^/]*|[^/]*_test)\\.c$")
add_library(ekho SHARED ${EKHO_CORE} ${EKHO_SNDFILE} ${EKHO_SNDFILE_GSM} ${EKHO_SNDFILE_G72X})
target_include_directories(ekho PRIVATE ${EKHO_ROOT} ${EKHO_ROOT}/libsndfile-src ${EKHO_ROOT}/sonic ${EKHO_ROOT}/sr-convert ${EKHO_ROOT}/utfcpp-source)
target_compile_definitions(ekho PRIVATE ANDROID DEBUG_ANDROID OUTPUT16BIT NO_SSE EKHO_NO_CURL HAVE_SONIC)
target_compile_options(ekho PRIVATE -fexceptions -Wno-bitwise-op-parentheses -Wno-shift-negative-value -Wno-unused-parameter)
target_compile_features(ekho PRIVATE cxx_std_17)
target_link_options(ekho PRIVATE -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384)
target_link_libraries(ekho PRIVATE log android m)
function(ekho_attach_to_target target)
  add_dependencies(${target} ekho)
  target_link_libraries(${target} PRIVATE ekho)
endfunction()

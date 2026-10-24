# Android integration subproject. Parent app CMake must call:
# add_subdirectory(${CMAKE_CURRENT_LIST_DIR}/../../gnuspeech/android gnuspeech-android)
# and link target gnuspeech_jni into the app's JNI shared library.
cmake_minimum_required(VERSION 3.22.1)
project(gnuspeech_android LANGUAGES CXX)
set(GNUSPEECH_SOURCE_DIR "${CMAKE_CURRENT_LIST_DIR}/../source" CACHE PATH "GnuSpeechSA source")
add_subdirectory("${GNUSPEECH_SOURCE_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/gnuspeechsa" EXCLUDE_FROM_ALL)
add_library(gnuspeech_jni SHARED "${CMAKE_CURRENT_LIST_DIR}/../../app/src/main/cpp/GnuspeechBridge.cpp")
target_include_directories(gnuspeech_jni PRIVATE "${GNUSPEECH_SOURCE_DIR}/src" "${GNUSPEECH_SOURCE_DIR}/src/trm_control_model" "${GNUSPEECH_SOURCE_DIR}/src/trm" "${GNUSPEECH_SOURCE_DIR}/src/en/phonetic_string_parser" "${GNUSPEECH_SOURCE_DIR}/src/en/text_parser")
target_link_libraries(gnuspeech_jni PRIVATE gnuspeechsa)
target_compile_features(gnuspeech_jni PRIVATE cxx_std_11)
if(ANDROID)
  # Upstream text parser stores negative sentinel values such as -11 in char.
  # ARM defaults to unsigned char; preserve the original desktop semantics.
  target_compile_options(gnuspeechsa PRIVATE -fsigned-char)
  target_compile_options(gnuspeech_jni PRIVATE -fsigned-char)
  target_link_options(gnuspeechsa PRIVATE -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384)
  target_link_options(gnuspeech_jni PRIVATE -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384)
endif()

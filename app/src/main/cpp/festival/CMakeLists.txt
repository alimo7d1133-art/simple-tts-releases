# Old TTS Android integration of actual Festival and Edinburgh Speech Tools.
set(FEST_ROOT "${CMAKE_CURRENT_LIST_DIR}/../../../../../festival-runtime")
set(FEST_PREBUILT "${FEST_ROOT}/prebuilt/${ANDROID_ABI}")
add_library(festivalretro SHARED FestivalPlugin.cpp)
target_include_directories(festivalretro PRIVATE "${FEST_ROOT}/festival/src/include" "${FEST_ROOT}/speech_tools/include")
target_compile_features(festivalretro PRIVATE cxx_std_11)
target_compile_options(festivalretro PRIVATE -fexceptions -frtti)
target_link_libraries(festivalretro PRIVATE
 "-Wl,--start-group"
 "${FEST_PREBUILT}/libFestival.a"
 "${FEST_PREBUILT}/libestools.a"
 "${FEST_PREBUILT}/libestbase.a"
 "${FEST_PREBUILT}/libeststring.a"
 "-Wl,--end-group" m dl log)
target_link_options(festivalretro PRIVATE -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384)

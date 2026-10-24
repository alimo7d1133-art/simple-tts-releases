cmake_minimum_required(VERSION 3.22.1)
project(rsynthretro C)
set(RSYNTH_DIR ${CMAKE_CURRENT_LIST_DIR})
set(CMAKE_C_STANDARD 99)
add_library(rsynthretro SHARED
  darray.c elements.c holmes.c opsynth.c phtoelm.c phones.c text.c say.c english.c deutsch.c charset.c trie.c
  ../rsynth_jni.c
)
target_include_directories(rsynthretro PUBLIC ${RSYNTH_DIR})
if(ANDROID)
  target_include_directories(rsynthretro PRIVATE ${CMAKE_ANDROID_NDK}/sysroot/usr/include)
  target_link_options(rsynthretro PRIVATE -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384)
endif()
target_link_libraries(rsynthretro m)
target_compile_definitions(rsynthretro PRIVATE HAVE_CONFIG_H)
target_compile_options(rsynthretro PRIVATE -Wno-error=implicit-function-declaration -Wno-error=incompatible-pointer-types -fno-strict-aliasing)

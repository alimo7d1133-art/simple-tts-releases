cmake_minimum_required(VERSION 3.22.1)
project(flitehts C CXX)
set(HTS "${CMAKE_CURRENT_LIST_DIR}/../openjtalk/upstream/hts_engine_API-1.10")
set(FLITE_ROOT "${CMAKE_CURRENT_LIST_DIR}/../../../../../flite")
file(GLOB HTS_SOURCES CONFIGURE_DEPENDS "${HTS}/lib/*.c")
add_library(flitehts SHARED ${HTS_SOURCES}
    upstream/flite_hts_engine.c flitehts_jni.cpp)
target_include_directories(flitehts PRIVATE
    "${CMAKE_CURRENT_LIST_DIR}/upstream" "${HTS}/include" "${FLITE_ROOT}/include")
target_compile_options(flitehts PRIVATE -fno-strict-aliasing -Wno-unused-parameter
    -Wno-error=implicit-function-declaration -Wno-error=incompatible-pointer-types)
target_link_libraries(flitehts PRIVATE fliteretro m log)
if(ANDROID)
    target_link_options(flitehts PRIVATE -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384)
endif()

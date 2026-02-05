cmake_minimum_required(VERSION 3.22.1)

project("signaturelens")

add_library(
        signaturelens-native
        SHARED
        yuv_converter.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        signaturelens-native
        ${log-lib}
)

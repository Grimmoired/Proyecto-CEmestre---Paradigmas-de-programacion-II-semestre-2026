 cmake_minimum_required(VERSION 4.2)
project(Cemestre C)

set(CMAKE_C_STANDARD 11)

add_executable(Cemestre main.c
        constants.h
        types.h
        catalogLoader.c
        catalogLoader.h)

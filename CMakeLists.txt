 cmake_minimum_required(VERSION 3.5)
project(Cemestre C)

 add_compile_definitions(PROJECT_ROOT_PATH="${CMAKE_SOURCE_DIR}/")
set(CMAKE_C_STANDARD 11)
add_executable(Cemestre main.c
        constants.h
        types.h
        catalogLoader.c
        catalogLoader.h
        studentLoader.h
        studentLoader.c
        scheduleClash.c
        scheduleClash.h
        Requisites.h
        Requisites.c
)
//
// Created by Bryan on 8/28/2026.
//

#ifndef CONSTANTS_H
#define CONSTANTS_H

#define maxCourses 70
#define maxGroupsPerCourse 40
#define maxScheduleBlocks 3
#define maxRequisites 3
#define maxCorequisites 3
#define maxCourseCodeLen 8
#define maxGroupIdLen 10
#define maxCourseNameLen 80
#define maxStudentNameLen 50
#define maxStudentIdLen 12
#define maxTimeLen 6
#define maxHistory 70
#define maxCycles maxCourses*maxRequisites
#define nombreCarrera1 "Ingenieria en Computadores"
#define nombreCarrera2 "Ingenieria Fisica"
#define CE_CATALOG_PATH   PROJECT_ROOT_PATH "PlanEstudioCE.txt"
#define CE_HISTORY_PATH   PROJECT_ROOT_PATH "HistorialEstudianteCE.txt"
#define IF_CATALOG_PATH   PROJECT_ROOT_PATH "PlanEstudioFI.txt"
#define IF_HISTORY_PATH   PROJECT_ROOT_PATH "HistorialEstudianteFI.txt"
#define CE_OUTPUT_PATH   PROJECT_ROOT_PATH "OutputCE.json"
#define IF_OUTPUT_PATH   PROJECT_ROOT_PATH "OutputIF.json"

#endif
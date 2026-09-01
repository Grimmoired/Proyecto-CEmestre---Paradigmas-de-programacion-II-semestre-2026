//
// Created by Bryan on 8/28/2026.
//

#ifndef TYPES_H
#define TYPES_H

#include "constants.h"

typedef struct {
    char day[4];
    char startTime[maxTimeLen];
    char endTime[maxTimeLen];
} ScheduleBlock;

typedef struct {
    char groupId[maxGroupIdLen];
    ScheduleBlock blocks[maxScheduleBlocks];
    int blockCount;
    int hasClash;
} CourseGroup;

typedef struct {
    char courseCode[maxCourseCodeLen];
    char courseName[maxCourseNameLen];
    int credits;
    CourseGroup groups[maxGroupsPerCourse];
    int groupCount;
    char requisites[maxRequisites][maxCourseCodeLen];
    int requisiteCount;
    char corequisites[maxCorequisites][maxCourseCodeLen];
    int corequisiteCount;
    int hasScheduleClash;
    int canEnroll;
} Course;

typedef struct {
    Course courses[maxCourses];
    int courseCount;
} Catalog;

typedef struct {
    char studentId[maxStudentIdLen];
    char studentName[maxStudentNameLen];
    char approvedCourses[maxHistory][maxCourseCodeLen];
    int approvedCount;
} StudentHistory;

#endif
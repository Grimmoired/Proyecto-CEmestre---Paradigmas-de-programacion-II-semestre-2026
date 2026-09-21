#ifndef REQUISITES_H
#define REQUISITES_H

#include "types.h"
#include "constants.h"


int fulfillRequisites(char requisites[][maxCourseCodeLen], int requisiteCount, const StudentHistory *history);

int fulfillCorequisites(
    char corequisites[][maxCourseCodeLen],
    int corequisiteCount,
    const StudentHistory *history,
    const Catalog *catalog,
    const int ancestors[],
    int ancestorCount
);

void computeEligibility(Catalog *catalog, const StudentHistory *history);

typedef struct {
    int courseIdx[maxCourses];
    int courseCount;
} CycleReport;

int detectCycles(const Catalog *catalog, CycleReport cyclesOut[], int maxCycles);

typedef enum { COLOR_WHITE, COLOR_GRAY, COLOR_BLACK } NodeColor;

#endif
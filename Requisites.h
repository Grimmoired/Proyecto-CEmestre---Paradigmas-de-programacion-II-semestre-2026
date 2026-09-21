#ifndef REQUISITES_H
#define REQUISITES_H

#include "types.h"
#include "constants.h"


int fulfillRequisites(const char requisites[][maxCourseCodeLen], int requisiteCount, const StudentHistory *history);

int fulfillCorequisites(
    const char corequisites[][maxCourseCodeLen],
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

int detectCycles(Catalog *catalog, CycleReport cyclesOut[]);



#endif
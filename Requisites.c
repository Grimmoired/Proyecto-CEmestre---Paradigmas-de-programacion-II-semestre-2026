#include <string.h>
#include "Requisites.h"

static int findCourseIdx(const Catalog *catalog, const char *code) {
    for (int i = 0; i < catalog->courseCount; i++) {
        if (strcmp(catalog->courses[i].courseCode, code) == 0) {
            return i;
        }
    }
    return -1;
}
 
static int isAncestor(const int ancestors[], int ancestorCount, int index) {
    for (int i = 0; i < ancestorCount; i++) {
        if (ancestors[i] == index) {
            return 1;
        }
    }
    return 0;
}

static int isCodeApproved(const char *code, const StudentHistory *history) {
    for (int i = 0; i < history->approvedCount; i++) {
        if (strcmp(code, history->approvedCourses[i]) == 0) {
            return 1;
        }
    }
    return 0;
}

int fulfillRequisites(const char requisites[][maxCourseCodeLen], int requisiteCount, const StudentHistory *history) {
    for (int i = 0; i < requisiteCount; i++) {
        if (!isCodeApproved(requisites[i], history)) {
            return 0;
        }
    }
    return 1;
}


int fulfillCorequisites(
    const char corequisites[][maxCourseCodeLen],
    int corequisiteCount,
    const StudentHistory *history,
    const Catalog *catalog,
    const int ancestors[],
    int ancestorCount
) {
    for (int i = 0; i < corequisiteCount; i++) {
        int idx = findCourseIdx(catalog, corequisites[i]);
        if (idx == -1) {
            return 0;
        }
        if (isAncestor(ancestors, ancestorCount, idx)) {
            continue;
        }
        if (isCodeApproved(corequisites[i], history)) {
            continue;
        }

        const Course *coreq = &catalog->courses[idx];
        int extendedAncestors[maxCourses];
        for (int k = 0; k < ancestorCount; k++) {
            extendedAncestors[k] = ancestors[k];
        }
        extendedAncestors[ancestorCount] = idx;
        int checkCoreqReqs = fulfillRequisites(coreq->requisites, coreq->requisiteCount, history);
        int checkCoreqCor = fulfillCorequisites(coreq->corequisites, coreq->corequisiteCount,history, catalog, extendedAncestors, ancestorCount + 1);
        if (!(checkCoreqReqs && checkCoreqCor)) {
            return 0;
        }
    }
    return 1;
}

void computeEligibility(Catalog *catalog, const StudentHistory *history) {
    for (int i = 0; i < catalog->courseCount; i++) {
        Course *course = &catalog->courses[i];
        int ancestors[maxCourses];
        ancestors[0] = i;
        int checkReqs = fulfillRequisites(course->requisites, course->requisiteCount, history);
        int checkCoreqs = fulfillCorequisites(course->corequisites, course->corequisiteCount, history, catalog, ancestors, 1);
        course->canEnroll = (checkReqs && checkCoreqs && !isCodeApproved(course->courseCode, history)) ? 1 : 0;
    }
}

typedef enum { COLOR_WHITE, COLOR_GRAY, COLOR_BLACK } NodeColor;
static void dfsCycles(
    const Catalog *catalog,
    int node,
    NodeColor colors[],
    int path[],
    int *pathCount,
    CycleReport cycleOut[],
    int *cycleCount
) {
    colors[node] = COLOR_GRAY;
    path[*pathCount] = node;
    (*pathCount)++;

    const Course *course = &catalog->courses[node];

    for (int i = 0; i < course->requisiteCount; i++) {
        int neighbor = findCourseIdx(catalog, course->requisites[i]);
        if (neighbor == -1) {
            continue;
        }
        if (colors[neighbor] == COLOR_WHITE) {
            dfsCycles(catalog, neighbor, colors, path, pathCount, cycleOut, cycleCount);

        } else if (colors[neighbor] == COLOR_GRAY) {
            int start = -1;
            for (int k = *pathCount - 1; k >= 0; k--) {
                if (path[k] == neighbor) {
                    start = k;
                    break;
                }
            }

            if (start != -1 && *cycleCount < maxCycles) {
                CycleReport *report = &cycleOut[*cycleCount];
                report->courseCount = 0;
                for (int k = start; k < *pathCount; k++) {
                    report->courseIdx[report->courseCount] = path[k];
                    report->courseCount++;
                }
                (*cycleCount)++;
            }
        }
    }

    (*pathCount)--;
    colors[node] = COLOR_BLACK;
}

void markCycle(Course courses[], const CycleReport cycleOut[], int cycleCount) {
    for (int i = 0; i < cycleCount; i++) {
        const CycleReport *cycle = &cycleOut[i];
        for (int j = 0; j < cycle->courseCount; j++) {
            courses[cycle->courseIdx[j]].hasCycle = 1;
        }
    }
}

int detectCycles(Catalog *catalog, CycleReport cycleOut[]) {
    NodeColor colors[maxCourses];
    for (int i = 0; i < catalog->courseCount; i++) {
        colors[i] = COLOR_WHITE;
    }

    int path[maxCourses];
    int pathCount = 0;
    int cycleCount = 0;

    for (int i = 0; i < catalog->courseCount; i++) {
        if (colors[i] == COLOR_WHITE) {
            dfsCycles(catalog, i, colors, path, &pathCount, cycleOut, &cycleCount);
        }
    }
    markCycle(catalog->courses, cycleOut, cycleCount);
    return cycleCount;
}
#include <stdbool.h>
#include <string.h>
#include <stdlib.h>
#include "scheduleClash.h"
#include "types.h"
#include "constants.h"

int timeToMinutes(const char *hhmm) {
    // Solo acepta el formato "HHMM", ej. "0730" -> 7*60+30 = 450
    if (hhmm == NULL || strlen(hhmm) != 4) {
        return -1;
    }
    for (int i = 0; i < 4; i++) {
        if (hhmm[i] < '0' || hhmm[i] > '9') {
            return -1;
        }
    }
    int hours = (hhmm[0] - '0') * 10 + (hhmm[1] - '0');
    int minutes = (hhmm[2] - '0') * 10 + (hhmm[3] - '0');
    return hours * 60 + minutes;
}

bool blocksClash(const ScheduleBlock *a, const ScheduleBlock *b) {
    if (strcmp(a->day, b->day) != 0) {
        return false;
    }

    int startA = timeToMinutes(a->startTime);
    int endA = timeToMinutes(a->endTime);
    int startB = timeToMinutes(b->startTime);
    int endB = timeToMinutes(b->endTime);

    if (startA < 0 || endA < 0 || startB < 0 || endB < 0) {
        return false;
    }

    return (startA < endB) && (startB < endA);
}

bool groupsClash(const CourseGroup *a, const CourseGroup *b) {
    for (int i = 0; i < a->blockCount; i++) {
        for (int j = 0; j < b->blockCount; j++) {
            if (blocksClash(&a->blocks[i], &b->blocks[j])) {
                return true;
            }
        }
    }
    return false;
}

void detectScheduleClashes(Catalog *catalog) {
    for (int c = 0; c < catalog->courseCount; c++) {
        catalog->courses[c].hasScheduleClash = 0;
        for (int g = 0; g < catalog->courses[c].groupCount; g++) {
            catalog->courses[c].groups[g].hasClash = 0;
        }
    }

    typedef struct {
        int courseIndex;
        int groupIndex;
    } GroupRef;

    int totalGroups = 0;
    for (int c = 0; c < catalog->courseCount; c++) {
        totalGroups += catalog->courses[c].groupCount;
    }

    if (totalGroups == 0) {
        return;
    }

    GroupRef *refs = malloc(sizeof(GroupRef) * totalGroups);
    if (refs == NULL) {
        return;
    }

    int idx = 0;
    for (int c = 0; c < catalog->courseCount; c++) {
        for (int g = 0; g < catalog->courses[c].groupCount; g++) {
            refs[idx].courseIndex = c;
            refs[idx].groupIndex = g;
            idx++;
        }
    }

    for (int i = 0; i < totalGroups; i++) {
        for (int j = i + 1; j < totalGroups; j++) {
            if (refs[i].courseIndex == refs[j].courseIndex) {
                continue;
            }

            Course *courseA = &catalog->courses[refs[i].courseIndex];
            Course *courseB = &catalog->courses[refs[j].courseIndex];
            CourseGroup *groupA = &courseA->groups[refs[i].groupIndex];
            CourseGroup *groupB = &courseB->groups[refs[j].groupIndex];

            if (groupsClash(groupA, groupB)) {
                groupA->hasClash = 1;
                groupB->hasClash = 1;
                courseA->hasScheduleClash = 1;
                courseB->hasScheduleClash = 1;
            }
        }
    }

    free(refs);
}
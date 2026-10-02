//
// scheduleClash.h
// Modulo de deteccion de choques de horario (Cuartil 2)
//

#ifndef SCHEDULECLASH_H
#define SCHEDULECLASH_H

#include <stdbool.h>
#include "types.h"

int timeToMinutes(const char *hhmm);


bool blocksClash(const ScheduleBlock *a, const ScheduleBlock *b);


bool groupsClash(const CourseGroup *a, const CourseGroup *b);


void detectScheduleClashes(Catalog *catalog);

#endif

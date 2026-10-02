//
// Created by Bryan on 8/28/2026.
//

#ifndef CATALOGLOADER_H
#define CATALOGLOADER_H
#include "types.h"
#include <stdbool.h>

int loadCatalog(const char *filePath, Catalog *catalog);
int parseCodeList(char *value, char list[][maxCourseCodeLen], int maxElements);
int parseGroupsField(char *value, CourseGroup groups[], int maxGroups);
const Course *findCourseByCode(const Catalog *catalog, const char *code);
bool charPrefix(const char *prefix, const char *str);

#endif
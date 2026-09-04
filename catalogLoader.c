//
// Created by Bryan on 8/28/2026.
//

#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "types.h"
#include "constants.h"
#include "catalogLoader.h"

bool charPrefix(const char *prefix, const char *str) {  // Para identificar a que tipo de dato corresponde cada linea de codigo
    size_t len = strlen(prefix);
    return strncmp(prefix, str, len) == 0;
}

int parseCodeList(char *value, char list[][maxCourseCodeLen], int maxElements) {  // Para tokenizar los datos de requisitos y corequisitos separados por comas
    int i = 0;
    char *rest;
    if (strcmp(value, "NINGUNO") == 0) return 0;
    char *token = strtok_r(value, ",", &rest);

    while (token != NULL && i < maxElements) {
        while (*token == ' ') token++;  // caso especial para los cursos CE1101 y CE1104
        strncpy(list[i], token, maxCourseCodeLen - 1);
        list[i][maxCourseCodeLen - 1] = '\0';
        i++;
        token = strtok_r(NULL, ",", &rest);
    }

    if (token != NULL) {
        fprintf(stderr, "Advertencia: se alcanzo el maximo de %d codigos, lista truncada\n", maxElements);
    }
    return i;
}

int parseGroupsField(char *value, CourseGroup groups[], int maxGroups) {  // Para tokenizar los datos de horario de grupo separados por ;
    if (strcmp(value, "NINGUNO") == 0) return 0;
    int i = 0;
    char *restGroups;
    char *token = strtok_r(value, ";", &restGroups);

    while (token != NULL && i < maxGroups) {
        char *openParen = strchr(token, '(');
        size_t idLen = openParen - token;
        if (idLen > maxGroupIdLen - 1) {
            idLen = maxGroupIdLen - 1;
            fprintf(stderr, "Advertencia: id de grupo truncado, excede maxGroupIdLen\n");
        }
        strncpy(groups[i].groupId, token, idLen);
        groups[i].groupId[idLen] = '\0';
        char *blocksContent = openParen + 1;
        blocksContent[strlen(blocksContent) - 1] = '\0';

        char *restBlocks;
        char *blockToken = strtok_r(blocksContent, ",", &restBlocks);
        int blockCount = 0;
        while (blockToken != NULL && blockCount < maxScheduleBlocks) {
            char *restFields;
            char *day = strtok_r(blockToken, "-", &restFields);
            char *start = strtok_r(NULL, "-", &restFields);
            char *end = strtok_r(NULL, "-", &restFields);

            strncpy(groups[i].blocks[blockCount].day, day, sizeof(groups[i].blocks[blockCount].day) - 1);
            groups[i].blocks[blockCount].day[sizeof(groups[i].blocks[blockCount].day) - 1] = '\0';
            strncpy(groups[i].blocks[blockCount].startTime, start, maxTimeLen - 1);
            groups[i].blocks[blockCount].startTime[maxTimeLen - 1] = '\0';
            strncpy(groups[i].blocks[blockCount].endTime, end, maxTimeLen - 1);
            groups[i].blocks[blockCount].endTime[maxTimeLen - 1] = '\0';

            blockCount++;
            blockToken = strtok_r(NULL, ",", &restBlocks);
        }

        groups[i].blockCount = blockCount;

        if (blockToken != NULL) {
            fprintf(stderr, "Advertencia: grupo '%s' supera el maximo de %d bloques, se trunco\n", groups[i].groupId, maxScheduleBlocks);
        }
        i++;
        token = strtok_r(NULL, ";", &restGroups);
    }

    if (token != NULL) {
        fprintf(stderr, "Advertencia: se alcanzo el maximo de %d grupos, lista truncada\n", maxGroups);
    }

    return i;
}

const Course *findCourseByCode(const Catalog *catalog, const char *code) {  // para buscar los datos de un curso a partir de su codigo identificador
    for (int i = 0; i < catalog->courseCount; i++) {
        if (strcmp(catalog->courses[i].courseCode, code) == 0) {
            return &catalog->courses[i];
        }
    }
    return NULL;
}

int loadCatalog(const char *filePath, Catalog *catalog) {  // Para cargar los archivos de malla curricular
    FILE *ptrCatalogFile = fopen(filePath, "r");
    if (ptrCatalogFile == NULL) {
        return -1;
    }

    char line[1024];
    catalog->courseCount = 0;
    Course tempCourse = {0};

    while (fgets(line, sizeof(line), ptrCatalogFile) != NULL) {

        if (strchr(line, '\n') == NULL && !feof(ptrCatalogFile)) {
            fprintf(stderr, "Advertencia: se alcanzo el maximo de %zu caracteres, linea truncada\n", sizeof(line));
        }
        line[strcspn(line, "\n")] = '\0';

        if (charPrefix("CODIGO:", line)) {
            char *value = line + strlen("CODIGO:");
            while (*value == ' ') value++;
            strncpy(tempCourse.courseCode, value, maxCourseCodeLen - 1);
            tempCourse.courseCode[maxCourseCodeLen - 1] = '\0';
        }
        else if (charPrefix("NOMBRE:", line)) {
            char *value = line + strlen("NOMBRE:");
            while (*value == ' ') value++;
            strncpy(tempCourse.courseName, value, maxCourseNameLen - 1);
            tempCourse.courseName[maxCourseNameLen - 1] = '\0';
        }
        else if (charPrefix("CREDITOS:", line)) {
            char *value = line + strlen("CREDITOS:");
            while (*value == ' ') value++;
            tempCourse.credits = atoi(value);
        }
        else if (charPrefix("REQUISITOS:", line)) {
            char *value = line + strlen("REQUISITOS:");
            while (*value == ' ') value++;
            tempCourse.requisiteCount = parseCodeList(value, tempCourse.requisites, maxRequisites);
        }
        else if (charPrefix("COREQUISITOS:", line)) {
            char *value = line + strlen("COREQUISITOS:");
            while (*value == ' ') value++;
            tempCourse.corequisiteCount = parseCodeList(value, tempCourse.corequisites, maxCorequisites);
        }
        else if (charPrefix("GRUPOS:", line)) {
            char *value = line + strlen("GRUPOS:");
            while (*value == ' ') value++;
            tempCourse.groupCount = parseGroupsField(value, tempCourse.groups, maxGroupsPerCourse);
        }
        else if (charPrefix("###", line)) {
            if (catalog->courseCount >= maxCourses) {
                fprintf(stderr, "Advertencia: se alcanzo maxCourses (%d), curso '%s' descartado\n",
                        maxCourses, tempCourse.courseCode);
            } else {
                catalog->courses[catalog->courseCount] = tempCourse;
                catalog->courseCount++;
            }
            tempCourse = (Course){0};
        }
    }

    fclose(ptrCatalogFile);
    return 0;
}
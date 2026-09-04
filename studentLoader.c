//
// Created by Bryan on 9/1/2026.
//

#include <stdio.h>
#include <string.h>
#include <stdbool.h>

#include "studentLoader.h"
#include "catalogLoader.h"
#include "types.h"
#include "constants.h"

int loadStudentHistory(const char *filePath, StudentHistory *history, const Catalog *catalog) {
    FILE *ptrStudentHistoryFile = fopen(filePath, "r");
    if (ptrStudentHistoryFile == NULL) {
        return -1;
    }

    history->approvedCount = 0;
    char line[1024];

    while (fgets(line, sizeof(line), ptrStudentHistoryFile) != NULL) {

        if (strchr(line, '\n') == NULL && !feof(ptrStudentHistoryFile)) {
            fprintf(stderr, "Advertencia: se alcanzo el maximo de %zu caracteres, linea truncada\n", sizeof(line));
        }
        line[strcspn(line, "\n")] = '\0';

        if (charPrefix("CARNET:", line)) {
            char *value = line + strlen("CARNET:");
            while (*value == ' ') value++;
            strncpy(history->studentId, value, maxStudentIdLen - 1);
            history->studentId[maxStudentIdLen - 1] = '\0';
        }
        else if (charPrefix("NOMBRE:", line)) {
            char *value = line + strlen("NOMBRE:");
            while (*value == ' ') value++;
            strncpy(history->studentName, value, maxStudentNameLen - 1);
            history->studentName[maxStudentNameLen - 1] = '\0';
        }
        else if (charPrefix("APROBADOS:", line)) {
            char *value = line + strlen("APROBADOS:");
            while (*value == ' ') value++;
            char rawList[maxHistory][maxCourseCodeLen];
            int rawCount = parseCodeList(value, rawList, maxHistory);

            for (int i = 0; i < rawCount; i++) {
                if (findCourseByCode(catalog, rawList[i]) == NULL) {
                    fprintf(stderr, "Advertencia: codigo '%s' del historial no existe en el catalogo, se omite\n", rawList[i]);
                    continue;
                }
                strncpy(history->approvedCourses[history->approvedCount], rawList[i], maxCourseCodeLen - 1);
                history->approvedCourses[history->approvedCount][maxCourseCodeLen - 1] = '\0';
                history->approvedCount++;
            }
        }
    }

    fclose(ptrStudentHistoryFile);
    return 0;
}
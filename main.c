#include <stdio.h>
#include <string.h>

#include "types.h"
#include "constants.h"
#include "catalogLoader.h"
#include "studentLoader.h"
#include "scheduleClash.h"
#include "Requisites.h"
#include "outputWriter.h"

void printCourse(const Course *c) {
    printf("Codigo: %s\n", c->courseCode);
    printf("Nombre: %s\n", c->courseName);
    printf("Creditos: %d\n", c->credits);

    printf("Requisitos (%d): ", c->requisiteCount);
    for (int i = 0; i < c->requisiteCount; i++) printf("%s ", c->requisites[i]);
    printf("\n");

    printf("Correquisitos (%d): ", c->corequisiteCount);
    for (int i = 0; i < c->corequisiteCount; i++) printf("%s ", c->corequisites[i]);
    printf("\n");

    printf("Grupos (%d):\n", c->groupCount);
    for (int i = 0; i < c->groupCount; i++) {
        printf("  Grupo %s, bloques: %d, choque: %s\n", c->groups[i].groupId,
               c->groups[i].blockCount, c->groups[i].hasClash ? "SI" : "no");
        for (int j = 0; j < c->groups[i].blockCount; j++) {
            printf("    %s %s-%s\n", c->groups[i].blocks[j].day,
                   c->groups[i].blocks[j].startTime, c->groups[i].blocks[j].endTime);
        }
    }
    printf("\n");
}

void printHistory(const StudentHistory *h) {
    printf("Carnet: %s\n", h->studentId);
    printf("Nombre: %s\n", h->studentName);
    printf("Aprobados (%d):\n", h->approvedCount);
    for (int i = 0; i < h->approvedCount; i++) {
        printf("  %s\n", h->approvedCourses[i]);
    }
    printf("\n");
}

void testCourse(const Catalog *catalog, const char *code) {
    const Course *course = findCourseByCode(catalog, code);
    if (course != NULL) {
        printCourse(course);
    }
}

void printClashSummary(const Catalog *cat) {
    for (int c = 0; c < cat->courseCount; c++) {
        const Course *course = &cat->courses[c];
        if (course->hasScheduleClash) {
            printf("CHOQUE en curso %s (%s):\n", course->courseCode, course->courseName);
            for (int g = 0; g < course->groupCount; g++) {
                if (course->groups[g].hasClash) {
                    printf("  - grupo con choque: %s\n", course->groups[g].groupId);
                }
            }
        }
    }
    printf("\n");
}

void testScheduleClashEdgeCases(void) {
    printf("Pruebas de casos de choque de horario:\n");

    ScheduleBlock a = { "MAR", "0730", "0920" };
    ScheduleBlock b = { "MAR", "0920", "1110" };
    printf("2 Cursos contiguos: Martes (07:30 a 09:20) y Martes (09:20 a 11:10): %s\n",
           blocksClash(&a, &b) ? "Chocan (Incorrecto)" : "No chocan (Correcto)");

    ScheduleBlock c1 = { "MAR", "0730", "0920" };
    ScheduleBlock d1 = { "JUE", "0730", "0920" };
    printf("Mismo horario, distinto dia: Martes (07:30 a 09:20) y Jueves (07:30 a 09:20): %s\n",
           blocksClash(&c1, &d1) ? "Chocan (Incorrecto)" : "No chocan (Correcto)");

    ScheduleBlock e = { "MAR", "0730", "0920" };
    ScheduleBlock f = { "MAR", "0800", "0850" };
    printf("Mismo dia, se sobrelapan los horarios: Martes (07:30 a 09:20) y Martes (08:00 a 08:50): %s\n",
           blocksClash(&e, &f) ? "Chocan (Correcto)" : "No chocan (Incorrecto)");

    printf("\n");
}

void testEligibility(const Catalog *catalog, const StudentHistory *history) {
    const char *article = (history->gender == 'M') ? "La" : "El";
    for (int c = 0; c < catalog->courseCount; c++) {
        const Course *course = &catalog->courses[c];
        if (course->canEnroll) {
            printf("%s estudiante %s puede llevar el curso: (%s)\n",
                   article, history->studentName, course->courseName);
        }
    }
}

void printCycles(const Catalog *catalog, const CycleReport cyclesOut[], int cycleCount) {
    for (int i = 0; i < cycleCount; i++) {
        const CycleReport *cycle = &cyclesOut[i];
        for (int j = 0; j < cycle->courseCount; j++) {
            int idx = cycle->courseIdx[j];
            printf("%s", catalog->courses[idx].courseCode);
            if (j < cycle->courseCount - 1) printf(" -> ");
        }
        printf(" -> %s\n", catalog->courses[cycle->courseIdx[0]].courseCode);
    }
    printf("\n");
}

void processCarrera(const char *catalogPath, const char *historyPath, const char *outputPath, const char *careerName) {
    Catalog catalog;
    StudentHistory history;
    CycleReport report[maxCycles];

    if (loadCatalog(catalogPath, &catalog) != 0) {
        fprintf(stderr, "Error: no se pudo cargar el catalogo (%s)\n", catalogPath);
        return;
    }
    printf("Cursos cargados: %d\n\n", catalog.courseCount);

    detectScheduleClashes(&catalog);
    printClashSummary(&catalog);

    if (loadStudentHistory(historyPath, &history, &catalog) != 0) {
        fprintf(stderr, "Error: no se pudo cargar el historial (%s)\n", historyPath);
        return;
    }
    printHistory(&history);

    computeEligibility(&catalog, &history);
    testEligibility(&catalog, &history);

    int cycleCount = detectCycles(&catalog, report);
    printCycles(&catalog, report, cycleCount);

    if (writeCatalogToJSON(outputPath, &catalog, careerName) != 0) {
        fprintf(stderr, "Error: no se pudo exportar el catalogo (%s)\n", outputPath);
        return;
    }
    printf("Se completo la creacion del JSON: %s (carrera: %s)\n", outputPath, careerName);
}

int main(int argc, char *argv[]) {
    setvbuf(stdout, NULL, _IOLBF, 0);

    const char *catalogPath = (argc > 1) ? argv[1] : CE_CATALOG_PATH;
    const char *historyPath = (argc > 2) ? argv[2] : CE_HISTORY_PATH;
    const char *outputPath  = (argc > 3) ? argv[3] : OUTPUT_PATH;
    const char *careerName  = (argc > 4) ? argv[4] : nombreCarrera1;

    printf("=== Catalogo cargado ===\n\n");
    processCarrera(catalogPath, historyPath, outputPath, careerName);

    return 0;
}
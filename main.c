#include <stdio.h>
#include <string.h>

#include "types.h"
#include "constants.h"
#include "catalogLoader.h"
#include "studentLoader.h"
#include "scheduleClash.h"
#include "Requisites.h"

#define CE_CATALOG_PATH   PROJECT_ROOT_PATH "PlanEstudioCE.txt"
#define CE_HISTORY_PATH   PROJECT_ROOT_PATH "HistorialEstudianteCE.txt"
#define IF_CATALOG_PATH   PROJECT_ROOT_PATH "PlanEstudioFI.txt"
#define IF_HISTORY_PATH   PROJECT_ROOT_PATH "HistorialEstudianteFI.txt"

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
    } else {
        fprintf(stderr, "No se encontro %s\n", code);
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
    ScheduleBlock d1 = { "JUE", "0730", "0920" }; //
    printf("Mismo horario, distinto dia: Martes (07:30 a 09:20) y Jueves (07:30 a 09:20): %s\n",
           blocksClash(&c1, &d1) ? "Chocan (Incorrecto)" : "No chocan (Correcto)");

    ScheduleBlock e = { "MAR", "0730", "0920" };
    ScheduleBlock f = { "MAR", "0800", "0850" }; //
    printf("Mismo dia, se sobrelapan los horarios: Martes (07:30 a 09:20) y Martes (08:00 a 08:50): %s\n",
           blocksClash(&e, &f) ? "Chocan (Correcto)" : "No chocan (Incorrecto)");

    printf("\n");
}

void testEligibility(const Catalog *catalog, const StudentHistory *history){
    for (int c = 0; c < catalog->courseCount; c++) {
            const Course *course = &catalog->courses[c];
            if (course->canEnroll) {
                printf("El estudiante %s puede llevar el curso: (%s)\n", history->studentName, course->courseName);
            }
        }
}

void printCycles(const Catalog *catalog, const CycleReport cyclesOut[], int cycleCount) {
    for (int i = 0; i < cycleCount; i++) {
        const CycleReport *cycle = &cyclesOut[i];

        for (int j = 0; j < cycle->courseCount; j++) {
            int idx = cycle->courseIdx[j];
            printf("%s", catalog->courses[idx].courseCode);

            if ( j < cycle->courseCount-1) {
                printf(" -> ");
            }
        }

        printf(" -> %s\n", catalog->courses[cycle->courseIdx[0]].courseCode);
    }
    printf("\n");
}

int main(void) {
    setvbuf(stdout, NULL, _IOLBF, 0);

    Catalog ceCatalog;
    Catalog ifCatalog;
    StudentHistory ceHistory;
    StudentHistory ifHistory;
    CycleReport ifReport[maxCycles];
    CycleReport ceReport[maxCycles];

    printf("=== Catalogo Computadores ===\n\n");
    if (loadCatalog(CE_CATALOG_PATH, &ceCatalog) != 0) {
        fprintf(stderr, "Error: no se pudo cargar el catalogo de Computadores\n");
        return 1;
    }
    printf("Cursos cargados: %d\n\n", ceCatalog.courseCount);

    detectScheduleClashes(&ceCatalog);
    printClashSummary(&ceCatalog);

    testCourse(&ceCatalog, "SE1100");
    testCourse(&ceCatalog, "CE1103");
    testCourse(&ceCatalog, "FH1000");
    testCourse(&ceCatalog, "QU1102");

    if (loadStudentHistory(CE_HISTORY_PATH, &ceHistory, &ceCatalog) != 0) {
        fprintf(stderr, "Error: no se pudo cargar el historial de Computadores\n");
        return 1;
    }
    printHistory(&ceHistory);

    printf("=== Catalogo Ingenieria Fisica ===\n\n");
    if (loadCatalog(IF_CATALOG_PATH, &ifCatalog) != 0) {
        fprintf(stderr, "Error: no se pudo cargar el catalogo de Ingenieria Fisica\n");
        return 1;
    }
    printf("Cursos cargados: %d\n\n", ifCatalog.courseCount);

    detectScheduleClashes(&ifCatalog);
    printClashSummary(&ifCatalog);

    testCourse(&ifCatalog, "CI1107");
    testCourse(&ifCatalog, "IF3502");
    testCourse(&ifCatalog, "SE1100");
    testCourse(&ifCatalog, "MT2002");

    if (loadStudentHistory(IF_HISTORY_PATH, &ifHistory, &ifCatalog) != 0) {
        fprintf(stderr, "Error: no se pudo cargar el historial de Ingenieria Fisica\n");
        return 1;
    }
    printHistory(&ifHistory);

    testScheduleClashEdgeCases();

    computeEligibility(&ifCatalog, &ifHistory);
    testEligibility(&ifCatalog, &ifHistory);

    computeEligibility(&ceCatalog, &ceHistory);
    testEligibility(&ceCatalog, &ceHistory);


    int ceCycleCount = detectCycles(&ceCatalog, ceReport);
    printCycles(&ceCatalog, ceReport, ceCycleCount);

    int ifCycleCount = detectCycles(&ifCatalog, ifReport);
    printCycles(&ifCatalog, ifReport, ifCycleCount);


    return 0;
}
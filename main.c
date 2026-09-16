#include <stdio.h>
#include <string.h>

#include "types.h"
#include "constants.h"
#include "catalogLoader.h"
#include "studentLoader.h"



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
        printf("  Grupo %s, bloques: %d\n", c->groups[i].groupId, c->groups[i].blockCount);
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

void testCourse(const Catalog *catalog, const char *code) {  // Casos de prueba para las implementacioens actuales de studentLoader y historyLoader
    const Course *course = findCourseByCode(catalog, code);
    if (course != NULL) {
        printCourse(course);
    } else {
        fprintf(stderr, "No se encontro %s\n", code);
    }
}

int main(void) {
    setvbuf(stdout, NULL, _IOLBF, 0);

    Catalog ceCatalog;
    Catalog ifCatalog;
    StudentHistory ceHistory;
    StudentHistory ifHistory;

    printf("=== Catalogo Computadores ===\n\n");
    if (loadCatalog(CE_CATALOG_PATH, &ceCatalog) != 0) {
        fprintf(stderr, "Error: no se pudo cargar el catalogo de Computadores\n");
        return 1;
    }
    printf("Cursos cargados: %d\n\n", ceCatalog.courseCount);

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

    testCourse(&ifCatalog, "CI1107");
    testCourse(&ifCatalog, "IF3502"); 
    testCourse(&ifCatalog, "SE1100");
    testCourse(&ifCatalog, "MT2002");

    if (loadStudentHistory(IF_HISTORY_PATH, &ifHistory, &ifCatalog) != 0) {
        fprintf(stderr, "Error: no se pudo cargar el historial de Ingenieria Fisica\n");
        return 1;
    }
    printHistory(&ifHistory);

    return 0;
}

#include <stdio.h>
#include <string.h>

#include "types.h"
#include "constants.h"
#include "catalogLoader.h"
#include "studentLoader.h"

void printCourse(const Course *c) { // Prueba rapida para mostrar que el programa puede leer el
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


int main(void) {
    Catalog catalog;
    int result = loadCatalog("PlanEstudioCE.txt", &catalog);
    if (result != 0) {
        fprintf(stderr, "Error: no se pudo cargar el catalogo\n");
        return 1;
    }

    StudentHistory history;
    int historyResult = loadStudentHistory("HistorialEstudiante.txt", &history, &catalog);
    if (historyResult != 0) {
        fprintf(stderr, "Error: no se pudo cargar el historial\n");
        return 1;
    }
    printf("Carnet: %s\n", history.studentId);
    printf("Nombre: %s\n", history.studentName);
    printf("Aprobados (%d):\n", history.approvedCount);
    for (int i = 0; i < history.approvedCount; i++) {
        printf("  %s\n", history.approvedCourses[i]);
    }
    printf("Cursos cargados: %d\n\n", catalog.courseCount);

    const Course *se1100 = findCourseByCode(&catalog, "SE1100");
    if (se1100 != NULL) {
        printCourse(se1100);
    } else {
        fprintf(stderr, "No se encontro SE1100\n");
    }

    const Course *ce1103 = findCourseByCode(&catalog, "CE1103");
    if (ce1103 != NULL) {
        printCourse(ce1103);
    } else {
        fprintf(stderr, "No se encontro CE1103\n");
    }

    const Course *fh1000 = findCourseByCode(&catalog, "FH1000");
    if (fh1000 != NULL) printCourse(fh1000);

    return 0;
}
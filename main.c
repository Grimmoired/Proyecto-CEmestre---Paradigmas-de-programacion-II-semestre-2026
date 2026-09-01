#include <stdio.h>
#include <string.h>

#include "types.h"
#include "constants.h"
#include "catalogLoader.h"

void printCourse(const Course *c) { // Prueba rapida para mostrar que el programa puede leer el
    printf("Codigo: %s\n", c->courseCode);
    printf("Nombre: %s\n", c->courseName);
    printf("Creditos: %d\n", c->credits);

    printf("Requisitos (%d): ", c->requisiteCount);
    for (int i = 0; i < c->requisiteCount; i++) printf("%s ", c->requisites[i]);
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

const Course *findCourseByCode(const Catalog *catalog, const char *code) {  // para buscar los datos de un curso a partir de su codigo identificador
    for (int i = 0; i < catalog->courseCount; i++) {
        if (strcmp(catalog->courses[i].courseCode, code) == 0) {
            return &catalog->courses[i];
        }
    }
    return NULL;
}

int main(void) {
    Catalog catalog;
    int result = loadCatalog("PlanEstudioCE.txt", &catalog);
    if (result != 0) {
        fprintf(stderr, "Error: no se pudo cargar el catalogo\n");
        return 1;
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

    return 0;
}
#include <stdio.h>
#include "types.h"
#include "constants.h"
#include "catalogLoader.h"
#include "scheduleClash.h"

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
}

int main(void) {
    Catalog cat;
    if (loadCatalog("PlanEstudioCE.txt", &cat) != 0) {
        fprintf(stderr, "no pude cargar el catalogo\n");
        return 1;
    }
    printf("Cursos cargados: %d\n\n", cat.courseCount);

    detectScheduleClashes(&cat);
    printClashSummary(&cat);

    
    printf("\n--- Pruebas de casos limite ---\n");

    ScheduleBlock a = { "MAR", "0730", "0920" };
    ScheduleBlock b = { "MAR", "0920", "1110" }; // termina cuando el otro empieza
    printf("Mismo dia, contiguo (no deberia chocar): %s\n", blocksClash(&a, &b) ? "CHOCA (mal)" : "OK, no choca");

    ScheduleBlock c1 = { "MAR", "0730", "0920" };
    ScheduleBlock d1 = { "JUE", "0730", "0920" }; // mismo horario, distinto dia
    printf("Mismo horario, distinto dia (no deberia chocar): %s\n", blocksClash(&c1, &d1) ? "CHOCA (mal)" : "OK, no choca");

    ScheduleBlock e = { "MAR", "0730", "0920" };
    ScheduleBlock f = { "MAR", "0800", "0850" }; // uno contenido dentro del otro
    printf("Mismo dia, uno contenido en el otro (deberia chocar): %s\n", blocksClash(&e, &f) ? "OK, choca" : "NO choca (mal)");

    return 0;
}

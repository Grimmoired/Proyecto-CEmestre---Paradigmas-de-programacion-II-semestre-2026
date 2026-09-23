#include <stdio.h>
#include "outputWriter.h"
#include "constants.h"

static void writeScheduleBlock(FILE *f, const ScheduleBlock *b) {
    fprintf(f, "{\"day\": \"%s\", \"startTime\": \"%s\", \"endTime\": \"%s\"}",
            b->day, b->startTime, b->endTime);
}

static void writeGroup(FILE *f, const CourseGroup *g) {
    fprintf(f, "{\"groupId\": \"%s\", \"hasClash\": %s, \"blocks\": [",
            g->groupId, g->hasClash ? "true" : "false");
    for (int i = 0; i < g->blockCount; i++) {
        if (i > 0) fprintf(f, ", ");
        writeScheduleBlock(f, &g->blocks[i]);
    }
    fprintf(f, "]}");
}

static void writeStringArray(FILE *f, const char arr[][maxCourseCodeLen], int count) {
    fprintf(f, "[");
    for (int i = 0; i < count; i++) {
        if (i > 0) fprintf(f, ", ");
        fprintf(f, "\"%s\"", arr[i]);
    }
    fprintf(f, "]");
}

static void writeCourse(FILE *f, const Course *c) {
    fprintf(f, "{\n");
    fprintf(f, "  \"courseCode\": \"%s\",\n", c->courseCode);
    fprintf(f, "  \"courseName\": \"%s\",\n", c->courseName);
    fprintf(f, "  \"credits\": %d,\n", c->credits);
    fprintf(f, "  \"requisites\": ");
    writeStringArray(f, c->requisites, c->requisiteCount);
    fprintf(f, ",\n");
    fprintf(f, "  \"corequisites\": ");
    writeStringArray(f, c->corequisites, c->corequisiteCount);
    fprintf(f, ",\n");
    fprintf(f, "  \"groups\": [\n");
    for (int i = 0; i < c->groupCount; i++) {
        if (i > 0) fprintf(f, ",\n");
        fprintf(f, "    ");
        writeGroup(f, &c->groups[i]);
    }
    fprintf(f, "\n  ],\n");
    fprintf(f, "  \"hasScheduleClash\": %s,\n", c->hasScheduleClash ? "true" : "false");
    fprintf(f, "  \"canEnroll\": %s,\n", c->canEnroll ? "true" : "false");
    fprintf(f, "  \"hasCycle\": %s\n", c->hasCycle ? "true" : "false");
    fprintf(f, "}");
}

int writeCatalogToJSON(const char *filePath, const Catalog *catalog) {
    FILE *f = fopen(filePath, "w");
    if (f == NULL) {
        fprintf(stderr, "Error: no se pudo abrir el archivo de salida %s\n", filePath);
        return -1;
    }
    fprintf(f, "{\n  \"courses\": [\n");
    for (int i = 0; i < catalog->courseCount; i++) {
        if (i > 0) fprintf(f, ",\n");
        writeCourse(f, &catalog->courses[i]);
    }
    fprintf(f, "\n  ]\n}\n");
    fclose(f);
    return 0;
}
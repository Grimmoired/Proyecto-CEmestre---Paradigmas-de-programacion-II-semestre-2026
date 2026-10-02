#ifndef STUDENTLOADER_H
#define STUDENTLOADER_H

#include "types.h"

int loadStudentHistory(const char *filePath, StudentHistory *history, const Catalog *catalog);
#endif
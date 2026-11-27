@echo off
rem Recorrido completo de CEmestre: C -> Racket -> Prolog -> Java.
rem Uso:   run_all.bat [preferencias.rktd] [politica.pl] [historialCE.txt] [historialFI.txt]
rem Todos los argumentos son opcionales; sin ellos se usan los archivos de ejemplo del repositorio.
setlocal

set "ROOT=%~dp0"
set "ROOT=%ROOT:~0,-1%"
set "C_DIR=%ROOT%\C - Etapa 1"
set "RACKET_DIR=%ROOT%\Racket - Etapa 2"
set "PROLOG_DIR=%ROOT%\Prolog - Etapa 3"
set "JAVA_DIR=%ROOT%\Java - Etapa 4"
set "OUT=%ROOT%\salidas"

set "PREFS=%RACKET_DIR%\PreferenciasAmbasCarreras.rktd"
if not "%~1"=="" if not "%~1"=="-" set "PREFS=%~f1"
set "POLICY=%PROLOG_DIR%\policy.pl"
if not "%~2"=="" if not "%~2"=="-" set "POLICY=%~f2"
set "HIST_CE=%C_DIR%\HistorialEstudianteCE.txt"
if not "%~3"=="" if not "%~3"=="-" set "HIST_CE=%~f3"
set "HIST_FI=%C_DIR%\HistorialEstudianteFI.txt"
if not "%~4"=="" if not "%~4"=="-" set "HIST_FI=%~f4"

rem Revisiones antes de empezar: detectan argumentos en el orden equivocado.
if /i not "%PREFS:~-5%"==".rktd" (set "MSG=el 1er argumento debe ser un archivo de preferencias .rktd, llego: %PREFS%" & goto :fail)
if /i not "%POLICY:~-3%"==".pl" (set "MSG=el 2do argumento debe ser un archivo de politica .pl, llego: %POLICY%" & goto :fail)
for %%F in ("%PREFS%" "%POLICY%" "%HIST_CE%" "%HIST_FI%") do (
    if not exist "%%~F" (set "MSG=no existe el archivo %%~F" & goto :fail)
)

if not exist "%OUT%" mkdir "%OUT%"
rem Se borran las salidas anteriores para no usar por error las de otra corrida.
del /q "%OUT%\*.json" 2>nul

echo.
echo === 1/4  C: catalogos con choques y elegibilidad ===
rem Se configura si todavia no hay Makefile (si una configuracion anterior fallo, se reintenta).
if not exist "%C_DIR%\build\Makefile" (
    cmake -S "%C_DIR%" -B "%C_DIR%\build" -G "MinGW Makefiles" > "%OUT%\compilacion_c.log" 2>&1 || (set "MSG=no se pudo configurar el proyecto de C, ver salidas\compilacion_c.log. Si el log menciona rutas de mas de 250 caracteres, mueva el proyecto a una carpeta corta como C:\cemestre y borre la carpeta C - Etapa 1\build" & goto :fail)
)
cmake --build "%C_DIR%\build" >> "%OUT%\compilacion_c.log" 2>&1 || (set "MSG=no se pudo compilar C, ver salidas\compilacion_c.log" & goto :fail)
set "EXE=%C_DIR%\build\Cemestre.exe"

"%EXE%" "%C_DIR%\PlanEstudioCE.txt" "%HIST_CE%" "%OUT%\catalogoCE.json" "Ingenieria en Computadores" > "%OUT%\etapa1_CE.log" 2>&1
if not exist "%OUT%\catalogoCE.json" (set "MSG=C no genero catalogoCE.json, ver salidas\etapa1_CE.log" & goto :fail)
"%EXE%" "%C_DIR%\PlanEstudioFI.txt" "%HIST_FI%" "%OUT%\catalogoFI.json" "Ingenieria Fisica" > "%OUT%\etapa1_FI.log" 2>&1
if not exist "%OUT%\catalogoFI.json" (set "MSG=C no genero catalogoFI.json, ver salidas\etapa1_FI.log" & goto :fail)
echo catalogoCE.json y catalogoFI.json listos

echo.
echo === 2/4  Racket: combinaciones ===
echo Preferencias: %PREFS%
racket "%RACKET_DIR%\main.rkt" "%PREFS%" -c "%OUT%\catalogoCE.json" -c "%OUT%\catalogoFI.json" -o "%OUT%\combinaciones.json"
if errorlevel 1 (set "MSG=Racket no pudo generar las combinaciones" & goto :fail)

echo.
echo === 3/4  Prolog: reglas de politica ===
echo Politica: %POLICY%
swipl "%PROLOG_DIR%\main.pl" "%OUT%\combinaciones.json" "%POLICY%" "%OUT%\resultados.json"
if errorlevel 1 (set "MSG=Prolog no pudo evaluar las combinaciones" & goto :fail)

echo.
echo === 4/4  Java: interfaz ===
set "JAR=%JAVA_DIR%\target\cemestre.jar"
if not exist "%JAR%" (set "MSG=no existe el jar. Compilelo una vez con mvn clean package en la carpeta Java - Etapa 4" & goto :fail)
java -jar "%JAR%" "%OUT%\resultados.json"
exit /b 0

:fail
echo.
echo ERROR: %MSG%
echo El recorrido se detuvo.
exit /b 1
#!/usr/bin/env bash
# Recorrido completo de CEmestre: C -> Racket -> Prolog -> Java.
# Uso:   ./run_all.sh [preferencias.rktd] [politica.pl] [historialCE.txt] [historialFI.txt]
# Todos los argumentos son opcionales; sin ellos se usan los archivos de ejemplo del repositorio.
# Las salidas intermedias quedan en la carpeta salidas/.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
C_DIR="$ROOT/C - Etapa 1"
RACKET_DIR="$ROOT/Racket - Etapa 2"
PROLOG_DIR="$ROOT/Prolog - Etapa 3"
JAVA_DIR="$ROOT/Java - Etapa 4"
OUT="$ROOT/salidas"

fail() { echo "ERROR: $1" >&2; echo "El recorrido se detuvo." >&2; exit 1; }
step() { echo; echo "=== $1 ==="; }

# arg_or_default VALOR DEFECTO: devuelve DEFECTO si VALOR esta vacio o es "-"
arg_or_default() { if [ -z "$1" ] || [ "$1" = "-" ]; then echo "$2"; else echo "$1"; fi; }

PREFS="$(arg_or_default "${1:-}" "$RACKET_DIR/PreferenciasAmbasCarreras.rktd")"
POLICY="$(arg_or_default "${2:-}" "$PROLOG_DIR/policy.pl")"
HIST_CE="$(arg_or_default "${3:-}" "$C_DIR/HistorialEstudianteCE.txt")"
HIST_FI="$(arg_or_default "${4:-}" "$C_DIR/HistorialEstudianteFI.txt")"

# Revisiones antes de empezar: detectan argumentos en el orden equivocado.
[[ "$PREFS" == *.rktd ]] || fail "el 1er argumento debe ser un archivo de preferencias .rktd (llego: $PREFS)"
[[ "$POLICY" == *.pl ]]  || fail "el 2do argumento debe ser un archivo de politica .pl (llego: $POLICY)"
for f in "$PREFS" "$POLICY" "$HIST_CE" "$HIST_FI"; do
    [ -f "$f" ] || fail "no existe el archivo $f"
done

mkdir -p "$OUT"
rm -f "$OUT"/*.json        # asi nunca se usa por error una salida de una corrida anterior

step "1/4  C: catalogos con choques y elegibilidad"
[ -f "$C_DIR/build/CMakeCache.txt" ] || cmake -S "$C_DIR" -B "$C_DIR/build" > "$OUT/compilacion_c.log" \
    || fail "no se pudo configurar el proyecto de C (ver salidas/compilacion_c.log)"
cmake --build "$C_DIR/build" >> "$OUT/compilacion_c.log" || fail "no se pudo compilar C (ver salidas/compilacion_c.log)"
EXE="$C_DIR/build/Cemestre"

"$EXE" "$C_DIR/PlanEstudioCE.txt" "$HIST_CE" "$OUT/catalogoCE.json" "Ingenieria en Computadores" > "$OUT/etapa1_CE.log"
[ -s "$OUT/catalogoCE.json" ] || fail "C no genero catalogoCE.json (ver salidas/etapa1_CE.log)"
"$EXE" "$C_DIR/PlanEstudioFI.txt" "$HIST_FI" "$OUT/catalogoFI.json" "Ingenieria Fisica" > "$OUT/etapa1_FI.log"
[ -s "$OUT/catalogoFI.json" ] || fail "C no genero catalogoFI.json (ver salidas/etapa1_FI.log)"
echo "catalogoCE.json y catalogoFI.json listos"

step "2/4  Racket: combinaciones (preferencias: $(basename "$PREFS"))"
racket "$RACKET_DIR/main.rkt" "$PREFS" -c "$OUT/catalogoCE.json" -c "$OUT/catalogoFI.json" -o "$OUT/combinaciones.json" \
    || fail "Racket no pudo generar las combinaciones"

step "3/4  Prolog: reglas de politica (politica: $(basename "$POLICY"))"
swipl "$PROLOG_DIR/main.pl" "$OUT/combinaciones.json" "$POLICY" "$OUT/resultados.json" \
    || fail "Prolog no pudo evaluar las combinaciones"

step "4/4  Java: interfaz"
JAR="$JAVA_DIR/target/cemestre.jar"
[ -f "$JAR" ] || fail "no existe $JAR. Compilelo una vez con: mvn clean package (en la carpeta Java - Etapa 4)"
java -jar "$JAR" "$OUT/resultados.json"
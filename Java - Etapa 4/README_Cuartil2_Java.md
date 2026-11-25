# Etapa 4, Cuartil 2: modelo de dominio, jerarquías y Factory

## Qué se agregó o cambió

| Archivo (`src/main/java/cemestre/...`) | Estado | Qué hace |
|---|---|---|
| `model/WeeklySchedule.java` | nuevo | Horario semanal: día → bloques ordenados por hora. Es lo que dibuja la vista |
| `model/PlacedBlock.java` | nuevo | Un bloque ya ubicado con su curso (código, nombre, grupo, día, horas) |
| `model/ScheduleMetrics.java` | nuevo | Días, huecos, hora promedio de inicio y jornada más larga |
| `model/CombinationBuilder.java` | nuevo | Patrón Builder (opcional): arma una combinación válida o rechazada |
| `model/reason/RejectionReasonFactory.java` | nuevo | Patrón Factory: elige la subclase de razón según `rule` |
| `model/reason/ReasonData.java` | nuevo | Interfaz para que la Factory lea campos sin conocer el JSON |
| `model/Combination.java` | cambiado | Guarda horario, métricas y créditos por carrera; agrega `getStatusDetail()` y `getRemovable()` abstractos, `equals` y `hashCode` |
| `model/ValidCombination.java`, `RejectedCombination.java` | cambiado | Implementan los métodos nuevos |
| `model/CourseSelection.java`, `IncompatiblePair.java` | cambiado | `equals` y `hashCode` |
| `model/reason/BalanceReason.java`, `AreaReason.java`, `IncompatibilityReason.java` | cambiado | `equals`, `hashCode` y validación de nulos |
| `io/PrologJsonAdapter.java` | cambiado | `readReason` ahora usa la Factory en vez del `switch` (los mensajes de error no cambian) |

Pruebas nuevas en `src/test/java/cemestre/model/`: `ScheduleMetricsTest`, `WeeklyScheduleTest`, `CombinationTest`, `RealDataMetricsTest`, `ModelDependenciesTest`, `reason/RejectionReasonFactoryTest` y la clase de ayuda `TestCourses`.

Resultado: **91 pruebas, 0 fallos** (las 40 del Cuartil 1 siguen pasando).

## Definición de las métricas (para el Cuartil 3 y el README)

Todas están en `ScheduleMetrics` y se calculan **una vez** al crear la combinación (`combination.getMetrics()`). Sirven para ordenar, no para validar.

| Métrica | Método | Definición |
|---|---|---|
| Días | `getDayCount()` | Cantidad de días distintos con al menos un bloque |
| Huecos | `getGapMinutes()` | Suma de los minutos libres entre bloques seguidos del mismo día. Un hueco de 10 minutos o menos (`DEFAULT_MIN_GAP_MINUTES`) **no cuenta**. Entre días distintos no hay hueco |
| Hora promedio de inicio | `getAverageStartMinutes()` | Promedio de la hora de inicio de la **primera** lección de cada día con clases, en minutos desde la medianoche (`double`) |
| Jornada más larga | `getLongestDayMinutes()` | Duración del día más largo, desde que empieza la primera lección hasta que termina la última (para el criterio propio del grupo) |

Detalles:
- Los bloques de cada día se ordenan por inicio antes de calcular.
- Si dos bloques se traslapan, el hueco nunca es negativo. Se mide desde el final más tardío visto hasta ese momento. Java **no detecta** choques.
- Una combinación sin bloques tiene todo en 0 (`hasClasses()` es `false`).
- El hueco mínimo se puede cambiar con `ScheduleMetrics.from(schedule, minutos)`.

Ejemplo, combinación 1 de `outputPrologMixto.json`: 3 días, 630 min de huecos, inicio promedio 560 (09:20) y jornada más larga 800 min. Se revisó a mano y con un script independiente.

Para los criterios de orden (Strategy), un ejemplo:
```java
Comparator.comparingInt((Combination c) -> c.getMetrics().getDayCount())
          .thenComparingInt(Combination::getId);   // empate por id
```

## Contrato para la interfaz (Cuartil 4)

Todo se usa por el tipo base `Combination`, **sin `instanceof`**:

| Método | Válida | Rechazada |
|---|---|---|
| `isValid()` | `true` | `false` |
| `getStatusLabel()` | `"Valida"` | `"Rechazada"` |
| `getStatusDetail()` | `"Cumple todas las reglas de politica"` | `reason.describe()` |
| `getRemovable()` | lista vacía | cursos que Prolog sugiere quitar |
| `getCreditsByCareer()` | créditos por carrera | igual |
| `getSchedule().getBlocks(Day.MON)` | bloques del lunes ordenados | igual |

Para el rango del dibujo: `getSchedule().getEarliestStartMinutes()` y `getLatestEndMinutes()`.

## Patrones de este cuartil

- **Factory**: `RejectionReasonFactory.create(rule, message, data)` es el único lugar donde se mira el nombre de la regla. Agregar una regla nueva es crear su subclase y agregar un caso.
- **Builder** (opcional): `CombinationBuilder`. Si se le da una razón crea una `RejectedCombination`; si no, una `ValidCombination`.
- `model` no importa Swing, AWT ni otros paquetes del proyecto, y no hay `getInstance`. Lo revisa `ModelDependenciesTest`.

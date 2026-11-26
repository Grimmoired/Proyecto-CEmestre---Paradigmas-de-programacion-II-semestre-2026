# Cuartil 3: diagramas y algoritmos (Strategy y Observer)

Material para el documento de Diseño (DI). Cada imagen PNG se genera con el codigo Mermaid
que aparece debajo de su titulo; para cambiar un diagrama, se edita ese bloque y se vuelve a
generar (por ejemplo en https://mermaid.live).

## 1. Patron Strategy: criterios de orden (`strategy.png`)

**Donde se aplica:** paquete `cemestre.sorting`.
**Problema que resuelve:** el usuario cambia el criterio de orden en ejecucion sin reiniciar y sin
cadenas `if/else` sobre el nombre del criterio.

- `SortCriterion` es la estrategia: lo unico que conoce quien ordena (`AppState`).
- `MetricCriterion` define una sola vez el desempate por id; cada subclase solo aporta su metrica
  en `primary()`.
- Las 4 estrategias concretas son intercambiables: cambiar de criterio es pasar otro objeto.
- `SortCriteria.available()` es el unico lugar donde se listan; crea objetos nuevos en cada
  llamada.

```mermaid
classDiagram
    direction TB
    class SortCriterion {
        <<interface>>
        +getName() String
        +comparator() Comparator~Combination~
    }
    class MetricCriterion {
        <<abstract>>
        -name : String
        #MetricCriterion(name : String)
        #primary()* Comparator~Combination~
        +getName() String
        +comparator() Comparator~Combination~
        +toString() String
    }
    class FewestDaysCriterion {
        #primary() Comparator~Combination~
    }
    class FewestGapsCriterion {
        #primary() Comparator~Combination~
    }
    class EarliestAverageStartCriterion {
        #primary() Comparator~Combination~
    }
    class ShortestLongestDayCriterion {
        #primary() Comparator~Combination~
    }
    class SortCriteria {
        +available()$ List~SortCriterion~
    }
    class AppState {
        -criterion : SortCriterion
        +setCriterion(c : SortCriterion) void
        +getCriterion() SortCriterion
        -refreshVisible() void
    }
    class Combination {
        <<abstract>>
        +getId() int
        +getMetrics() ScheduleMetrics
    }
    class ScheduleMetrics {
        +getDayCount() int
        +getGapMinutes() int
        +getAverageStartMinutes() double
        +getLongestDayMinutes() int
    }

    SortCriterion <|.. MetricCriterion
    MetricCriterion <|-- FewestDaysCriterion
    MetricCriterion <|-- FewestGapsCriterion
    MetricCriterion <|-- EarliestAverageStartCriterion
    MetricCriterion <|-- ShortestLongestDayCriterion
    AppState o--> "1" SortCriterion : criterio actual
    SortCriteria ..> SortCriterion : crea
    MetricCriterion ..> Combination : compara
    Combination --> "1" ScheduleMetrics
```

## 2. Patron Observer: estado y vistas (`observer.png`)

**Donde se aplica:** paquetes `cemestre.observer` y `cemestre.state` (sujeto) y `cemestre.view`
(observadores).
**Problema que resuelve:** al cambiar el criterio, el filtro o la seleccion, todas las vistas se
actualizan sin que el estado conozca las vistas y sin que una vista llame a otra.

- `AppState` es el sujeto: guarda una lista de `StateObserver` y les avisa cada cambio.
- El evento (`StateEvent`) solo dice que cambio; cada vista le pide a `AppState` los datos que
  necesita (modelo *pull*).
- `AppState` no importa Swing ni AWT; lo verifica la prueba `LayerDependenciesTest`.
- Las clases marcadas como cuartil 4 son las vistas que se registran como observadores.

```mermaid
classDiagram
    direction TB
    class StateObserver {
        <<interface>>
        +onStateChanged(event : StateEvent) void
    }
    class StateEvent {
        <<enumeration>>
        DATA_LOADED
        CRITERION_CHANGED
        FILTER_CHANGED
        SELECTION_CHANGED
    }
    class AppState {
        -observers : List~StateObserver~
        -data : CombinationSet
        -criterion : SortCriterion
        -filter : CombinationFilter
        -visible : List~Combination~
        -selected : Combination
        +AppState(initialCriterion : SortCriterion)
        +addObserver(o : StateObserver) void
        +removeObserver(o : StateObserver) void
        +load(data : CombinationSet) void
        +setCriterion(c : SortCriterion) void
        +setFilter(f : CombinationFilter) void
        +select(c : Combination) void
        +getVisibleCombinations() List~Combination~
        +getSelected() Optional~Combination~
        -notifyObservers(event : StateEvent) void
        -refreshVisible() void
    }
    class CombinationFilter {
        <<enumeration>>
        ALL
        VALID
        REJECTED
        +accepts(c : Combination) boolean
    }
    class CombinationTableModel {
        <<cuartil 4>>
        +onStateChanged(event : StateEvent) void
    }
    class ScheduleView {
        <<cuartil 4>>
        +onStateChanged(event : StateEvent) void
    }
    class CreditSummaryView {
        <<cuartil 4>>
        +onStateChanged(event : StateEvent) void
    }

    AppState o--> "0..*" StateObserver : avisa a
    AppState ..> StateEvent : envia
    AppState --> "1" CombinationFilter
    StateObserver ..> StateEvent : recibe
    StateObserver <|.. CombinationTableModel
    StateObserver <|.. ScheduleView
    StateObserver <|.. CreditSummaryView
    CombinationTableModel --> AppState : consulta
    ScheduleView --> AppState : consulta
    CreditSummaryView --> AppState : consulta
```

## 3. Secuencia: seleccionar una combinacion (`secuencia_seleccion.png`)

Muestra las dos vistas sincronizadas que pide el enunciado: el horario y el resumen de creditos
se actualizan por el mismo aviso, y ninguna llama a la otra.

```mermaid
sequenceDiagram
    actor U as Usuario
    participant T as Tabla (CombinationTableModel)
    participant S as AppState
    participant H as ScheduleView
    participant R as CreditSummaryView

    U->>T: clic en la fila de la combinacion 86
    T->>S: select(combinacion 86)
    Note over S: guarda la seleccion
    S->>T: onStateChanged(SELECTION_CHANGED)
    S->>H: onStateChanged(SELECTION_CHANGED)
    H->>S: getSelected()
    S-->>H: combinacion 86
    Note over H: redibuja el horario semanal
    S->>R: onStateChanged(SELECTION_CHANGED)
    R->>S: getSelected()
    S-->>R: combinacion 86
    Note over R: muestra creditos, estado y razon
    Note over H,R: Ninguna vista llama a la otra
```

## 4. Secuencia: cambiar el criterio de orden (`secuencia_criterio.png`)

```mermaid
sequenceDiagram
    actor U as Usuario
    participant C as Selector de criterio (JComboBox)
    participant S as AppState
    participant K as SortCriterion elegido
    participant T as Tabla (CombinationTableModel)

    U->>C: elige "Menos huecos"
    C->>S: setCriterion(FewestGapsCriterion)
    alt es el mismo criterio que ya estaba
        Note over S: no hace nada ni avisa
    else criterio distinto
        S->>S: refreshVisible()
        S->>K: comparator()
        K-->>S: metrica principal y luego id
        Note over S: filtra y ordena la lista visible
        S->>T: onStateChanged(CRITERION_CHANGED)
        T->>S: getVisibleCombinations()
        S-->>T: lista en el nuevo orden
        Note over T: redibuja las filas
    end
```

## 5. Algoritmos de orden

**Metricas** (las calcula `ScheduleMetrics` una sola vez al crear cada combinacion; el orden
solo las lee):

| Criterio | Clase | Ordena por (de menor a mayor) |
|---|---|---|
| Menos dias en el TEC | `FewestDaysCriterion` | cantidad de dias distintos con clases |
| Menos huecos | `FewestGapsCriterion` | minutos libres entre lecciones seguidas del mismo dia (no cuentan huecos de 10 minutos o menos) |
| Hora promedio de inicio | `EarliestAverageStartCriterion` | promedio de la hora de inicio de la primera leccion de cada dia |
| Jornada mas larga mas corta (criterio del grupo) | `ShortestLongestDayCriterion` | duracion del dia mas largo, de la primera a la ultima leccion |

**Comparador de cada criterio:** compara primero por su metrica; si dos combinaciones empatan,
compara por id (menor primero). El desempate esta en `MetricCriterion.comparator()`, que es
`final`, asi que ninguna subclase puede omitirlo. Resultado: el orden es total y siempre el mismo
para los mismos datos.

**Lista visible (`AppState.refreshVisible`)**, que se ejecuta al cargar datos, al cambiar el
criterio y al cambiar el filtro:

1. Recorrer todas las combinaciones cargadas y quedarse con las que acepta el filtro actual
   (Todas, Validas o Rechazadas). Costo O(n).
2. Ordenar esa lista con el comparador del criterio actual (`List.sort`, algoritmo TimSort,
   estable). Costo O(n log n).
3. Guardarla como lista inmutable; las vistas la leen con `getVisibleCombinations()`.

**Notificacion:** despues de cada cambio, `AppState` recorre una copia de la lista de
observadores y llama `onStateChanged(evento)` en cada uno. Si el cambio no altera nada (mismo
criterio, misma seleccion) no se avisa. Si un filtro nuevo esconde la combinacion seleccionada,
se quita la seleccion y se avisa tambien `SELECTION_CHANGED`.

**Ejemplo con los datos reales** (`outputPrologMixto.json`, 347 combinaciones):

| Criterio | Primera combinacion | Por que gana |
|---|---|---|
| Menos dias | 88 | 2 dias (pero 460 min de huecos) |
| Menos huecos | 80 | 0 min de huecos (pero 4 dias) |
| Hora promedio de inicio | 88 | empieza en promedio a las 07:30 |
| Jornada mas larga mas corta | 86 | su dia mas largo dura 230 min |

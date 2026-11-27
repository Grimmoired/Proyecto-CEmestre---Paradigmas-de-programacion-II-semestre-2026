# Etapa 4 (Java): diagramas, patrones y validacion del diseno

Material para el documento de Diseno (DI). Cada PNG se genera con el codigo Mermaid que aparece
bajo su titulo; para cambiar un diagrama se edita ese bloque y se vuelve a generar
(por ejemplo en https://mermaid.live). Reemplaza a `diagramas_cuartil3.md`.

| Archivo | Tipo | Que muestra |
|---|---|---|
| `arquitectura.png` | Diagrama de bloques | Las 4 etapas, los archivos que se pasan y el script `run_all` |
| `paquetes_java.png` | Dependencias entre paquetes | Quien puede usar a quien dentro del proyecto Java |
| `strategy.png` | Clases (patron Strategy) | Criterios de orden intercambiables |
| `observer.png` | Clases (patron Observer) | `AppState` y las vistas que lo observan |
| `vista.png` | Clases | Las clases de Swing: ventana, paneles y modelo de tabla |
| `secuencia_seleccion.png` | Secuencia | Clic en una fila: las vistas se sincronizan |
| `secuencia_criterio.png` | Secuencia | Cambio de criterio de orden |
| `secuencia_carga.png` | Secuencia | Carga de un archivo en segundo plano (`SwingWorker`) |

## Patrones de diseno y donde se aplican

| Patron | Donde | Para que |
|---|---|---|
| Adapter | `cemestre.io.PrologJsonAdapter` (interfaz `CombinationSource`) | Convierte el JSON de Prolog en objetos del dominio; la interfaz grafica solo conoce la interfaz |
| Strategy | `cemestre.sorting` (`SortCriterion` y 4 criterios) | Cambiar el criterio de orden en ejecucion, sin `if/else` sobre nombres |
| Factory | `cemestre.model.reason.RejectionReasonFactory` | Crear la subclase de razon segun el campo `rule` |
| Builder (opcional) | `cemestre.model.CombinationBuilder` | Armar combinaciones validas o rechazadas |
| Observer (extra grupo de 4) | `cemestre.state.AppState` + `cemestre.observer` + vistas | Que las vistas se actualicen solas sin que el estado las conozca |

No hay Singleton: `AppState` se crea en `Main` y se pasa por constructor. Lo verifican
`ModelDependenciesTest` y `LayerDependenciesTest` (ningun `getInstance`).

## 1. Arquitectura de las 4 etapas (`arquitectura.png`)

```mermaid
flowchart LR
    subgraph E1["Etapa 1: C"]
        P1["PlanEstudioCE.txt / PlanEstudioFI.txt<br/>HistorialEstudiante*.txt"] --> C["Cemestre.exe<br/>choques, requisitos, ciclos"]
    end
    subgraph E2["Etapa 2: Racket"]
        PR["Preferencias*.rktd"] --> R["main.rkt<br/>combinaciones por streams<br/>y filtro de creditos"]
    end
    subgraph E3["Etapa 3: Prolog"]
        PO["policy.pl"] --> PL["main.pl<br/>reglas de politica, razon<br/>y curso a quitar"]
    end
    subgraph E4["Etapa 4: Java"]
        J["cemestre.jar<br/>interfaz grafica"]
    end
    C -- "catalogoCE.json<br/>catalogoFI.json" --> R
    R -- "combinaciones.json" --> PL
    PL -- "resultados.json" --> J
    RA(["run_all.bat / run_all.sh<br/>ejecuta las 4 etapas en orden"]) -.-> C
    RA -.-> R
    RA -.-> PL
    RA -.-> J
```

## 2. Paquetes del proyecto Java (`paquetes_java.png`)

Las flechas van de quien usa hacia quien es usado. Solo `view` (en naranja) usa Swing.
`model`, `sorting`, `observer` y `state` no importan Swing ni AWT; lo verifican
`ModelDependenciesTest` y `LayerDependenciesTest`.

```mermaid
flowchart TB
    app["cemestre.app<br/>Main, SortingDemo<br/>(arma las piezas)"]
    view["cemestre.view<br/>MainFrame, CombinationListPanel,<br/>CombinationTableModel, ScheduleView,<br/>CreditSummaryView"]
    state["cemestre.state<br/>AppState, CombinationFilter"]
    observer["cemestre.observer<br/>StateObserver, StateEvent"]
    sorting["cemestre.sorting<br/>SortCriterion y criterios"]
    io["cemestre.io<br/>PrologJsonAdapter, JsonParser,<br/>CombinationSource"]
    model["cemestre.model<br/>Combination, CourseSelection,<br/>WeeklySchedule, ScheduleMetrics,<br/>model.reason"]

    app --> view
    app --> state
    app --> sorting
    app --> io
    view --> state
    view --> observer
    view --> sorting
    view --> model
    view -- "solo la interfaz<br/>CombinationSource" --> io
    state --> observer
    state --> sorting
    state --> model
    sorting --> model
    io --> model

    classDef swing fill:#fde2c8,stroke:#c77b30
    class view swing
```

## 3. Patron Strategy (`strategy.png`)

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

## 4. Patron Observer (`observer.png`)

- `AppState` es el sujeto: guarda una lista de `StateObserver` y les avisa cada cambio.
- El evento solo dice que cambio; cada vista le pide a `AppState` lo que necesita (modelo *pull*).
- Cada vista reacciona solo a los eventos que le importan: la lista a todos, el horario y el
  resumen solo a `SELECTION_CHANGED` y `DATA_LOADED`.
- Ninguna vista menciona a otra; lo verifica `ViewIndependenceTest`. Solo `MainFrame` las crea.

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
        +addObserver(o : StateObserver) void
        +removeObserver(o : StateObserver) void
        +load(data : CombinationSet) void
        +setCriterion(c : SortCriterion) void
        +setFilter(f : CombinationFilter) void
        +select(c : Combination) void
        +getVisibleCombinations() List~Combination~
        +getSelected() Optional~Combination~
        -notifyObservers(event : StateEvent) void
    }
    class CombinationListPanel {
        -syncing : boolean
        +onStateChanged(event : StateEvent) void
        -showSelection() void
    }
    class ScheduleView {
        +onStateChanged(event : StateEvent) void
        #paintComponent(g : Graphics) void
    }
    class CreditSummaryView {
        +onStateChanged(event : StateEvent) void
        -refresh() void
    }
    class MainFrame {
        -statusBar : JLabel
        -updateStatusBar() void
    }

    AppState o--> "0..*" StateObserver : avisa a
    AppState ..> StateEvent : envia
    StateObserver ..> StateEvent : recibe
    StateObserver <|.. CombinationListPanel
    StateObserver <|.. ScheduleView
    StateObserver <|.. CreditSummaryView
    CombinationListPanel --> AppState : consulta y pide cambios
    ScheduleView --> AppState : consulta
    CreditSummaryView --> AppState : consulta
    MainFrame ..> StateObserver : barra de estado (lambda)
    note for AppState "No importa Swing ni conoce las vistas. Lo verifica LayerDependenciesTest."
    note for StateObserver "Las vistas no se conocen entre si. Lo verifica ViewIndependenceTest."
```

## 5. Clases de la interfaz (`vista.png`)

```mermaid
classDiagram
    direction TB
    class JFrame { <<Swing>> }
    class JPanel { <<Swing>> }
    class AbstractTableModel { <<Swing>> }
    class MainFrame {
        -state : AppState
        -source : CombinationSource
        -openItem : JMenuItem
        -statusBar : JLabel
        +MainFrame(state, criteria, source)
        +openFile(file : Path) void
        -chooseFile() void
        -setLoading(loading : boolean, file : Path) void
        -showWarnings(warnings : List~String~) void
    }
    class CombinationListPanel {
        -state : AppState
        -table : JTable
        +CombinationListPanel(state, criteria)
    }
    class CombinationTableModel {
        -state : AppState
        +getValueAt(row : int, column : int) Object
        +combinationAt(row : int) Combination
        +rowOf(c : Combination) int
    }
    class ScheduleView {
        -state : AppState
        #paintComponent(g : Graphics) void
        +getToolTipText(e : MouseEvent) String
    }
    class CreditSummaryView {
        -state : AppState
        -content : JEditorPane
    }
    class CombinationSource {
        <<interface>>
        +load(file : Path) CombinationSet
    }
    class Main {
        +main(args : String[])$ void
    }

    JFrame <|-- MainFrame
    JPanel <|-- CombinationListPanel
    JPanel <|-- ScheduleView
    JPanel <|-- CreditSummaryView
    AbstractTableModel <|-- CombinationTableModel
    MainFrame *-- CombinationListPanel
    MainFrame *-- ScheduleView
    MainFrame *-- CreditSummaryView
    CombinationListPanel *-- CombinationTableModel
    MainFrame --> CombinationSource : carga con SwingWorker
    Main ..> MainFrame : crea
    Main ..> AppState : crea y pasa por constructor
```

## 6. Secuencia: seleccionar una combinacion (`secuencia_seleccion.png`)

```mermaid
sequenceDiagram
    actor U as Usuario
    participant L as CombinationListPanel
    participant S as AppState
    participant H as ScheduleView
    participant R as CreditSummaryView
    participant B as Barra de estado

    U->>L: clic en la fila de la combinacion 86
    L->>S: select(combinacion 86)
    Note over S: guarda la seleccion
    S->>L: onStateChanged(SELECTION_CHANGED)
    Note over L: marca la fila (syncing = true)
    S->>H: onStateChanged(SELECTION_CHANGED)
    H->>H: repaint()
    H->>S: getSelected() dentro de paintComponent
    S-->>H: combinacion 86
    S->>R: onStateChanged(SELECTION_CHANGED)
    R->>S: getSelected()
    S-->>R: combinacion 86
    Note over R: muestra creditos, estado, razon y sugerencia
    S->>B: onStateChanged(SELECTION_CHANGED)
    Note over L,B: Ninguna vista llama a otra
```

## 7. Secuencia: cambiar el criterio (`secuencia_criterio.png`)

```mermaid
sequenceDiagram
    actor U as Usuario
    participant L as CombinationListPanel
    participant S as AppState
    participant K as SortCriterion elegido
    participant H as ScheduleView y CreditSummaryView

    U->>L: elige "Menos huecos" en el selector
    L->>S: setCriterion(FewestGapsCriterion)
    alt es el mismo criterio que ya estaba
        Note over S: no hace nada ni avisa
    else criterio distinto
        S->>S: refreshVisible()
        S->>K: comparator()
        K-->>S: metrica principal y luego id
        Note over S: filtra y ordena la lista visible
        S->>L: onStateChanged(CRITERION_CHANGED)
        L->>S: getVisibleCombinations()
        Note over L: redibuja las filas y vuelve a marcar la seleccion
        S->>H: onStateChanged(CRITERION_CHANGED)
        Note over H: lo ignoran: no cambio la seleccion
    end
```

## 8. Secuencia: abrir un archivo (`secuencia_carga.png`)

```mermaid
sequenceDiagram
    actor U as Usuario
    participant M as MainFrame (hilo de Swing)
    participant W as SwingWorker (hilo de fondo)
    participant A as PrologJsonAdapter
    participant S as AppState

    U->>M: menu Archivo, Abrir, o argumento de java -jar
    M->>M: setLoading(true), desactiva Abrir y pone cursor de espera
    M->>W: execute()
    W->>A: load(archivo)
    alt archivo valido
        A-->>W: CombinationSet
        W->>M: done()
        M->>S: load(datos)
        S-->>M: avisa DATA_LOADED a todas las vistas
        M->>M: muestra advertencias si las hay
    else archivo danado
        A-->>W: DataLoadException
        W->>M: done()
        M->>U: dialogo de error, los datos anteriores se conservan
    end
    M->>M: setLoading(false)
```

## Algoritmos de orden

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

## Dibujo del horario (`ScheduleView`)

1. Columnas: lunes a viernes, mas sabado solo si la combinacion tiene clases ese dia.
2. Filas: desde la primera hora completa antes del primer bloque hasta la primera hora completa
   despues del ultimo (por ejemplo 07:30-20:50 se dibuja de 07:00 a 21:00).
3. Cada bloque: `y = arriba + (minutos - inicio) * alto / (fin - inicio)`, asi el alto es
   proporcional a la duracion. Se recalcula en cada dibujo, por eso se adapta al tamano de la ventana.
4. Texto del bloque: codigo, nombre, grupo y horas; lo que no cabe se recorta con "..." y el
   tooltip muestra todo.
5. Mismo color para todos los bloques de un curso. Java no detecta choques: confia en Racket.

## Validacion del diseno (lista de verificacion de la seccion 1.2 del plan)

| Requisito | Como se cumple | Evidencia |
|---|---|---|
| Cargar y unificar los datos en un modelo de objetos | `PrologJsonAdapter` convierte el JSON en `Combination`, `CourseSelection`, etc.; la interfaz y los criterios solo ven el dominio | `PrologJsonAdapterTest` |
| Mostrar TODAS las combinaciones, validas y rechazadas con su razon | Filtro "Todas" por defecto; columna y resumen con la razon de Prolog | `AppStateTest` (347 entran, 347 visibles) |
| No validar reglas ni recalcular choques | `complies` y `reason` se toman de Prolog; las metricas solo ordenan | Revision de codigo: ningun calculo de reglas en Java |
| Al menos 2 criterios de orden intercambiables en ejecucion | 4 criterios en el selector, sin reiniciar | `SortCriterionTest`, `AppStateTest` |
| Metricas definidas y empates por id | `ScheduleMetrics`; desempate en `MetricCriterion.comparator()` (`final`) | `SortCriterionTest.tiesAreBrokenById` |
| Horario semanal con codigo, nombre y grupo en su bloque | `ScheduleView` | Capturas de la interfaz |
| Jerarquia con 2 o mas subclases y polimorfismo | `Combination` (2), `RejectionReason` (3), `SortCriterion`/`MetricCriterion` (4); las vistas usan el tipo base sin `instanceof` | Revision de codigo |
| Al menos 2 patrones, sin Singleton | Strategy y Adapter (+ Factory y Builder) | `LayerDependenciesTest`, `ModelDependenciesTest` |
| Observer con dos vistas sincronizadas sin que el modelo las conozca | Horario y resumen de creditos (y la lista) observan `AppState` | `AppStateTest.everyObserverReceivesTheSameEvents`, `ViewIndependenceTest` |
| Codigo por paquetes y ejecutable con `java -jar` | Paquetes `cemestre.*`; `target/cemestre.jar` con `Main-Class` | `mvn clean package` y `java -jar` |
| Abrir cualquier salida de Prolog sin recompilar | Argumento de `java -jar` y menu Archivo > Abrir, con carga en segundo plano | Prueba manual con varios archivos |
| Recorrido de las 4 etapas con parametros cambiados | `run_all.bat` / `run_all.sh` con preferencias, politica e historiales opcionales | Corrida con `policy.pl`, `policyAreaN1.pl` y `policyCambiada.pl` |

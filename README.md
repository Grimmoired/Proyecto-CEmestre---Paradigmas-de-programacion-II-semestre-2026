# Proyecto CEmestre - Etapa 2 (Paradigma Funcional) 

Segunda etapa del proyecto CEmestre para el grupo 2 del curso de
Paradigmas de Programación (CE1106), segundo semestre 2026.

Elaborado por los estudiantes:
- Bryan David Abarca Quiros
- Marcelo Matey Rodriguez
- Josue Adrian Perez Perez
- Jorge Pablo Porras Alvarado

**Aviso para el profesor: el readme se actualizo para incluir los detalles de la etapa 2 del proyecto, si quiere acceder al README de la etapa 1 para calificar la rubrica de documentacion de esta etapa, el readme viejo se guardo en la raiz del repositorio.**

Esta etapa se desarrolló exclusivamente en el lenguaje Racket, siguiendo el paradigma funcional, y su
función es leer el catálogo de cursos que produjo la etapa 1 (en C) junto con un archivo de preferencias
del estudiante, generar todas las combinaciones de horario posibles sin choques entre los grupos elegidos,
filtrarlas por el rango de créditos que pidió el estudiante, y exportarlas a un archivo JSON que va a ser
el input de la siguiente etapa del proyecto, la cual será implementada en Prolog.

Debido a que somos un grupo de 4 personas, además de los requisitos base de la etapa también se implementó
la optimización de la generación: las combinaciones inválidas se descartan apenas se detecta el primer
choque (poda temprana), en vez de generar la combinación completa y descartarla después.

Esta etapa **no** arma un horario final, no optimiza, no aplica preferencias más allá del rango de créditos
y no presenta resultados en una interfaz; el resultado es únicamente un archivo.

## Cómo correr el proyecto

### Requisitos
- Racket 8.x (el proyecto se desarollo con Racket 8.10 CS). No se necesitan paquetes externos, los unicos paquetes usados son built-in de racket:
  `racket/base`, `racket/contract`, `racket/list`, `racket/stream`, `racket/runtime-path` y `json`.

### Ejecución

Desde la carpeta del proyecto:

```bash
racket main.rkt [preferencias.rktd] [-c catalogo.json]... [-o salida.json]
```

- Sin argumentos usa las rutas por defecto definidas al inicio de `main.rkt` (catálogos de ambas carreras,
  `PreferenciasPrueba.rktd` y salida en `CombinacionesSalida.json`).
- `-c` se puede repetir, una vez por cada catálogo de carrera producido por la etapa 1.
- `-o` cambia el archivo de salida.

Al terminar imprime la cantidad de combinaciones exportadas. Si ninguna combinación cumple las
restricciones, el archivo queda con la lista vacía y se imprime una advertencia. Todas las advertencias
(cursos o grupos inexistentes, cursos no habilitados, etc.) salen por la salida de error.

El repositorio incluye scripts con pruebas unitarias para probar de manera aislada los 3 scripts mas complejos de la etapa, puede correrlos usando los siguientes comandos.

```bash
racket PruebaGenerador.rkt
racket PruebaGeneradorAvanzada.rkt
racket PruebaCreditFilter.rkt
```

`PruebaGenerador.rkt` y `PruebaGeneradorAvanzada.rkt` prueban el generador de combinaciones (choques,
poda, laziness, comparación contra un algoritmo de fuerza bruta independiente y casos límite), y
`PruebaCreditFilter.rkt` prueba `filter-stream`, el cálculo de créditos y el filtrado por rango.

---

## Organización del trabajo

Al igual que en la etapa 1, la distribucion de trabajo se realizo de forma lineal o en cascada, el trabajo se dividió en 4 cuartiles y cada persona
le entregó su parte ya funcionando a la siguiente. La distribución fue la siguiente:

1. Carga de datos y tipado (`Types.rkt`, `CatalogReader.rkt`, `PreferencesReader.rkt`, `DataLoader.rkt`):
   Bryan David Abarca Quiros

2. Generación de combinaciones mediante streams y poda temprana (`CombinationGenerator.rkt`):
   Jorge Pablo Porras Alvarado

3. Filtrado por créditos y función con polimorfismo paramétrico (`CreditFilter.rkt`):
   Marcelo Matey Rodriguez

4. Exportación, integración y documentación (`OutputWriter.rkt`, `main.rkt`):
   Josue Adrian Perez Perez

## Arquitectura del proyecto

El programa es un único ejecutable que sigue un flujo de transformación de datos:

```
leer catálogos -> leer y validar preferencias -> generar combinaciones (stream)
-> filtrar por créditos (stream) -> exportar a JSON (escritura incremental)
```

Los datos pasan de una función a la siguiente sin modificarse (inmutabilidad), y las combinaciones nunca
se juntan en una lista completa: cada una viaja por el stream desde que se genera hasta que se escribe en el archivo.

| Archivo | Función |
|---|---|
| `Types.rkt` | Los `structs` con contrato que representan bloques de horario, grupos, cursos, preferencias, selecciones y combinaciones |
| `CatalogReader.rkt` | Lee el JSON de la etapa 1 de una o más carreras y lo convierte a `course` tipados, agregando la carrera a cada curso |
| `PreferencesReader.rkt` | Lee el archivo de preferencias y lo valida contra el catálogo |
| `DataLoader.rkt` | Junta los dos lectores: devuelve el catálogo y las preferencias ya validadas |
| `CombinationGenerator.rkt` | Genera, mediante streams y con poda temprana, las combinaciones sin choques |
| `CreditFilter.rkt` | Calcula los créditos de una combinación y filtra por rango; contiene `filter-stream`, la función polimórfica |
| `OutputWriter.rkt` | Escribe el stream de combinaciones al JSON de salida sin convertirlo antes en una lista |
| `main.rkt` | Lee los argumentos, orquesta todo el pipeline y maneja los errores |

Cada módulo expone solo lo necesario mediante `provide`, y depende únicamente de `Types.rkt` y de la salida del módulo anterior, por lo que se puede modificar la implementación de uno sin afectar a los demás.

### Archivos de entrada

**Catálogo (salida de la etapa 1).** Es el JSON que produce el módulo de C, con un campo adicional a nivel
de catálogo, `carrera`, que indica a cuál de las dos licenciaturas pertenecen los cursos; no se modificó la
lógica de choques ni de requisitos de la etapa 1. Ambos catálogos se extendieron a los primeros 6 semestres
de cada carrera, a continuacion un ejemplo del formato que usan:

```json
{
  "carrera": "Ingenieria en Computadores",
  "courses": [
    {
      "courseCode": "CE1101",
      "courseName": "Introduccion a la Programacion",
      "credits": 3,
      "requisites": [],
      "corequisites": [],
      "groups": [
        {"groupId": "1", "hasClash": true,
         "blocks": [{"day": "JUE", "startTime": "0730", "endTime": "0920"},
                    {"day": "MAR", "startTime": "0730", "endTime": "0920"}]}
      ],
      "hasScheduleClash": true,
      "canEnroll": true,
      "hasCycle": false
    }
  ]
}
```

**Preferencias del estudiante.** Es un archivo de texto con una lista asociativa escrita en la sintaxis
de Racket, con los créditos mínimos y máximos deseados y los cursos de interés; cada curso de interés va
seguido de **los grupos** que el estudiante estaría dispuesto a matricular (el enunciado pide más de un grupo):

```racket
((credits-min . 10)
 (credits-max . 14)
 (interests . (("CE1101" "1" "2")
               ("CE1104" "1")
               ("MA1403" "1" "2"))))
```

Se eligió esta sintaxis porque Racket la lee de forma nativa con `read`, sin necesitar un parser ni
una librería externa, así como en la etapa 1 se eligió un formato por prefijos que se podía leer con `fgets`.

## Decisiones de diseño

### Justificación de decisiones propias de nuestra implementación

**Los choques se recalculan con los bloques de horario crudos y no se usa `hasClash`.**
El campo `hasClash` que calculó la etapa 1 se obtuvo comparando cada grupo contra todo el catálogo, no
contra los cursos que le interesan al estudiante, de hecho, en los catálogos de prueba todos los grupos
tienen `hasClash: true`, por lo que ese booleano no sirve para decidir nada en esta etapa, por ejemplo,
`CE1101` grupo 2 tiene `hasClash: true`, pero sí se puede combinar con `CE1104` grupo 1, por eso el
generador compara los bloques (`day`, `startTime`, `endTime`) únicamente entre los grupos candidatos de los cursos
de interés; esta comparación se hace entre todos los cursos juntos, sin importar de cuál carrera sean, así que un curso de
Computadores puede chocar con uno de Física.

**Criterio de choque entre bloques.** Dos bloques chocan si son del mismo día y sus intervalos se traslapan
(`inicioA < finB` y `inicioB < finA`), si un bloque termina justo cuando empieza el otro (por ejemplo,
`0730-0920` y `0920-1110`) **no** se cuenta como un choque, las horas se convierten a número antes de compararlas, así que la comparación no depende de que las horas vengan con el cero inicial.

**Generación con streams y poda temprana (backtracking).** El generador no calcula el producto cartesiano
de todos los grupos para después descartar los que chocan: va eligiendo un grupo por curso, en el orden
de los intereses, y apenas el grupo que está probando choca con alguno de los ya elegidos descarta ese
candidato junto con toda la rama que saldría de él, mientras que el estado de la búsqueda (los grupos ya elegidos)
viaja como parámetro de las funciones recursivas `search` y `try-candidates`, sin ninguna variable mutable;
las combinaciones se devuelven en un stream perezoso construido con `stream-cons`, y `stream-append-lazy`
concatena las ramas de forma que la siguiente rama no se explora hasta que alguien consume el stream hasta ahí,
esto lo pudimos confirmar mediante pruebas unitarias, el resultado es que de 6^10 combinaciones posibles solo se piden 3 y el resultado es inmediato,
y cuando dos cursos del inicio de la lista chocan entre sí el stream queda vacío sin recorrer las 6^10 ramas.

**Tipado explícito con contratos de Racket.** Se eligió `racket/contract` en vez de Typed Racket porque
los datos entran como `jsexpr` desde un archivo JSON y como datos leídos con `read`, y los contratos permiten validar estos datos en el
momento exacto en que se convierten en `structs`; todos los `structs` se declaran con `struct/contract` y las funciones con `define/contract`.
En las funciones recursivas internas del generador (`search`, `try-candidates`, `clashes-with-chosen?`) se usan contratos livianos (`list?`, `stream?`)
con el tipo exacto documentado en un comentario, porque un contrato como `(listof (listof candidate/c))` se revalida completo
en cada llamada recursiva; las funciones públicas sí llevan el contrato completo.

**Función con polimorfismo paramétrico: `filter-stream`.** Recibe un predicado y un stream de elementos de
cualquier tipo y devuelve el stream filtrado de forma perezosa, su firma es
`(All (A) (-> (-> A Boolean) (Streamof A) (Streamof A)))`; en si esta no está hecha solamente para créditos, pues fue desarollada con polimorfismo en mente y por tanto se puede usar con enteros, strings o combinaciones sin necesidad de cambiar una nada (`credits-in-range?` construye el predicado y `filter-by-credits` se lo pasa a `filter-stream`).

Hay una limitación que descubrimos, pues al usar contratos de Racket, el combinador formal de polimorfismo (`parametric->/c`) no funciona sobre streams,
porque sella los valores del tipo `A` y el predicado que pasa el llamador deja de reconocerlos; por eso el contrato verifica la forma
(`(-> any/c boolean?)`, `stream?`) y el parámetro de tipo `A` se escribe explicitamente en el comentario de la función.

**Los créditos se calculan a partir del catálogo.** `combination` solo guarda los pares curso-grupo, así
que los créditos se obtienen consultando una tabla inmutable código-créditos que se construye una sola vez por catálogo, no
buscando el curso en la lista cada vez; si una combinación incluye un código que no está en el catálogo, se lanza un error con el código del curso en vez de contarlo
como 0 créditos en silencio, porque sería un error de datos que no queremos ocultar.

**Qué se hace con los intereses que no se pueden usar.** Antes de generar, `validate-preferences` omite los cursos que no existen en el catálogo y los grupos que no existen en su curso, y el generador omite además los cursos con
`canEnroll = false` y los cursos sin grupos (por ejemplo `CI0205`, que tiene 0 créditos y no tiene horario). El resto del pipeline continúa con los cursos que sí quedaron.

**Los correquisitos no se imponen.** El enunciado pide un grupo por cada curso habilitado sin choques,
y la elegibilidad por requisitos y correquisitos ya la calculó la etapa 1 (`canEnroll`); lo que sí se hace es
avisar por la salida de error cuando un curso de interés tiene un correquisito que no está entre los demás cursos de interés (por ejemplo, pedir `CE1104` sin `CE1101`), en cualquier caso, la combinación se genera igual.

**Sin mutación.** No se usa `set!`, ni estructuras mutables, ni variables globales reasignadas, en su lugar el estado se maneja con recursión, de modo que
los grupos elegidos viajan por parámetro en el generador, el número de id de cada combinación es un parámetro del ciclo recursivo
de `OutputWriter.rkt`, y los argumentos de línea de comandos se leen con una función recursiva (`parse-args`) en vez de la macro `command-line`, que
se apoya en variables que se modifican.

### Un caso límite real que se encontro y como se resolvio

Al probar el generador contra un algoritmo de fuerza bruta independiente nos dimos cuenta de un caso que no habíamos
considerado, ¿que pasa si se lista el mismo curso dos veces en las preferencias? por ejemplo:

```racket
(interests . (("A" "1") ("A" "2")))
```

El generador trataba cada línea de interés como un curso distinto, así que producía una combinación con
`("A" "1")` **y** `("A" "2")` a la vez, es decir, dos grupos del mismo curso en un mismo horario, lo cual
nunca es válido (y además no choca, porque son grupos de horarios distintos, por lo que la poda no lo detectaba).

Lo resolvimos con la función `merge-interests`, que antes de armar los candidatos junta las entradas repetidas de un mismo curso
en una sola, uniendo sus grupos sin duplicados y conservando el orden de la primera aparición, y emite una advertencia.
Con la corrección, el ejemplo anterior produce una combinación con `("A" "1")` y otra con `("A" "2")`, nunca ambas.
Esto quedó cubierto por una prueba en `PruebaGeneradorAvanzada.rkt`.

Otro caso límite relacionado con el anterior es el de un curso sin ningún grupo (`CI0205`), que está habilitado pero no tiene horario, por lo que si se
intentara generar "un grupo por cada curso" fallaría, asi que en su lugar se omite con una advertencia y la generación continúa con los demás.

### Justificación del formato de salida

Se eligió **JSON** para el archivo de salida, por consistencia con la etapa 1 y porque cada combinación contiene listas anidadas de
longitud variable (principalmente las selecciones, y dentro de cada selección tambien estan los bloques de horario); en CSV habría que inventar un separador dentro de cada
celda para simular esa anidación, además, Prolog puede leer JSON directamente con su librería estándar `http/json`.

Sin embargo el principal motivo de la eleccion fue a nivel de eficiencia de ejecucion, ya que con este formato es posible hacer que las combinaciones se escriban una por una a medida que el stream las
produce, sin convertir antes el stream en una lista (el enunciado del proyecto pide explícitamente no armar listas completas al final); esta decision produjo dos principales cambios en el formato que se tenia planeado hasta ese momento, primero que nada, la cantidad de combinaciones (`combinationCount`) se localiza **al final** del archivo, porque solo se conoce cuando el stream se agotó, si fuera el primer campo habría que recorrer todo el stream antes de escribir, perdiendo la ventaja de la escritura incremental; segundo, el archivo se escribe primero a un archivo temporal y se renombra a su nombre final solo cuando termina sin errores, así que si algo falla a mitad del stream no queda un JSON truncado.

Estructura del archivo (los campos de cada objeto salen en orden alfabético, ya que así los escribe `write-json`):

```json
{
  "creditsMin": 10,
  "creditsMax": 14,
  "combinations": [
    {
      "id": 1,
      "totalCredits": 10,
      "selections": [
        {
          "courseCode": "CE1101",
          "courseName": "Introduccion a la Programacion",
          "groupId": "2",
          "career": "Ingenieria en Computadores",
          "credits": 3,
          "blocks": [{"day": "JUE", "startTime": "0930", "endTime": "1120"},
                     {"day": "MAR", "startTime": "0930", "endTime": "1120"}]
        }
      ]
    }
  ],
  "combinationCount": 2
}
```

El enunciado exige como mínimo la lista de pares curso-grupo (`courseCode` y `groupId`), los créditos totales (`totalCredits`) y la carrera
de origen de cada curso (`career`), sin embnargo decidimos incluir tambien los siguientes campos al formato:

| Campo | Justificación |
|---|---|
| `id` | Identifica cada combinación para que las etapas siguientes puedan referirse a ella (por ejemplo, para mostrar un resultado al usuario) |
| `courseName`, `credits` (por selección) | Evitan que Prolog tenga que volver a leer el catálogo para saber el nombre y los créditos de cada curso |
| `blocks` (por selección) | La etapa de Prolog aplica reglas de compatibilidad sobre las combinaciones, y para eso necesita el horario de cada grupo elegido |
| `creditsMin`, `creditsMax` | Dejan registrado con cuál rango se filtró, para que Prolog aplique sus reglas sobre el mismo rango |
| `combinationCount` | Permite saber cuántas combinaciones hay sin contarlas; va al final por lo explicado arriba |

## Estructuras de datos desarrolladas

Todos los `structs` están definidos en `Types.rkt` con `struct/contract` y son transparentes e inmutables (en Racket
los `structs` son inmutables por defecto, y no se declaró ningún campo como mutable):

```racket
(struct/contract schedule-block
  ([day string?] [start-time string?] [end-time string?]))

(struct/contract course-group
  ([group-id string?] [has-clash boolean?] [blocks (listof schedule-block?)]))

(struct/contract course
  ([course-code string?] [course-name string?] [credits exact-nonnegative-integer?]
   [requisites (listof string?)] [corequisites (listof string?)]
   [groups (listof course-group?)]
   [has-schedule-clash boolean?] [can-enroll boolean?] [has-cycle boolean?]
   [career string?]))

(struct/contract preference
  ([credits-min exact-nonnegative-integer?]
   [credits-max exact-nonnegative-integer?]
   [interests (listof (cons/c string? (listof string?)))]))

(struct/contract selection
  ([course-code string?] [group-id string?]))

(struct/contract combination
  ([selections (listof selection?)]))
```

- `schedule-block`, `course-group` y `course` reflejan directamente el JSON de la etapa 1; `course` tiene un campo más, `career`, que **no** viene dentro de cada curso en el archivo, sino del campo `carrera` del catálogo, y `CatalogReader.rkt` lo copia a cada curso al cargarlo.
- `preference` guarda los intereses como una lista de pares `(codigo . (grupo1 grupo2 ...))`, que es la forma exacta en que se escriben en el archivo de preferencias.
- `combination` solo guarda los pares curso-grupo (`selection`), sin créditos ni carrera; esos datos se derivan del catálogo cuando se necesitan (`CreditFilter.rkt` para los créditos, `OutputWriter.rkt` para la exportación), para no guardar información duplicada que podría ser inconsistnte.

Estructuras internas del generador (`CombinationGenerator.rkt`):

- **Candidato** (`candidate/c`): un par `(codigo-de-curso . course-group)`; se usa para poder comparar los horarios de un grupo sin buscar otra vez el curso al que pertenece.
- **Lista de candidatos por curso**: por cada curso de interés se arma una lista con los candidatos que el estudiante pidió; la entrada del algoritmo de búsqueda es la lista de estas listas.
- **Lista de elegidos** (`chosen`): los candidatos elegidos hasta ese momento, con el último elegido al frente; se pasa como parámetro y se invierte al final (`chosen->combination`) para que las selecciones queden en el mismo orden que los intereses.
- **Stream de `combination`**: es el resultado de `generate-combinations`, cada elemento se calcula únicamente cuando alguien lo pide.

Estructuras auxiliares inmutables:

- En `CreditFilter.rkt`, una tabla hash inmutable `código -> créditos` construida una sola vez por catálogo.
- En `OutputWriter.rkt`, un índice hash inmutable `código -> curso` para encontrar cada curso sin recorrer la lista del catálogo.

## Limitaciones detectadas hasta el momento

- Si el mismo código de curso apareciera en los catálogos de las dos carreras, el índice de cursos y las búsquedas por código usarían solo uno de ellos, con los datos actuales esto no pasa.
- La velocidad de exportación está limitada por `write-json` de la librería estándar: con una cantidad muy grande de combinaciones (del orden de cientos de miles) escribir el archivo tarda varios segundos. Con los catálogos reales y un número razonable de cursos de interés, la cantidad de combinaciones es mucho menor.
- Los correquisitos no se imponen en la generación (solo se avisa), tal como se explicó arriba.
- Los campos de cada objeto del JSON de salida salen en orden alfabético y no en el orden en que se muestran en un esquema conceptual, esto no afecta a quien lea el archivo, ya que los campos se acceden por nombre.

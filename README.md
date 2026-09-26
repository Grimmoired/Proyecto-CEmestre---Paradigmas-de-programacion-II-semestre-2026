# Proyecto CEmestre

Primera etapa del proyecto CEmestre para el grupo 2 del curso de
Paradigmas de Programación (CE1106), segundo semestre 2026.

Elaborado por los estudiantes:
- Bryan David Abarca Quiros
- Marcelo Matey Rodriguez
- Josue Adrian Perez Perez
- Jorge Pablo Porras Alvarado

Esta etapa se desarrollo exclusivamente en el lenguaje C usando el IDE CLion, siguiendo
el paradigma imperativo, y su funcion es construir el catálogo de cursos de dos
carreras, calcular los choques de horario entre grupos, y determinar qué cursos puede
matricular un estudiante según su historial; el resultado se exporta a un archivo JSON
que va a ser el input de la siguiente etapa del proyecto, la cual sera implementada en Racket.

Debido a que somos un grupo de 4 personas, además de los requisitos base de la etapa tambien se
implementó la detección de ciclos en el grafo de requisitos tal y como lo pedia el enunciado del proyecto.

Las 2 lisenciaturas escogidas para el proyecto fueron Ingeniería en Computadores (obligatoria para todos los grupos), e Ingeniería Física.

Se tomaron en cuenta los cursos de los primeros 4 semestres de cada plan, más el
semestre 0, el cual incluye cursos de nivelacion o examenes de convalidacion, ya que algunos cursos del semestre 1
tienen como requisito un curso de ese semestre 0, por lo que si no lo incluíamos, el grafo de
requisitos hubiese quedado con referencias a cursos que no existen en ningún lado del
catálogo, lo cual hubiese sido un problema para la validación de requisitos y
detección de ciclos.

Para realizar las pruebas referentes a historiales de estudiantes se crearon 2 perfiles de estudiantes inventados, uno perteneciente a la licensiatura de CE y otra perteneciente a IF.

Los datos de horarios, créditos, requisitos y correquisitos se recolectaron de
la Guía de Horarios institucional del TEC, tomando solamente la sede de Cartago, ya que asi nos lo especifico el profesor.

## Cómo compilar y correr el proyecto

El proyecto se desarrolló en CLion, con CMake y el estándar C11; debido a que esta solo es la primera parte de un proyecto mas grande, el main imprime una serie de comprobaciones para demostrar que los archivos se leen correctamente y las implementaciones de deteccion de choques de horario y corequesitos se efectuaron correctamente, al final deja como output los archivos JSON que se usaran en la siguiente etapa.

### Requisitos

### Linux
- GCC 7.0 o superior
- CMake 3.5 o superior
- Make

### Windows
- MinGW-w64 con GCC
- CMake 3.10 o superior
- Los tres deben estar agregados al PATH del sistema

### Compilación

Abra una terminal en la raíz del proyecto y ejecute:
```bash
mkdir build
cd build
cmake .. -G "MinGW Makefiles"
mingw32-make
```

Esto generará el ejecutable "CEmestre.exe" en la carpeta build, use el siguiente comando para ejecutar el binario y crear los archivos de Output, asi como imprimir en consola todos los resultados de lectura y comprobaciones de catalogos e historiales.
```cmd
.\Cemestre.exe 2>&1
```
---

## Organización del trabajo

Como grupo, decidimos trabajar de forma "lineal" en vez de en paralelo ya que en 
semestres anteriores esto nos ha dado buenos resultados al trabajar juntos, asi que 
procedimos a desglosar los requerimientos del proyectoen 4 cuartiles con una carga de trabajo mas o menos equitativa,
cada persona se encargó de un cuartil del trabajo total y le entregó su parte ya funcionando a la persona encargada del siguiente cuartil, la distribucion fue la siguiente:

1. Carga del catálogo y del historial del estudiante (`catalogLoader`, `studentLoader`):
Bryan David Abarca Quiros

2. Detección de choques de horario (`scheduleClash`):
Jorge Pablo Porras Alvarado

3. Validación de requisitos/correquisitos y detección de ciclos (`Requisites`):
  Marcelo Matey Rodriguez

4. Exportación del resultado final a un JSON de facil acceso para la etapa de Racket (`outputWriter`):
   Josue Adrian Perez Perez

## Arquitectura del proyecto

El programa sigue un flujo simple, todo se hace mediante un unico ejecutable que sigue la siguiente estructura:

```
cargar catalogo -> cargar historial -> detectar choques -> calcular elegibilidad -> detectar ciclos -> exportar a JSON
```

Archivos principales:

| Archivo | Funcion |
|---|---|
| `constants.h` | Todas las constantes y límites del programa (tamaños máximos, rutas de archivo) |
| `types.h` | Los `structs` que representan el catálogo, un curso, un grupo y el historial |
| `catalogLoader.c/h` | Lee el archivo de catálogo y llena el `Catalog` |
| `studentLoader.c/h` | Lee el historial del estudiante y lo valida contra el catálogo |
| `scheduleClash.c/h` | Detecta los choques de horario entre grupos |
| `Requisites.c/h` | Calcula si el estudiante puede matricular cada curso y detecta ciclos |
| `outputWriter.c/h` | Exporta el catálogo completo a un archivo JSON |
| `main.c` | Junta todos los módulos anteriores y corre las pruebas |

Las constantes se guardan en un header aparte, tal como se indico en el enunciado del proyecto;
Cada sccript trabaja de manera independiente, haciendo uso de los structs declarados en `types.h`, así que se
puede modificar la mayoria de implementaciones del programa sin romper o afectar de manera significativa a las demas.

El formato de los archivos de entrada fue diseñado pensando en que fueran fáciles
de leer línea por línea con los `fgets` que ofrece C por defecto, sin necesitar una librería de parseo externa, para esto se hace uso de los prefijos que tiene cada linea de texto, como NOMBRE, CREDITOS, etc para detectar a que tipo de dato corresponde cada uno.

### Catálogo de cursos

Cada curso es un bloque de líneas con una etiqueta por campo, cerrado con `###` para diferenciarlos
Ejemplo:
```
CODIGO: CE1103
NOMBRE: Algoritmos y Estructuras de Datos I
CREDITOS: 4
REQUISITOS: CE1101,CE1104,MA1403
COREQUISITOS: NINGUNO
GRUPOS: 1(JUE-0930-1120,MAR-0930-1120);2(JUE-1500-1650,MAR-1500-1650)
###
```

Los requisitos y correquisitos se manejan mediante una lista de códigos correspondientes a los cursos requeridos, separados por coma; se usa `NINGUNO` en los casos donde no aplican requsitos ni corequisitos.

Los grupos siguen el formato `id(dia-horaInicio-horaFin,dia-horaInicio-horaFin)`, y se separan con `;`. Las horas van sin los dos puntos (`0930` en vez de `09:30`) para que sea más simple tokenizar con un solo caracter separador.

Un curso sin horario (por ejemplo, uno que se gestiona fuera de la Guía de Horarios) se escribe como `GRUPOS: NINGUNO`.

### Historial del estudiante

Igualmente, se siguio un formato que fuese facil de obtener con fget haciendo uso de prefijos
Ejemplo:
```
CARNET: 2024015678
NOMBRE: Ana Sofia Vargas Mora
GENERO: F
APROBADOS: CI0205,MA0101,CE1101,CE1104,MA1403
```

`GENERO` no lo pedía el enunciado, lo agregamos nosotros para que el programa pueda usar el
artículo correcto (`el`/`la estudiante`) en vez de usar alguno de los 2 por defecto siempre.

## Decisiones de diseño

### Actividades culturales y deportivas (`SE1100`, `SE1200`)

En el plan de Computadores, cursos como `SE1100` (Actividad Cultural I) no tienen un
horario propio: el estudiante elige una opción específica (por ejemplo, "Artes
Musicales" o "Ajedrez"), y cada opción tiene su propio horario; decidimos modelar esto
tratando cada opción específica como un "grupo" más del curso genérico, usando un id
compuesto (`codigoEspecifico_numeroDeGrupo`), por ejemplo `SE1105_6` para el sexto
grupo de la opción de Artes Musicales.

### Manejo de un código no reconocido en el historial

Si un código del archivo de historial no aparece en el catálogo, por ejemplo, un typo,
o un curso que no está entre los primeros 4 semestres, el programa no se detiene, sino que simplemente se
imprime una advertencia por la salida de error y ese código simplemente se ignora, y el programa continua
leyendo el resto del historial.

### Choque de horarios

El enunciado pide reportar si un curso choca "con al menos otro curso/grupo del
catálogo", en el caso de las dos mayas curriculares escogidas esto se da para casi todos los grupos de los cursos, 
lo cual es normal ya que esta es simplemente la forma en la que el tec ofrece sus horarios, impartiendo lecciones 
a la misma hora y el mismo dia para varios cursos, algunas veces incluso dando el mismo curso a la misma hora y el mismo dia, pero dado por profesores distintos; sin embargo, como en un caso real
un estudiante matricularia un solo grupo de un curso, al momento comparar horarios se excluyen explícitamente los pares de grupos que pertenecen al mismo curso.

### Correquisitos que se referencian entre sí

Hay pares de cursos que son correquisito uno del otro (por ejemplo, `QU1102` y
`QU1106`, o `QU1104` y `QU1107` en Física). Si la función que revisa correquisitos
simplemente llamara a sí misma para revisar el correquisito del correquisito, se
quedaría en un ciclo infinito, asi que para evitar esto la función lleva una lista de
"ancestros", que son los cursos que ya se estan revisando en la cadena actual; si el correquisito
que está revisando ya es parte de esa cadena, simplemente lo ignora y sigue, en vez de
volver a evaluarlo.

## Justificación del formato de salida

Inicialmente se consideraron dos formatos: JSON, y CSV, al final elegimos JSON para el archivo de salida ya que varios
campos del curso son listas de longitud variable, como los requisitos, correquisitos, y grupos; escribir
todo eso en una fila de CSV requeriria inventar un separador extra dentro de cada
celda para simular una lista anidada dentro de otra lista, lo cual sobrecomplicaria inecesariamente las cosas considerando que JSON puede usarse para representar listas anidadas de forma nativa, por lo que el
resultado final es mucho mas amigable para ser leido posteriormente por racket.

## Estructuras de datos desarrolladas

Todos los `structs` del proyecto usan arreglos de tamaño fijo en vez de memoria
reservada dinámicamente (con la única excepción del arreglo temporal de
`scheduleClash.c`, que se explica más abajo); decidimos hacerlo así porque nos pareció más seguro trabajar con tamaños conocidos de
antemano que manejar `malloc`/`free` en todos los lugares donde se guardan cursos, grupos o requisitos; como se menciono anteriormente estos valores se guardane en constants.h, y los structs se definene en el header types.h.

```c
typedef struct {
    char day[4];
    char startTime[maxTimeLen];
    char endTime[maxTimeLen];
} ScheduleBlock;

typedef struct {
    char groupId[maxGroupIdLen];
    ScheduleBlock blocks[maxScheduleBlocks];
    int blockCount;
    int hasClash;
} CourseGroup;

typedef struct {
    char courseCode[maxCourseCodeLen];
    char courseName[maxCourseNameLen];
    int credits;
    CourseGroup groups[maxGroupsPerCourse];
    int groupCount;
    char requisites[maxRequisites][maxCourseCodeLen];
    int requisiteCount;
    char corequisites[maxCorequisites][maxCourseCodeLen];
    int corequisiteCount;
    int hasScheduleClash;
    int canEnroll;
    int hasCycle;
} Course;

typedef struct {
    Course courses[maxCourses];
    int courseCount;
} Catalog;

typedef struct {
    char studentId[maxStudentIdLen];
    char studentName[maxStudentNameLen];
    char gender;
    char approvedCourses[maxHistory][maxCourseCodeLen];
    int approvedCount;
} StudentHistory;
```

Los tres últimos campos de `Course` (`hasScheduleClash`, `canEnroll`, `hasCycle`) no
vienen del archivo de entrada, sino que se calculan en etapas posteriores a la carga
(`scheduleClash.c` y `Requisites.c`), por eso se agrupan al final del `struct`,
separados de los campos que sí vienen directo del archivo.

La única excepción a esta regla de que todo es arreglo fijo está en `detectScheduleClashes`, donde se
usa `malloc` para armar un arreglo de referencias a todos los grupos del catálogo (uno
por cada grupo real que existe, sin importar de qué curso es), porque ahí sí conviene
que el tamaño se ajuste a la cantidad real de grupos cargados en vez de reservar de más
por adelantado, y posteriormente ese arreglo se libera con `free` antes de que la función termine.

## Detección de ciclos (requisito extra por grupo de 4)

El conjunto de requisitos de un curso forma un grafo dirigido: cada curso apunta a sus
requisitos; para detectar ciclos en ese grafo, se implementó un recorrido DFS con 3
colores usados para diferenciar estados: (blanco: no visitado, gris: en el camino actual, negro: ya terminado).

Si durante el recorrido se llega a un curso que está en gris (es decir, ya está en el
camino que se está siguiendo en este momento), significa que hay un ciclo, y se guarda
la porción del camino desde ese curso hasta el actual como el ciclo encontrado; el resultado se guarda en un `CycleReport` por cada ciclo detectado:

```c
typedef struct {
    int courseIdx[maxCourses];
    int courseCount;
} CycleReport;
```

Y además, cada curso que forma parte de algún ciclo queda marcado con
`hasCycle = 1` en su propio `struct Course`, para que quede disponible en el archivo de
salida sin que la siguiente etapa tenga que volver a correr el algoritmo.

## Limitaciones detectadas hasta el momento

- Si el archivo de catálogo tiene dos cursos con el mismo código, ambos se cargan, pero
  la búsqueda por código siempre va a encontrar el primero, esto en la practica nunca deberia de pasar asi que se decide omitir este caso.
  
- La exportación a JSON no soporta comillas ni barras invertidas dentro de nombres de
  curso; con los datos actuales de ambos catalogos esto nunca pasa, pero si algún nombre de
  curso llegara a tener uno de esos caracteres, el JSON resultante quedaría inválido.

## Casos límite que se encontraron durante el desarollo

Algunas de las constantes definidas en el header constants.h fueron los valores maximos 
de algun elemento soportado por el programa, por ejemplo, maxima cantidad de cursos en un grupo, 
longitud maxima de los strings o enteros que conforman los ID de curso y de estudiantes, etc, 
estos parametros se definieron para ahorrarnos el tener que definir estos tamaños con malloc o realloc independientemente para cada dato, pero tambien para implementar comprobaciones que se aseguraran de que 
los datos leidos en los archivos de input se mantuvieran dentro de margenes razonables y facilitaran 
la deteccion de errores en dichos archivos de input, sin embargo varias veces nos encontramos con
que los valores definidos inicialmente no eran suficientemente grandes para para manejar algunos casos particulares en los parametros de los input.

Por ejemplo, inicialmente se asigno a `catalogLoader` un buffer de
lectura de línea de 128 caracteres, lo cual era suficiente para los cursos de prueba
que se uso al principio, pero al correrlo usando el catálogo real de CE, la línea `GRUPOS` de
`MA1102` (que en aquel momento tenia 20 grupos) resultó tener 625 caracteres — muchísimo más que el
buffer de 128, razon por la cual `fgets` cortaba la línea a la mitad sin ningún aviso, y el resto de esa
línea se leía como si fuera texto nuevo en la siguiente vuelta del ciclo, que no
calzaba con ninguna etiqueta conocida y por tanto era ignorado por el programa, razon por la cual el catálogo 
parecia cargarse sin errores, pero tenia la informacion de varios cursos "truncada" a la mitad, esto nos llevo 
a modificar varias veces estos valores maximos o de buffer hasta encontrar un limite que si fuera razonable, 
y a programar una advertencia cada vez que se alcanza uno de estos limites.

Otro problema que nos encontramos se dio al momento de escribir a la terminar errores o advertencias al ejecutar el programa desde Clion,
ya que algunas advertencias de error aparecían "mezcladas" en medio del texto normal del programa, aparentemente esto pasa porque al ejecutarse a través de un pipe (como
hace CLion), la salida estándar de C deja de vaciarse línea por línea y empieza a
acumularse en un buffer más grande, mientras que la salida de error sigue
imprimiéndose de inmediato, esto se resolvió forzando el buffer por línea de la salida
estándar con `setvbuf` al inicio de `main`.

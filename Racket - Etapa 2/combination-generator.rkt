#lang racket/base

;; combination-generator.rkt  (Cuartil 2)
;;
;; Genera, de forma perezosa (con streams), todas las combinaciones de horario
;; validas: un grupo por cada curso de interes, sin choques entre los grupos.
;;
;; Idea general (backtracking / DFS con poda temprana):
;;   - Se van eligiendo los cursos de uno en uno, en el orden de las preferencias.
;;   - Antes de agregar un grupo a la combinacion parcial se revisa si choca con
;;     los grupos ya elegidos. Si choca, esa rama se abandona de inmediato
;;     (poda): nunca se exploran los cursos que faltaban por elegir.
;;   - Las combinaciones se producen una por una, solo cuando alguien las pide.
;;
;; No se usa ninguna mutacion: el estado ("lo que llevo elegido") viaja como
;; parametro de las funciones recursivas.

(require racket/contract
         racket/list
         racket/stream
         "Types.rkt")

(provide generate-combinations
         blocks-clash?
         groups-clash?)

;; Un candidato es un par (codigo-del-curso . grupo)
(define candidate/c (cons/c string? course-group?))

;; ------------------------------------------------------------
;; Choques de horario (se recalculan con los bloques crudos)
;; ------------------------------------------------------------

;; time->number : string? -> exact-nonnegative-integer?
;; Convierte una hora "HHMM" a numero: "0730" -> 730, "1650" -> 1650.
;; Como el formato es HHMM, comparar los numeros equivale a comparar las horas.
(define/contract (time->number t)
  (-> string? exact-nonnegative-integer?)
  (string->number t))

;; blocks-clash? : schedule-block? schedule-block? -> boolean?
;; Dos bloques chocan si son el mismo dia y sus rangos se traslapan.
;; Si uno termina justo cuando empieza el otro (0920 y 0920) NO chocan.
(define/contract (blocks-clash? a b)
  (-> schedule-block? schedule-block? boolean?)
  (define start-a (time->number (schedule-block-start-time a)))
  (define end-a   (time->number (schedule-block-end-time a)))
  (define start-b (time->number (schedule-block-start-time b)))
  (define end-b   (time->number (schedule-block-end-time b)))
  (and (string=? (schedule-block-day a) (schedule-block-day b))
       (< start-a end-b)
       (< start-b end-a)))

;; block-clashes-with-any? : schedule-block? (listof schedule-block?) -> boolean?
;; Un bloque contra una lista de bloques.
(define/contract (block-clashes-with-any? block blocks)
  (-> schedule-block? (listof schedule-block?) boolean?)
  (ormap (lambda (other) (blocks-clash? block other)) blocks))

;; groups-clash? : course-group? course-group? -> boolean?
;; Dos grupos chocan si algun bloque de uno choca con algun bloque del otro.
(define/contract (groups-clash? g1 g2)
  (-> course-group? course-group? boolean?)
  (define blocks-2 (course-group-blocks g2))
  (ormap (lambda (block) (block-clashes-with-any? block blocks-2))
         (course-group-blocks g1)))

;; clashes-with-chosen? : course-group? (listof candidate/c) -> boolean?
;; Un grupo nuevo contra todos los grupos que ya llevo elegidos.
(define/contract (clashes-with-chosen? group chosen)
  (-> course-group? (listof candidate/c) boolean?)
  (ormap (lambda (candidate) (groups-clash? group (cdr candidate)))
         chosen))

;; ------------------------------------------------------------
;; Preparacion de los candidatos
;; ------------------------------------------------------------

;; find-course : string? (listof course?) -> (or/c course? #f)
(define/contract (find-course code courses)
  (-> string? (listof course?) (or/c course? #f))
  (findf (lambda (c) (string=? (course-course-code c) code)) courses))

;; interest->candidates : (cons/c string? (listof string?)) (listof course?)
;;                        -> (or/c (listof candidate/c) #f)
;; Para un curso de interes devuelve la lista de sus grupos candidatos
;; (solo los grupos que el estudiante pidio). Devuelve #f si el curso no
;; se puede usar: no existe, no esta habilitado (canEnroll = false) o no
;; le quedo ningun grupo.
(define/contract (interest->candidates interest courses)
  (-> (cons/c string? (listof string?)) (listof course?)
      (or/c (listof candidate/c) #f))
  (define code (car interest))
  (define wanted-group-ids (cdr interest))
  (define found (find-course code courses))
  (cond
    [(not found) #f]
    [(not (course-can-enroll found))
     (eprintf "Advertencia: '~a' no esta habilitado (canEnroll = false), se omite\n" code)
     #f]
    [else
     (define wanted-groups
       (filter (lambda (g) (member (course-group-group-id g) wanted-group-ids))
               (course-groups found)))
     (cond
       [(null? wanted-groups) #f]
       [else (map (lambda (g) (cons code g)) wanted-groups)])]))

;; build-candidate-lists : preference? (listof course?) -> (listof (listof candidate/c))
;; Una lista de candidatos por cada curso de interes utilizable.
;; Ejemplo: (list (list (CE1101 . g1) (CE1101 . g2)) (list (CE1104 . g1)) ...)
(define/contract (build-candidate-lists prefs courses)
  (-> preference? (listof course?) (listof (listof candidate/c)))
  (filter-map (lambda (interest) (interest->candidates interest courses))
              (preference-interests prefs)))

;; ------------------------------------------------------------
;; Streams: append perezoso
;; ------------------------------------------------------------

;; stream-append-lazy : stream? (-> stream?) -> stream?
;; Igual que stream-append, pero el segundo stream se pide con una funcion,
;; asi que NO se calcula hasta que el primero se termina.
;; (Con stream-append normal los dos argumentos se evaluan de una vez y la
;; busqueda dejaria de ser perezosa.)
(define/contract (stream-append-lazy first-stream make-second-stream)
  (-> stream? (-> stream?) stream?)
  (cond
    [(stream-empty? first-stream) (make-second-stream)]
    [else
     (stream-cons (stream-first first-stream)
                  (stream-append-lazy (stream-rest first-stream)
                                      make-second-stream))]))

;; ------------------------------------------------------------
;; Busqueda (DFS con poda)
;; ------------------------------------------------------------

;; chosen->combination : (listof candidate/c) -> combination?
;; chosen viene al reves (el ultimo elegido va primero), por eso el reverse.
(define/contract (chosen->combination chosen)
  (-> (listof candidate/c) combination?)
  (combination
   (reverse
    (map (lambda (candidate)
           (selection (car candidate)
                      (course-group-group-id (cdr candidate))))
         chosen))))

;; search : (listof (listof candidate/c)) (listof candidate/c) -> stream?
;; remaining = los cursos que faltan por elegir (cada uno con sus candidatos)
;; chosen    = los grupos ya elegidos (la combinacion parcial)
(define/contract (search remaining chosen)
  (-> (listof (listof candidate/c)) (listof candidate/c) stream?)
  (cond
    ;; Ya elegi un grupo para cada curso: esta combinacion esta completa
    [(null? remaining)
     (stream (chosen->combination chosen))]
    ;; Falta elegir: pruebo los candidatos del siguiente curso
    [else
     (try-candidates (first remaining) (rest remaining) chosen)]))

;; try-candidates : (listof candidate/c) (listof (listof candidate/c)) (listof candidate/c)
;;                  -> stream?
;; Prueba, uno por uno, los candidatos de un curso.
(define/contract (try-candidates candidates remaining chosen)
  (-> (listof candidate/c) (listof (listof candidate/c)) (listof candidate/c) stream?)
  (cond
    ;; No quedan candidatos de este curso: no hay mas combinaciones por aqui
    [(null? candidates) empty-stream]
    [else
     (define candidate (first candidates))
     (define other-candidates (rest candidates))
     (cond
       ;; PODA: este grupo choca con lo ya elegido, se descarta la rama
       ;; completa sin tocar los cursos restantes.
       [(clashes-with-chosen? (cdr candidate) chosen)
        (try-candidates other-candidates remaining chosen)]
       ;; No choca: sigo con los demas cursos usando este grupo, y cuando se
       ;; agoten esas combinaciones, pruebo el siguiente candidato.
       [else
        (stream-append-lazy
         (search remaining (cons candidate chosen))
         (lambda () (try-candidates other-candidates remaining chosen)))])]))

;; ------------------------------------------------------------
;; Funcion publica
;; ------------------------------------------------------------

;; generate-combinations : (listof course?) preference? -> stream?
;; Stream de combinaciones (combination?) sin choques, un grupo por cada
;; curso de interes utilizable. Si no hay ningun curso utilizable el stream
;; es vacio (no se devuelve una combinacion vacia).
(define/contract (generate-combinations courses prefs)
  (-> (listof course?) preference? stream?)
  (define candidate-lists (build-candidate-lists prefs courses))
  (cond
    [(null? candidate-lists) empty-stream]
    [else (search candidate-lists '())]))

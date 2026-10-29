#lang racket/base
;; OutputWriter.rkt
;; Exporta el stream de combinaciones a un archivo JSON SIN materializarlo en una lista:
;; cada combinacion se escribe apenas el stream la produce (memoria constante).
;; Estructura del archivo:
;;   { "creditsMin": n, "creditsMax": n,
;;     "combinations": [ {"id", "totalCredits", "selections": [...]}, ... ],
;;     "combinationCount": n }
;; combinationCount va AL FINAL porque solo se conoce cuando el stream se agoto.
;; Sin mutacion: el contador de ids viaja como parametro de un ciclo recursivo.

(require racket/contract
         racket/list
         racket/stream
         json
         "Types.rkt")

(provide write-combinations-to-json)

(define/contract (block->jsexpr b)
  (-> schedule-block? hash?)
  (hasheq 'day       (schedule-block-day b)
          'startTime (schedule-block-start-time b)
          'endTime   (schedule-block-end-time b)))

;; Indice inmutable codigo -> curso, construido una sola vez (en vez de findf por seleccion).
(define/contract (index-courses courses)
  (-> (listof course?) (hash/c string? course?))
  (for/hash ([c courses]) (values (course-course-code c) c)))

(define/contract (lookup-course code index)
  (-> string? hash? course?)
  (hash-ref index code
            (lambda () (raise-arguments-error 'write-combinations-to-json
                                              "el curso de la combinacion no esta en el catalogo"
                                              "course-code" code))))

(define/contract (lookup-group group-id crs)
  (-> string? course? course-group?)
  (or (findf (lambda (g) (string=? (course-group-group-id g) group-id))
             (course-groups crs))
      (raise-arguments-error 'write-combinations-to-json
                             "el grupo de la combinacion no esta en el curso"
                             "course-code" (course-course-code crs)
                             "group-id" group-id)))

(define/contract (selection->jsexpr sel index)
  (-> selection? hash? hash?)
  (define crs (lookup-course (selection-course-code sel) index))
  (define grp (lookup-group (selection-group-id sel) crs))
  (hasheq 'courseCode (course-course-code crs)
          'courseName (course-course-name crs)
          'groupId    (course-group-group-id grp)
          'career     (course-career crs)
          'credits    (course-credits crs)
          'blocks     (map block->jsexpr (course-group-blocks grp))))

;; (Contratos internos livianos: hash? en vez de jsexpr? o (hash/c ...), que recorren el arbol
;; o el indice completo en CADA llamada; con cientos de miles de combinaciones eso dominaba el tiempo.)
;; totalCredits se obtiene sumando los creditos ya consultados de cada seleccion
;; (equivale a combination-credits, sin reconstruir la tabla de creditos por combinacion).
(define/contract (combination->jsexpr comb id index)
  (-> combination? exact-positive-integer? hash? hash?)
  (define selections (map (lambda (sel) (selection->jsexpr sel index))
                          (combination-selections comb)))
  (hasheq 'id           id
          'totalCredits (apply + (map (lambda (s) (hash-ref s 'credits)) selections))
          'selections   selections))

;; Escribe el arreglo de combinaciones; devuelve cuantas escribio.
(define/contract (write-combinations-array out combos index)
  (-> output-port? stream? hash? exact-nonnegative-integer?)
  (let loop ([s combos] [id 1])
    (cond
      [(stream-empty? s) (sub1 id)]
      [else
       (unless (= id 1) (write-string "," out))
       (write-json (combination->jsexpr (stream-first s) id index) out)
       (loop (stream-rest s) (add1 id))])))

;; write-combinations-to-json : path-string? stream? (listof course?) preference? -> natural
;; Escribe a un archivo temporal y lo renombra al final: si algo falla a mitad del
;; stream no queda un JSON truncado en la ruta final. Devuelve la cantidad escrita.
(define/contract (write-combinations-to-json path combos courses prefs)
  (-> path-string? stream? (listof course?) preference? exact-nonnegative-integer?)
  (define index (index-courses courses))
  (define tmp (format "~a.tmp" path))
  (with-handlers ([exn:fail? (lambda (e)
                               (when (file-exists? tmp) (delete-file tmp))
                               (raise e))])
    (define count
      (call-with-output-file tmp
        (lambda (out)
          (fprintf out "{\"creditsMin\":~a,\"creditsMax\":~a,\"combinations\":["
                   (preference-credits-min prefs) (preference-credits-max prefs))
          (define n (write-combinations-array out combos index))
          (fprintf out "],\"combinationCount\":~a}" n)
          n)
        #:exists 'replace))
    (rename-file-or-directory tmp path #t)
    count))

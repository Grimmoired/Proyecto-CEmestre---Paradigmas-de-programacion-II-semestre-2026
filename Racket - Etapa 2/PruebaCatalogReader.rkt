#lang racket/base

;; test-catalog-reader.rkt

(require "Types.rkt"
         "CatalogReader.rkt")

(define total-tests 0)
(define passed-tests 0)

(define (check description actual expected)
  (set! total-tests (add1 total-tests))
  (cond
    [(equal? actual expected)
     (set! passed-tests (add1 passed-tests))
     (printf "PASS: ~a\n" description)]
    [else
     (printf "FAIL: ~a (esperado ~v, obtenido ~v)\n" description expected actual)]))

;; Caso 1: leer un solo archivo de sola carrera)
(define ce-courses (read-catalog (list "OutputCEPrueba.json")))

(check "Un solo archivo (CE): cantidad de cursos" (length ce-courses) 4)

(check "Un solo archivo (CE): todos los cursos quedan con la carrera correcta"
       (andmap (lambda (c) (string=? (course-career c) "Ingenieria en Computadores")) ce-courses)
       #t)

(define ce1101 (findf (lambda (c) (string=? (course-course-code c) "CE1101")) ce-courses))
(check "CE1101 existe y tiene 2 grupos" (length (course-groups ce1101)) 2)

(define ci0205 (findf (lambda (c) (string=? (course-course-code c) "CI0205")) ce-courses))
(check "CI0205 (sin horario) tiene 0 grupos" (length (course-groups ci0205)) 0)

(define ce1104 (findf (lambda (c) (string=? (course-course-code c) "CE1104")) ce-courses))
(check "CE1104 tiene 1 correquisito (CE1101)" (course-corequisites ce1104) (list "CE1101"))

(define primer-bloque (car (course-group-blocks (car (course-groups ce1101)))))
(check "Primer bloque de CE1101/1: dia" (schedule-block-day primer-bloque) "JUE")
(check "Primer bloque de CE1101/1: hora inicio" (schedule-block-start-time primer-bloque) "0730")
(check "Primer bloque de CE1101/1: hora fin" (schedule-block-end-time primer-bloque) "0920")

;; Caso 2: leer y combinar dos archivos (dos carreras)
(define ambas-carreras (read-catalog (list "OutputCEPrueba.json" "OutputFIPrueba.json")))

(check "Dos archivos combinados: cantidad total de cursos" (length ambas-carreras) 5)

(define codigos-fisica
  (map course-course-code (filter (lambda (c) (string=? (course-career c) "Ingenieria Fisica")) ambas-carreras)))
(check "Solo CA2125 viene marcado como Ingenieria Fisica" codigos-fisica (list "CA2125"))

;; Caso 3: un archivo inexistente debe dar error al intentar leerse
(define archivo-inexistente-fallo?
  (with-handlers ([exn:fail? (lambda (e) #t)])
    (read-catalog (list "esto-no-existe.json"))
    #f))
(check "Archivo inexistente produce un error" archivo-inexistente-fallo? #t)

(printf "\n~a/~a pruebas pasaron\n" passed-tests total-tests)

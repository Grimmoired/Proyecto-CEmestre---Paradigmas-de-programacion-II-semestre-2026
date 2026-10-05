#lang racket/base

(require "Types.rkt"
         "CatalogReader.rkt"
         "PreferencesReader.rkt"
         "DataLoader.rkt")

(displayln "Prueba 1: CatalogReader.rkt")
(define courses (read-catalog (list "OutputCEPrueba.json" "OutputFIPrueba.json")))
(printf "Total cursos leidos: ~a\n" (length courses))
(for ([c courses])
  (printf "  ~a (~a) - carrera: ~a - grupos: ~a\n"
          (course-course-code c) (course-course-name c)
          (course-career c) (length (course-groups c))))

(newline)
(displayln "Prueba 2: PreferencesReader.rkt")
(define prefs-ok (read-preferences "PreferenciasPrueba.rktd"))
(printf "Creditos: ~a a ~a\n" (preference-credits-min prefs-ok) (preference-credits-max prefs-ok))
(for ([i (preference-interests prefs-ok)])
  (printf "  ~a -> grupos ~a\n" (car i) (cdr i)))

(newline)
(displayln "Prueba 3: ValidatePreferences con errores deliberados")
(displayln "(Se esperan 4 advertencias por stderr antes del resultado)")
(define prefs-raw (read-preferences "PreferenciasPruebaError.rktd"))
(define prefs-validadas (validate-preferences prefs-raw courses))
(printf "Intereses que sobrevivieron la validacion: ~a\n" (length (preference-interests prefs-validadas)))
(for ([i (preference-interests prefs-validadas)])
  (printf "  ~a -> grupos ~a\n" (car i) (cdr i)))

(newline)
(displayln "Prueba 4: DataLoader.rkt")
(define-values (courses2 prefs2)
  (load-data (list "OutputCEPrueba.json" "OutputFIPrueba.json") "PreferenciasPrueba.rktd"))
(printf "load-data devolvio ~a cursos y ~a intereses validados\n"
        (length courses2) (length (preference-interests prefs2)))

(newline)
(displayln "=== Fin de las pruebas del Cuartil 1 ===")

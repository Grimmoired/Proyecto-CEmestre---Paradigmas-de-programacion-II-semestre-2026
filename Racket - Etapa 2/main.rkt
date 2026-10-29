#lang racket/base

(require racket/runtime-path
         "DataLoader.rkt"
         "combination-generator.rkt"
         "credit-filter.rkt"
         "output-writer.rkt")

(define-runtime-path ce-catalog-path          "../OutputCE.json")
(define-runtime-path if-catalog-path          "../OutputIF.json")
(define-runtime-path default-preferences-path "PreferenciasPrueba.rktd")
(define-runtime-path output-path              "CombinacionesSalida.json")

;; run : path-string? -> void?
(define (run preferences-path)
  (define-values (courses prefs)
    (load-data (list ce-catalog-path if-catalog-path) preferences-path))
  (define combos
    (filter-by-credits (generate-combinations courses prefs) courses prefs))
  (write-combinations-to-json output-path combos courses prefs)
  (printf "Exportacion completada: ~a\n" output-path))

(module+ main
  (define args (current-command-line-arguments))
  (define preferences-path
    (if (> (vector-length args) 0)
        (vector-ref args 0)
        default-preferences-path))
  (with-handlers ([exn:fail? (lambda (e)
                               (eprintf "Error: ~a\n" (exn-message e))
                               (exit 1))])
    (run preferences-path)))
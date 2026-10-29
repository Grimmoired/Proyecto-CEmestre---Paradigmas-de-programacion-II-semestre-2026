#lang racket/base
(require racket/runtime-path
         racket/list
         "DataLoader.rkt"
         "CombinationGenerator.rkt"
         "CreditFilter.rkt"
         "OutputWriter.rkt")

;; AJUSTAR: rutas por defecto (usar aqui los JSON reales de 6 semestres de la Etapa 1).
(define-runtime-path default-ce-catalog   "OutputCE.json")
(define-runtime-path default-fi-catalog   "OutputFI.json")
(define-runtime-path default-preferences  "PreferenciasCE.rktd")
(define-runtime-path default-output       "CombinacionesSalida.json")

;; parse-args : (listof string?) -> (values (or/c string? #f) (listof string?) (or/c string? #f))
;; Devuelve (preferencias catalogos salida). Recursivo, sin mutacion.
(define (parse-args args)
  (let loop ([as args] [prefs #f] [catalogs '()] [out #f])
    (cond
      [(null? as) (values prefs (reverse catalogs) out)]
      [(member (first as) '("-c" "-o"))
       (when (null? (rest as))
         (raise-user-error (format "falta el valor despues de '~a'" (first as))))
       (if (string=? (first as) "-c")
           (loop (cddr as) prefs (cons (second as) catalogs) out)
           (loop (cddr as) prefs catalogs (second as)))]
      [prefs (raise-user-error (format "argumento inesperado '~a'" (first as)))]
      [else (loop (rest as) (first as) catalogs out)])))

;; run : path-string? (listof path-string?) path-string? -> natural
(define (run preferences-path catalog-paths output-path)
  (define-values (courses prefs) (load-data catalog-paths preferences-path))
  (define combos
    (filter-by-credits (generate-combinations courses prefs) courses prefs))
  (define count (write-combinations-to-json output-path combos courses prefs))
  (printf "Exportacion completada: ~a combinaciones -> ~a\n" count output-path)
  (when (zero? count)
    (eprintf "Advertencia: ninguna combinacion cumple las restricciones; el archivo quedo con la lista vacia\n"))
  count)

(module+ main
  (with-handlers ([exn:fail? (lambda (e)
                               (eprintf "Error: ~a\n" (exn-message e))
                               (exit 1))])
    (define-values (prefs-arg catalogs-arg out-arg)
      (parse-args (vector->list (current-command-line-arguments))))
    (void (run (or prefs-arg default-preferences)
               (if (null? catalogs-arg) (list default-ce-catalog default-fi-catalog) catalogs-arg)
               (or out-arg default-output)))))

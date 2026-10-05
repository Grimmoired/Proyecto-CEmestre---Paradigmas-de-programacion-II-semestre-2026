#lang racket/base

(require racket/contract
         "Types.rkt"
         "CatalogReader.rkt"
         "PreferencesReader.rkt")

(provide load-data)

;; load-data : (listof path-string?) path-string? -> (values (listof course?) preference?)
(define/contract (load-data catalog-paths preferences-path)
  (-> (listof path-string?) path-string? (values (listof course?) preference?))
  (define courses (read-catalog catalog-paths))
  (define raw-preferences (read-preferences preferences-path))
  (define preferences (validate-preferences raw-preferences courses))
  (values courses preferences))
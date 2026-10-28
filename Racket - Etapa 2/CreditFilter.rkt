#lang racket/base
 
(require racket/contract
         racket/list
         racket/stream
         "Types.rkt")
 
(provide filter-stream
         combination-credits
         credits-in-range?
         filter-by-credits)
 
;; filter-stream : (All (A) (-> (-> A Boolean) (Streamof A) (Streamof A)))
(define/contract (filter-stream pred s)
  (-> (-> any/c boolean?) stream? stream?)
  (let loop ([s s])
    (cond
      [(stream-empty? s) empty-stream]
      [else
       (define x (stream-first s))
       (if (pred x)
           (stream-cons x (loop (stream-rest s)))
           (loop (stream-rest s)))])))
 
;; credit-table : (listof course?) -> (hash/c string? exact-nonnegative-integer?)
(define/contract (credit-table courses)
  (-> (listof course?) (hash/c string? exact-nonnegative-integer?))
  (for/hash ([c courses])
    (values (course-course-code c) (course-credits c))))
 
(define/contract (combination-credits/table comb table)
  (-> combination? (hash/c string? exact-nonnegative-integer?) exact-nonnegative-integer?)
  (foldl (lambda (sel total)
           (define code (selection-course-code sel))
           (unless (hash-has-key? table code)
             (raise-arguments-error 'combination-credits
                                    "el curso de la combinacion no esta en el catalogo"
                                    "course-code" code))
           (+ total (hash-ref table code)))
         0
         (combination-selections comb)))
 
;; combination-credits : combination? (listof course?) -> exact-nonnegative-integer?
(define/contract (combination-credits comb courses)
  (-> combination? (listof course?) exact-nonnegative-integer?)
  (combination-credits/table comb (credit-table courses)))
 
;; credits-in-range? : lo hi courses -> (combination? -> boolean?)
;; Devuelve un predicado.
(define/contract (credits-in-range? lo hi courses)
  (-> exact-nonnegative-integer? exact-nonnegative-integer? (listof course?)
      (-> combination? boolean?))
  (define table (credit-table courses))
  (lambda (comb)
    (define total (combination-credits/table comb table))
    (and (>= total lo) (<= total hi))))
 
;; filter-by-credits : stream? (listof course?) preference? -> stream?
(define/contract (filter-by-credits combos courses prefs)
  (-> stream? (listof course?) preference? stream?)
  (define lo (preference-credits-min prefs))
  (define hi (preference-credits-max prefs))
  (cond
    [(> lo hi)
     (eprintf "Advertencia: el rango de creditos es invalido (minimo ~a > maximo ~a), no hay combinaciones\n" lo hi)
     empty-stream]
    [else
     (filter-stream (credits-in-range? lo hi courses) combos)]))
#lang racket/base

(require racket/contract
         racket/list
         "Types.rkt")

(provide read-preferences
         validate-preferences)

;; raw-interest->interest : (listof string?) -> (cons/c string? (listof string?)).
(define/contract (raw-interest->interest raw)
  (-> (cons/c string? (listof string?)) (cons/c string? (listof string?)))
  (cons (first raw) (rest raw)))

;; read-preferences : path-string? -> preference?
(define/contract (read-preferences path)
  (-> path-string? preference?)
  (define data (call-with-input-file path read))
  (define credits-min (cdr (assoc 'credits-min data)))
  (define credits-max (cdr (assoc 'credits-max data)))
  (define raw-interests (cdr (assoc 'interests data)))
  (preference credits-min
              credits-max
              (map raw-interest->interest raw-interests)))

;; find-course-by-code : string? (listof course?) -> (or/c course? #f)
(define/contract (find-course-by-code code courses)
  (-> string? (listof course?) (or/c course? #f))
  (findf (lambda (c) (string=? (course-course-code c) code)) courses))

;; validate-interest : (cons/c string? (listof string?)) (listof course?) -> (or/c (cons/c string? (listof string?)) #f)
(define/contract (validate-interest interest courses)
  (-> (cons/c string? (listof string?)) (listof course?)
      (or/c (cons/c string? (listof string?)) #f))
  (define code (car interest))
  (define wanted-groups (cdr interest))
  (define found (find-course-by-code code courses))
  (cond
    [(not found)
     (eprintf "Advertencia: el curso de interes '~a' no existe en el catalogo, se omite\n" code)
     #f]
    [else
     (define valid-group-ids (map course-group-group-id (course-groups found)))
     (for ([g wanted-groups] #:unless (member g valid-group-ids))
       (eprintf "Advertencia: el grupo '~a' de '~a' no existe en el catalogo, se omite\n" g code))
     (define kept-groups (filter (lambda (g) (member g valid-group-ids)) wanted-groups))
     (cond
       [(null? kept-groups)
        (eprintf "Advertencia: '~a' no quedo con ningun grupo valido de interes, se omite el curso\n" code)
        #f]
       [else (cons code kept-groups)])]))

;; validate-preferences : preference? (listof course?) -> preference?
(define/contract (validate-preferences prefs courses)
  (-> preference? (listof course?) preference?)
  (preference (preference-credits-min prefs)
              (preference-credits-max prefs)
              (filter-map (lambda (i) (validate-interest i courses))
                          (preference-interests prefs))))

#lang racket/base

(require racket/contract
         racket/list
         racket/stream
         "Types.rkt")

(provide generate-combinations
         blocks-clash?
         groups-clash?)

;; Los candidatos son pares codigo-grupo
(define candidate/c (cons/c string? course-group?))

(define/contract (time->number t)
  (-> string? exact-nonnegative-integer?)
  (string->number t))

(define/contract (blocks-clash? a b)
  (-> schedule-block? schedule-block? boolean?)
  (define start-a (time->number (schedule-block-start-time a)))
  (define end-a   (time->number (schedule-block-end-time a)))
  (define start-b (time->number (schedule-block-start-time b)))
  (define end-b   (time->number (schedule-block-end-time b)))
  (and (string=? (schedule-block-day a) (schedule-block-day b))
       (< start-a end-b)
       (< start-b end-a)))

(define/contract (block-clashes-with-any? block blocks)
  (-> schedule-block? (listof schedule-block?) boolean?)
  (ormap (lambda (other) (blocks-clash? block other)) blocks))

(define/contract (groups-clash? g1 g2)
  (-> course-group? course-group? boolean?)
  (define blocks-2 (course-group-blocks g2))
  (ormap (lambda (block) (block-clashes-with-any? block blocks-2))
         (course-group-blocks g1)))

(define/contract (clashes-with-chosen? group chosen)
  (-> course-group? (listof candidate/c) boolean?)
  (ormap (lambda (candidate) (groups-clash? group (cdr candidate)))
         chosen))

(define/contract (find-course code courses)
  (-> string? (listof course?) (or/c course? #f))
  (findf (lambda (c) (string=? (course-course-code c) code)) courses))

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

(define/contract (build-candidate-lists prefs courses)
  (-> preference? (listof course?) (listof (listof candidate/c)))
  (filter-map (lambda (interest) (interest->candidates interest courses))
              (preference-interests prefs)))

(define/contract (stream-append-lazy first-stream make-second-stream)
  (-> stream? (-> stream?) stream?)
  (cond
    [(stream-empty? first-stream) (make-second-stream)]
    [else
     (stream-cons (stream-first first-stream)
                  (stream-append-lazy (stream-rest first-stream)
                                      make-second-stream))]))

(define/contract (chosen->combination chosen)
  (-> (listof candidate/c) combination?)
  (combination
   (reverse
    (map (lambda (candidate)
           (selection (car candidate)
                      (course-group-group-id (cdr candidate))))
         chosen))))

(define/contract (search remaining chosen)
  (-> (listof (listof candidate/c)) (listof candidate/c) stream?)
  (cond
    [(null? remaining)
     (stream (chosen->combination chosen))]
    [else
     (try-candidates (first remaining) (rest remaining) chosen)]))


(define/contract (try-candidates candidates remaining chosen)
  (-> (listof candidate/c) (listof (listof candidate/c)) (listof candidate/c) stream?)
  (cond
    [(null? candidates) empty-stream]
    [else
     (define candidate (first candidates))
     (define other-candidates (rest candidates))
     (cond
       [(clashes-with-chosen? (cdr candidate) chosen)
        (try-candidates other-candidates remaining chosen)]
       [else
        (stream-append-lazy
         (search remaining (cons candidate chosen))
         (lambda () (try-candidates other-candidates remaining chosen)))])]))


(define/contract (generate-combinations courses prefs)
  (-> (listof course?) preference? stream?)
  (define candidate-lists (build-candidate-lists prefs courses))
  (cond
    [(null? candidate-lists) empty-stream]
    [else (search candidate-lists '())]))

#lang racket/base
(require racket/contract)

(provide
 (struct-out schedule-block)
 (struct-out course-group)
 (struct-out course)
 (struct-out preference))

(struct/contract schedule-block
  ([day string?]
   [start-time string?]
   [end-time string?])
  #:transparent)

(struct/contract course-group
  ([group-id string?]
   [has-clash boolean?]
   [blocks (listof schedule-block?)])
  #:transparent)

(struct/contract course
  ([course-code string?]
   [course-name string?]
   [credits exact-nonnegative-integer?]
   [requisites (listof string?)]
   [corequisites (listof string?)]
   [groups (listof course-group?)]
   [has-schedule-clash boolean?]
   [can-enroll boolean?]
   [has-cycle boolean?]
   [career string?])
  #:transparent)

(struct/contract preference
  ([credits-min exact-nonnegative-integer?]
   [credits-max exact-nonnegative-integer?]
   [interests (listof (cons/c string? (listof string?)))])
  #:transparent)

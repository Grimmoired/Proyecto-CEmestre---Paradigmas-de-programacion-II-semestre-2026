#lang racket/base

(require racket/contract
         racket/list      ; para findf
         racket/stream
         json             ; para jsexpr? y write-json
         "Types.rkt"
         "credit-filter.rkt")
        
(provide write-combinations-to-json)

(define/contract (block->jsexpr b)
  (-> schedule-block? jsexpr?)
  (hasheq 'day       (schedule-block-day b)
          'startTime (schedule-block-start-time b)
          'endTime   (schedule-block-end-time b)))

(define/contract (find-course code courses)
  (-> string? (listof course?) course?)
  (findf (lambda (c) (string=? (course-course-code c) code)) courses))

(define/contract (find-group group-id crs)
  (-> string? course? course-group?)
  (findf (lambda (g) (string=? (course-group-group-id g) group-id))
         (course-groups crs)))

(define/contract (selection->jsexpr sel courses)
  (-> selection? (listof course?) jsexpr?)
  (define crs (find-course (selection-course-code sel) courses))
  (define grp (find-group (selection-group-id sel) crs))
  (hasheq 'courseCode (course-course-code crs)
          'courseName (course-course-name crs)
          'groupId    (course-group-group-id grp)
          'career     (course-career crs)
          'credits    (course-credits crs)
          'blocks     (map block->jsexpr (course-group-blocks grp))))

(define/contract (combination->jsexpr comb id courses)
  (-> combination? exact-positive-integer? (listof course?) jsexpr?)
  (hasheq 'id           id
          'totalCredits (combination-credits comb courses)
          'selections   (map (lambda (sel) (selection->jsexpr sel courses))
                             (combination-selections comb))))

(define/contract (write-combinations-to-json path combos courses prefs)
  (-> path-string? stream? (listof course?) preference? void?)
  (define combo-list (stream->list combos))
  (define ids (range 1 (add1 (length combo-list))))
  (define data
    (hasheq 'creditsMin       (preference-credits-min prefs)
            'creditsMax       (preference-credits-max prefs)
            'combinationCount (length combo-list)
            'combinations     (map (lambda (comb id) (combination->jsexpr comb id courses))
                                   combo-list
                                   ids)))
  (call-with-output-file path
    (lambda (out) (write-json data out))
    #:exists 'replace))

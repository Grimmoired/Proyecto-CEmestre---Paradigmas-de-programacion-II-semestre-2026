#lang racket/base

(require racket/contract
         json
         racket/list
         "Types.rkt")
(provide read-catalog)


(define/contract (jsexpr->schedule-block j)
  (-> jsexpr? schedule-block?)
  (schedule-block (hash-ref j 'day)
                   (hash-ref j 'startTime)
                   (hash-ref j 'endTime)))

(define/contract (jsexpr->course-group j)
  (-> jsexpr? course-group?)
  (course-group (hash-ref j 'groupId)
                (hash-ref j 'hasClash)
                (map jsexpr->schedule-block (hash-ref j 'blocks))))

(define/contract (jsexpr->course j career)
  (-> jsexpr? string? course?)
  (course (hash-ref j 'courseCode)
          (hash-ref j 'courseName)
          (hash-ref j 'credits)
          (hash-ref j 'requisites)
          (hash-ref j 'corequisites)
          (map jsexpr->course-group (hash-ref j 'groups))
          (hash-ref j 'hasScheduleClash)
          (hash-ref j 'canEnroll)
          (hash-ref j 'hasCycle)
          career))

(define/contract (read-catalog-file path)
  (-> path-string? (listof course?))
  (define doc (call-with-input-file path read-json))
  (define career (hash-ref doc 'carrera))
  (map (lambda (j) (jsexpr->course j career)) (hash-ref doc 'courses)))

(define/contract (read-catalog paths)
  (-> (listof path-string?) (listof course?))
  (append-map read-catalog-file paths))

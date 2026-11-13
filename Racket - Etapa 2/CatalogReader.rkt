#lang racket/base

(require racket/contract
         json
         racket/list
         racket/string
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

(define/contract (jsexpr->area j)
  (-> jsexpr? string?)
  (define code (hash-ref j 'courseCode))
  (define area
    (hash-ref j 'area
              (lambda ()
                (raise-arguments-error
                 'read-catalog
                 "el curso no tiene el campo 'area' (obligatorio); regenerar el catalogo con la version actual de la etapa 1"
                 "course-code" code))))
  (unless (and (string? area) (positive? (string-length area)))
    (raise-arguments-error 'read-catalog
                           "el campo 'area' del curso esta vacio o no es texto"
                           "course-code" code
                           "area" area))
  area)

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
          career
          (jsexpr->area j)))

(define/contract (read-catalog-file path)
  (-> path-string? (listof course?))
  (define doc (call-with-input-file path read-json))
  (define career (hash-ref doc 'carrera))
  (map (lambda (j) (jsexpr->course j career)) (hash-ref doc 'courses)))

(define (usable? c) (and (course-can-enroll c) (pair? (course-groups c))))

(define/contract (pick-copy copies)
  (-> (non-empty-listof course?) course?)
  (or (findf usable? copies)
      (findf course-can-enroll copies)
      (first copies)))

(define/contract (copies-of code courses)
  (-> string? (listof course?) (listof course?))
  (filter (lambda (c) (string=? (course-course-code c) code)) courses))

(define/contract (dedupe-courses courses)
  (-> (listof course?) (listof course?))
  (define codes (remove-duplicates (map course-course-code courses)))
  (define shared
    (filter (lambda (code) (> (length (copies-of code courses)) 1)) codes))
  (define differing
    (filter (lambda (code)
              (define groups (map course-groups (copies-of code courses)))
              (not (andmap (lambda (g) (equal? g (first groups))) (rest groups))))
            shared))
  (unless (null? shared)
    (eprintf "Advertencia: ~a cursos aparecen en mas de un catalogo; se usa una sola copia por codigo (la primera habilitada y con grupos, segun el orden de los catalogos): ~a\n"
             (length shared) (string-join shared ", ")))
  (unless (null? differing)
    (eprintf "Advertencia: las copias de estos cursos tienen grupos distintos entre catalogos (revisar contra la Guia de Horarios): ~a\n"
             (string-join differing ", ")))
  (map (lambda (code) (pick-copy (copies-of code courses))) codes))

(define/contract (read-catalog paths)
  (-> (listof path-string?) (listof course?))
  (dedupe-courses (append-map read-catalog-file paths)))
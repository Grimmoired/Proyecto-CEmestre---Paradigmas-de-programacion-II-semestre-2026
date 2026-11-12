#lang racket/base
(require racket/list
         racket/file
         racket/stream
         json
         "Types.rkt"
         "CatalogReader.rkt"
         "CombinationGenerator.rkt"
         "OutputWriter.rkt")

(define-syntax-rule (t label actual expected)
  (list label (lambda () actual) expected))

(define (run-tests tests)
  (define results
    (for/list ([tst tests])
      (define actual
        (with-handlers ([exn:fail? (lambda (e) (list 'excepcion (exn-message e)))])
          ((second tst))))
      (define ok (equal? actual (third tst)))
      (printf "[~a] ~a~a\n" (if ok "PASS" "FAIL") (first tst)
              (if ok "" (format "\n   esperado: ~s\n   obtenido: ~s" (third tst) actual)))
      ok))
  (define failed (length (filter not results)))
  (printf "\nResumen: ~a/~a pruebas pasaron.\n" (- (length results) failed) (length results))
  (when (> failed 0) (exit 1)))

(define (quiet thunk)
  (parameterize ([current-error-port (open-output-string)]) (thunk)))
(define (error-message thunk)
  (with-handlers ([exn:fail? exn-message]) (quiet thunk) #f))

(define (course-json code credits #:area [area "AreaX"] #:enroll [enroll #t] #:groups [groups '("1")])
  (define base
    (hasheq 'courseCode code 'courseName code 'credits credits
            'requisites '() 'corequisites '()
            'groups (for/list ([g groups])
                      (hasheq 'groupId g 'hasClash #f
                              'blocks (list (hasheq 'day "LUN" 'startTime "0800" 'endTime "0950"))))
            'hasScheduleClash #f 'canEnroll enroll 'hasCycle #f))
  (if area (hash-set base 'area area) base))

(define (catalog-file career . courses)
  (define path (make-temporary-file "catalogo~a.json"))
  (call-with-output-file path #:exists 'truncate
    (lambda (out) (write-json (hasheq 'carrera career 'courses courses) out)))
  path)

(define (career-of code courses)
  (course-career (findf (lambda (c) (string=? (course-course-code c) code)) courses)))

;; ------------------------------------------------------------ pruebas
(run-tests
 (list
  (t "lee el area de cada curso"
     (map course-area (read-catalog (list (catalog-file "CarreraA"
                                                        (course-json "A1" 3 #:area "Matematicas")
                                                        (course-json "A2" 3 #:area "Electronica")))))
     '("Matematicas" "Electronica"))
  (t "falta el campo area -> error claro que nombra el curso"
     (let ([m (error-message (lambda () (read-catalog (list (catalog-file "CarreraA" (course-json "SINAREA" 3 #:area #f))))))])
       (and m (regexp-match? #rx"SINAREA" m) (regexp-match? #rx"area" m)))
     #t)
  (t "area vacia -> error"
     (and (error-message (lambda () (read-catalog (list (catalog-file "CarreraA" (course-json "VACIA" 3 #:area ""))))))
          #t)
     #t)

  (t "un codigo en dos catalogos queda una sola vez"
     (length (quiet (lambda ()
                      (read-catalog (list (catalog-file "CarreraA" (course-json "X" 3))
                                          (catalog-file "CarreraB" (course-json "X" 3)))))))
     1)
  (t "se prefiere la copia habilitada y con grupos aunque no sea la primera"
     (career-of "X" (quiet (lambda ()
                             (read-catalog (list (catalog-file "CarreraA" (course-json "X" 3 #:enroll #f))
                                                 (catalog-file "CarreraB" (course-json "X" 3)))))))
     "CarreraB")
  (t "si ambas copias sirven gana el primer catalogo (el orden es la prioridad)"
     (let ([a (catalog-file "CarreraA" (course-json "X" 3))]
           [b (catalog-file "CarreraB" (course-json "X" 3))])
       (list (career-of "X" (quiet (lambda () (read-catalog (list a b)))))
             (career-of "X" (quiet (lambda () (read-catalog (list b a)))))))
     '("CarreraA" "CarreraB"))
  (t "una copia sin grupos pierde contra una con grupos (caso SE1100: CE con grupos, FI sin grupos)"
     (career-of "X" (quiet (lambda ()
                             (read-catalog (list (catalog-file "CarreraA" (course-json "X" 0 #:groups '()))
                                                 (catalog-file "CarreraB" (course-json "X" 0)))))))
     "CarreraB")

  (t "la carrera y el area de la salida son las de la copia que uso el generador"
     (let* ([courses (quiet (lambda ()
                              (read-catalog (list (catalog-file "CarreraA" (course-json "X" 3 #:area "AreaA"))
                                                  (catalog-file "CarreraB" (course-json "X" 3 #:area "AreaB"))))))]
            [prefs (preference 0 99 '(("X" "1")))]
            [out (make-temporary-file "salida~a.json")])
       (write-combinations-to-json out (generate-combinations courses prefs) courses prefs)
       (define sel (first (hash-ref (first (hash-ref (call-with-input-file out read-json) 'combinations)) 'selections)))
       (list (hash-ref sel 'career) (hash-ref sel 'area)))
     '("CarreraA" "AreaA"))
  (t "el area se exporta por curso en cada combinacion"
     (let* ([courses (read-catalog (list (catalog-file "CarreraA" (course-json "A1" 3 #:area "Matematicas")
                                                       (course-json "A2" 3 #:area "Electronica"))))]
            [prefs (preference 0 99 '(("A1" "1") ("A2" "1")))]
            [out (make-temporary-file "salida~a.json")])
       (write-combinations-to-json out (generate-combinations courses prefs) courses prefs)
       (define combos (hash-ref (call-with-input-file out read-json) 'combinations))
       (sort (remove-duplicates (for*/list ([c combos] [s (hash-ref c 'selections)]) (hash-ref s 'area))) string<?))
     '("Electronica" "Matematicas"))))
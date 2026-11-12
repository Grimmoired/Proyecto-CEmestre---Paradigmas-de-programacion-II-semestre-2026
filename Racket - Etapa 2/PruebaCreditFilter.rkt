#lang racket/base

(require racket/list
         racket/stream
         "Types.rkt"
         "DataLoader.rkt"
         "CombinationGenerator.rkt"
         "CreditFilter.rkt")

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

(define (raises? thunk)
  (with-handlers ([exn:fail? (lambda (e) #t)]) (thunk) #f))
(define (quiet thunk)
  (parameterize ([current-error-port (open-output-string)]) (thunk)))

(define (mk-course code credits)
  (course code code credits '() '() '() #f #t #f "Prueba" "Area"))
(define (comb . codes) 
  (combination (map (lambda (c) (selection c "1")) codes)))

(define courses (list (mk-course "A" 1) (mk-course "B" 2) (mk-course "C" 3) (mk-course "D" 4)))
(define (prefs lo hi) (preference lo hi '()))
(define (naturals-from n) (stream-cons n (naturals-from (add1 n))))

(define-values (real-courses real-prefs)
  (quiet (lambda ()
           (load-data (list "OutputCEPrueba.json" "OutputFIPrueba.json") "PreferenciasPrueba.rktd"))))

(run-tests
 (list
  (t "filter-stream: enteros"
     (stream->list (filter-stream even? (in-list (range 10))))
     '(0 2 4 6 8))
  (t "filter-stream: strings (otro tipo, misma funcion)"
     (stream->list (filter-stream (lambda (s) (char=? (string-ref s 0) #\C)) (stream "CE1101" "MA1403" "CA2125")))
     '("CE1101" "CA2125"))
  (t "filter-stream: stream vacio y ningun elemento cumple -> vacio"
     (list (stream-empty? (filter-stream even? empty-stream))
           (stream-empty? (filter-stream (lambda (x) #f) (stream 1 2 3))))
     '(#t #t))
  (t "filter-stream: es perezoso (primeros 3 pares de un stream infinito)"
     (for/list ([x (in-stream (filter-stream even? (naturals-from 0)))] [i (in-range 3)]) x)
     '(0 2 4))

  (t "combination-credits: suma de creditos (1+2+3)"
     (combination-credits (comb "A" "B" "C") courses)
     6)
  (t "combination-credits: combinacion vacia = 0"
     (combination-credits (combination '()) courses)
     0)
  (t "combination-credits: curso fuera del catalogo -> error"
     (raises? (lambda () (combination-credits (comb "A" "NOEXISTE") courses)))
     #t)

  (t "credits-in-range?: limites inclusivos (total 6 en 6..10 y 1..6)"
     (list ((credits-in-range? 6 10 courses) (comb "A" "B" "C"))
           ((credits-in-range? 1 6 courses) (comb "A" "B" "C")))
     '(#t #t))
  (t "credits-in-range?: fuera de rango por 1 (total 6 en 7..10 y 1..5)"
     (list ((credits-in-range? 7 10 courses) (comb "A" "B" "C"))
           ((credits-in-range? 1 5 courses) (comb "A" "B" "C")))
     '(#f #f))

  (t "filter-by-credits: deja solo las combinaciones dentro del rango (3..6)"
     (map (lambda (c) (combination-credits c courses))
          (stream->list (filter-by-credits (stream (comb "A") (comb "A" "B") (comb "A" "B" "C") (comb "D" "C" "B"))
                                           courses (prefs 3 6))))
     '(3 6))
  (t "filter-by-credits: rango invertido -> stream vacio"
     (quiet (lambda () (stream-empty? (filter-by-credits (stream (comb "A")) courses (prefs 10 1)))))
     #t)

  (t "integracion: generador + filtro con PreferenciasPrueba (10..14) -> 2 combinaciones"
     (length (stream->list (filter-by-credits (generate-combinations real-courses real-prefs) real-courses real-prefs)))
     2)
  (t "integracion: rango 11..14 -> ninguna"
     (stream-empty? (filter-by-credits (generate-combinations real-courses real-prefs) real-courses (prefs 11 14)))
     #t)))
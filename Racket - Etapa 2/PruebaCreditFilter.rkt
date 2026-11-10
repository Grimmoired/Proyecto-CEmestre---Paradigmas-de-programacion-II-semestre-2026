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
  (course code code credits '() '() '() #f #t #f "Prueba"))
(define (comb . codes)  ;; (comb "A" "B") -> combination con grupo "1" de cada curso
  (combination (map (lambda (c) (selection c "1")) codes)))
 
(define courses (list (mk-course "A" 1) (mk-course "B" 2) (mk-course "C" 3) (mk-course "D" 4)))
(define (prefs lo hi) (preference lo hi '()))
(define (naturals-from n) (stream-cons n (naturals-from (add1 n))))
 
(define-values (real-courses real-prefs)
  (quiet (lambda ()
           (load-data (list "OutputCEPrueba.json" "OutputFIPrueba.json") "PreferenciasPrueba.rktd"))))
 
(run-tests
 (list
  ;; filter-stream (polimorfico, perezoso)
  (t "filter-stream: Enteros"
     (stream->list (filter-stream even? (in-list (range 10))))
     '(0 2 4 6 8))
  (t "filter-stream: strings"
     (stream->list (filter-stream (lambda (s) (char=? (string-ref s 0) #\C)) (stream "CE1101" "MA1403" "CA2125")))
     '("CE1101" "CA2125"))
  (t "filter-stream: Stream vacio, ningun elemento cumple condiciones, devuelve vacio"
     (list (stream-empty? (filter-stream even? empty-stream))
           (stream-empty? (filter-stream (lambda (x) #f) (stream 1 2 3))))
     '(#t #t))
  (t "filter-stream: Usa lazy para encontrar unicamente los 0primeros 3 pares de un stream infinito"
     (for/list ([x (in-stream (filter-stream even? (naturals-from 0)))] [i (in-range 3)]) x)
     '(0 2 4))
 
  ;; combination-credits
  (t "combination-credits: Suma de creditos (1+2+3) = 6"
     (combination-credits (comb "A" "B" "C") courses)
     6)
  (t "combination-credits: Combinacion vacia = 0"
     (combination-credits (combination '()) courses)
     0)
  (t "combination-credits: Procesar un curso fuera del catalogo da error"
     (raises? (lambda () (combination-credits (comb "A" "NOEXISTE") courses)))
     #t)
 
  ;; credits-in-range?
  (t "credits-in-range?: Aplica limites inclusivos"
     (list ((credits-in-range? 6 10 courses) (comb "A" "B" "C"))
           ((credits-in-range? 1 6 courses) (comb "A" "B" "C")))
     '(#t #t))
  (t "credits-in-range?: Fuera de rango por 1 credito"
     (list ((credits-in-range? 7 10 courses) (comb "A" "B" "C"))
           ((credits-in-range? 1 5 courses) (comb "A" "B" "C")))
     '(#f #f))
 
  ;; filter-by-credits
  (t "filter-by-credits: Deja solo las combinaciones dentro del rango (3..6)"
     (map (lambda (c) (combination-credits c courses))
          (stream->list (filter-by-credits (stream (comb "A") (comb "A" "B") (comb "A" "B" "C") (comb "D" "C" "B"))
                                           courses (prefs 3 6))))
     '(3 6))
  (t "filter-by-credits: Si el rango esta vacio devuelve un stream vacio"
     (quiet (lambda () (stream-empty? (filter-by-credits (stream (comb "A")) courses (prefs 10 1)))))
     #t)
 
  ;; integracion con el generador y datos reales
  (t "integracion: Uso de generador junto a filtro para producir 2 combinaciones validas"
     (length (stream->list (filter-by-credits (generate-combinations real-courses real-prefs) real-courses real-prefs)))
     2)
  (t "integracion: Rango de creditos sin combinaciones validas (ej: 11..14) devuelve un string vacio"
     (stream-empty? (filter-by-credits (generate-combinations real-courses real-prefs) real-courses (prefs 11 14)))
     #t)))
#lang racket/base

(require racket/list
         racket/stream
         "Types.rkt"
         "DataLoader.rkt"
         "combination-generator.rkt")

;; check : string? any/c any/c -> void
;; Imprime PASS o FAIL comparando el resultado con lo esperado.
(define (check label actual expected)
  (printf "[~a] ~a~a\n"
          (if (equal? actual expected) "PASS" "FAIL")
          label
          (if (equal? actual expected)
              ""
              (format "\n   esperado: ~a\n   obtenido: ~a" expected actual))))

;; combination->pairs : combination? -> (listof (list string? string?))
;; Forma corta para comparar: (("CE1101" "2") ("CE1104" "1") ...)
(define (combination->pairs c)
  (map (lambda (s) (list (selection-course-code s) (selection-group-id s)))
       (combination-selections c)))

;; Helpers para armar datos sinteticos
(define (blk day start end) (schedule-block day start end))
(define (grp id . blocks) (course-group id #f blocks))
(define (mk-course code credits . groups)
  (course code code credits '() '() groups #f #t #f "Prueba"))

(displayln "=== Prueba 1: choques entre bloques ===")
(check "mismo dia, traslape"
       (blocks-clash? (blk "MAR" "0730" "0920") (blk "MAR" "0900" "1050")) #t)
(check "mismo dia, uno termina cuando empieza el otro (no chocan)"
       (blocks-clash? (blk "MAR" "0730" "0920") (blk "MAR" "0920" "1110")) #f)
(check "distinto dia"
       (blocks-clash? (blk "MAR" "0730" "0920") (blk "JUE" "0730" "0920")) #f)
(check "un bloque contenido en otro"
       (blocks-clash? (blk "MAR" "0700" "1200") (blk "MAR" "0800" "0900")) #t)

(newline)
(displayln "=== Prueba 2: datos de prueba reales (CE + FI) ===")
(define-values (courses prefs)
  (load-data (list "OutputCEPrueba.json" "OutputFIPrueba.json") "PreferenciasPrueba.rktd"))
(define combos (stream->list (generate-combinations courses prefs)))
(for ([c combos]) (printf "  ~a\n" (combination->pairs c)))
(check "cantidad de combinaciones"  (length combos) 2)
(check "contenido de las combinaciones"
       (map combination->pairs combos)
       '((("CE1101" "2") ("CE1104" "1") ("MA1403" "1"))
         (("CE1101" "2") ("CE1104" "1") ("MA1403" "2"))))

(newline)
(displayln "=== Prueba 3: cursos de ambas carreras a la vez ===")
(define prefs-ambas
  (preference 10 14 '(("CE1101" "1" "2") ("CE1104" "1") ("CA2125" "1"))))
(define combos-ambas (stream->list (generate-combinations courses prefs-ambas)))
(check "CE1101 g2 + CE1104 g1 + CA2125 g1"
       (map combination->pairs combos-ambas)
       '((("CE1101" "2") ("CE1104" "1") ("CA2125" "1"))))

(newline)
(displayln "=== Prueba 4: no existe ninguna combinacion sin choque ===")
(define prefs-choque
  (preference 0 20 '(("CE1101" "1") ("CE1104" "1"))))
(check "CE1101 g1 y CE1104 g1 chocan -> stream vacio"
       (stream-empty? (generate-combinations courses prefs-choque)) #t)

(newline)
(displayln "=== Prueba 5: curso no habilitado y sin cursos de interes ===")
(define courses-con-bloqueado
  (append courses
          (list (course "XX0000" "Bloqueado" 3 '() '()
                        (list (grp "1" (blk "LUN" "0800" "0950")))
                        #f #f #f "Prueba"))))
(define prefs-bloqueado
  (preference 0 20 '(("CE1101" "2") ("XX0000" "1"))))
(check "el curso con canEnroll=false se omite"
       (map combination->pairs
            (stream->list (generate-combinations courses-con-bloqueado prefs-bloqueado)))
       '((("CE1101" "2"))))
(check "sin cursos de interes -> stream vacio"
       (stream-empty? (generate-combinations courses (preference 0 20 '()))) #t)

(newline)
(displayln "=== Prueba 6: conteo en un caso sintetico (3 cursos x 2 grupos, sin choques) ===")
(define sint-chico
  (list (mk-course "A" 3 (grp "1" (blk "LUN" "0800" "0950")) (grp "2" (blk "LUN" "1000" "1150")))
        (mk-course "B" 3 (grp "1" (blk "MAR" "0800" "0950")) (grp "2" (blk "MAR" "1000" "1150")))
        (mk-course "C" 3 (grp "1" (blk "MIE" "0800" "0950")) (grp "2" (blk "MIE" "1000" "1150")))))
(check "2 x 2 x 2 = 8 combinaciones"
       (length (stream->list
                (generate-combinations
                 sint-chico
                 (preference 0 20 '(("A" "1" "2") ("B" "1" "2") ("C" "1" "2"))))))
       8)

;; Caso grande: 10 cursos con 6 grupos cada uno, todos en dias distintos
;; (nunca chocan entre si). Hay 6^10 = 60,466,176 combinaciones.
(define (course-grande n)
  (define day (format "D~a" n))
  (apply mk-course
         (format "G~a" n) 3
         (for/list ([i (in-range 6)])
           (grp (number->string i)
                (blk day
                     (format "~a00" (+ 7 (* 2 i)))   ; 700, 900, 1100, ...
                     (format "~a50" (+ 7 (* 2 i))))))))
(define cursos-grandes (for/list ([n (in-range 10)]) (course-grande n)))
(define interes-grande
  (for/list ([n (in-range 10)])
    (cons (format "G~a" n) (for/list ([i (in-range 6)]) (number->string i)))))

(newline)
(displayln "=== Prueba 7: pereza (6^10 combinaciones, solo se piden 3) ===")
(define tres-primeras
  (for/list ([c (in-stream (generate-combinations cursos-grandes
                                                  (preference 0 100 interes-grande)))]
             [i (in-range 3)])
    (combination->pairs c)))
(check "se obtienen 3 combinaciones sin generar todas"
       (length tres-primeras) 3)
(check "primera combinacion = grupo 0 de cada curso"
       (first tres-primeras)
       (for/list ([n (in-range 10)]) (list (format "G~a" n) "0")))
(check "segunda combinacion cambia solo el ultimo curso"
       (last (second tres-primeras)) '("G9" "1"))

(newline)
(displayln "=== Prueba 8: poda temprana (los 2 primeros cursos SIEMPRE chocan) ===")
;; P y Q tienen un unico grupo y los dos estan el mismo dia a la misma hora.
;; Despues vienen los 10 cursos grandes. Sin poda habria que recorrer 6^10
;; ramas antes de descartar todo; con poda se descarta en el segundo curso.
(define cursos-poda
  (append (list (mk-course "P" 3 (grp "1" (blk "LUN" "0800" "0950")))
                (mk-course "Q" 3 (grp "1" (blk "LUN" "0900" "1050"))))
          cursos-grandes))
(define interes-poda
  (append '(("P" "1") ("Q" "1")) interes-grande))
(define-values (resultado-poda ms-cpu ms-real ms-gc)
  (time-apply (lambda ()
                (stream-empty? (generate-combinations cursos-poda
                                                      (preference 0 100 interes-poda))))
              '()))
(check "stream vacio" (first resultado-poda) #t)
(printf "   (tardo ~a ms; sin poda serian ~a ramas)\n" ms-real (expt 6 10))

(newline)
(displayln "=== Fin de las pruebas del Cuartil 2 ===")

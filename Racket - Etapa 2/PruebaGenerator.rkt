#lang racket/base

(require racket/list racket/stream "Types.rkt" "DataLoader.rkt" "CombinationGenerator.rkt")

(define (check label actual expected)
  (printf "[~a] ~a~a\n" (if (equal? actual expected) "PASS" "FAIL") label
          (if (equal? actual expected) "" (format "\n   esperado: ~a\n   obtenido: ~a" expected actual))))

(define (combination->pairs c)
  (map (lambda (s) (list (selection-course-code s) (selection-group-id s))) (combination-selections c)))
(define (blk day start end) (schedule-block day start end))
(define (grp id . blocks) (course-group id #f blocks))
(define (mk-course code credits . groups) (course code code credits '() '() groups #f #t #f "Prueba"))
(define (all-pairs courses prefs) (map combination->pairs (stream->list (generate-combinations courses prefs))))
(define (quiet thunk) (parameterize ([current-error-port (open-output-string)]) (thunk)))
(define (has-course? pairs code) (and (assoc code pairs) #t))

(define (oracle courses prefs)
  (define lists
    (for/list ([i (preference-interests prefs)])
      (define c (findf (lambda (c) (equal? (course-course-code c) (car i))) courses))
      (for/list ([g (course-groups c)] #:when (member (course-group-group-id g) (cdr i))) (cons (car i) g))))
  (define (subsets ls)
    (if (null? ls) '(())
        (for*/list ([opt (cons #f (first ls))] [r (subsets (rest ls))]) (if opt (cons opt r) r))))
  (define (clash? x y)
    (for*/or ([a (course-group-blocks (cdr x))] [b (course-group-blocks (cdr y))])
      (and (equal? (schedule-block-day a) (schedule-block-day b))
           (< (string->number (schedule-block-start-time a)) (string->number (schedule-block-end-time b)))
           (< (string->number (schedule-block-start-time b)) (string->number (schedule-block-end-time a))))))
  (for/list ([s (subsets lists)]
             #:unless (null? s)
             #:unless (for*/or ([x s] [y s] #:unless (eq? x y)) (clash? x y)))
    (map (lambda (cand) (list (car cand) (course-group-group-id (cdr cand)))) s)))
(define (same-set? a b) (equal? (sort (map ~s a) string<?) (sort (map ~s b) string<?)))
(define (~s x) (format "~s" x))

(displayln "Prueba 1: choques entre bloques")
(check "traslape" (blocks-clash? (blk "MAR" "0730" "0920") (blk "MAR" "0900" "1050")) #t)
(check "uno termina cuando empieza el otro (no chocan)"
       (blocks-clash? (blk "MAR" "0730" "0920") (blk "MAR" "0920" "1110")) #f)
(check "distinto dia" (blocks-clash? (blk "MAR" "0730" "0920") (blk "JUE" "0730" "0920")) #f)
(check "bloque contenido en otro" (blocks-clash? (blk "MAR" "0700" "1200") (blk "MAR" "0800" "0900")) #t)

(newline)
(displayln "Prueba 2: datos reales (CE1101 g1/g2, CE1104 g1, MA1403 g1/g2); CE1101 g1 choca con CE1104 g1")
(define-values (courses prefs)
  (load-data (list "OutputCEPrueba.json" "OutputFIPrueba.json") "PreferenciasPrueba.rktd"))
(define real (all-pairs courses prefs))
(for ([c real]) (printf "  ~a\n" c))
(check "cantidad: subconjuntos no vacios sin choques" (length real) 14)
(check "incluye la combinacion con los 3 cursos"
       (and (member '(("CE1101" "2") ("CE1104" "1") ("MA1403" "1")) real) #t) #t)
(check "incluye combinaciones de 2 cursos" (and (member '(("CE1101" "2") ("MA1403" "2")) real) #t) #t)
(check "incluye combinaciones de 1 solo curso"
       (list (and (member '(("CE1104" "1")) real) #t) (and (member '(("MA1403" "2")) real) #t)) '(#t #t))
(check "CE1101 g1 y CE1104 g1 nunca aparecen juntos"
       (for/or ([c real]) (and (member '("CE1101" "1") c) (member '("CE1104" "1") c) #t)) #f)
(check "no hay combinacion vacia" (for/or ([c real]) (null? c)) #f)
(check "coincide con el oraculo de fuerza bruta" (same-set? real (oracle courses prefs)) #t)

(newline)
(displayln "Prueba 3: cursos de ambas carreras a la vez")
(define prefs-ambas (preference 10 14 '(("CE1101" "1" "2") ("CE1104" "1") ("CA2125" "1"))))
(define ambas (all-pairs courses prefs-ambas))
(check "cantidad" (length ambas) 9)
(check "incluye CE1101 g2 + CE1104 g1 + CA2125 g1"
       (and (member '(("CE1101" "2") ("CE1104" "1") ("CA2125" "1")) ambas) #t) #t)
(check "incluye el curso de Fisica solo" (and (member '(("CA2125" "1")) ambas) #t) #t)
(check "coincide con el oraculo" (same-set? ambas (oracle courses prefs-ambas)) #t)

(newline)
(displayln "Prueba 4: dos cursos que chocan entre si")
(define prefs-choque (preference 0 20 '(("CE1101" "1") ("CE1104" "1"))))
(check "salen solo las combinaciones de 1 curso, nunca ambos"
       (all-pairs courses prefs-choque) '((("CE1101" "1")) (("CE1104" "1"))))

(newline)
(displayln "Prueba 5: curso no habilitado y sin cursos de interes")
(define courses-con-bloqueado
  (append courses (list (course "XX0000" "Bloqueado" 3 '() '() (list (grp "1" (blk "LUN" "0800" "0950"))) #f #f #f "Prueba"))))
(check "el curso con canEnroll=false se omite"
       (quiet (lambda () (all-pairs courses-con-bloqueado (preference 0 20 '(("CE1101" "2") ("XX0000" "1"))))))
       '((("CE1101" "2"))))
(check "sin cursos de interes -> stream vacio"
       (stream-empty? (generate-combinations courses (preference 0 20 '()))) #t)

(newline)
(displayln "Prueba 6: conteo sintetico (3 cursos x 2 grupos, sin choques)")
(define sint-chico
  (list (mk-course "A" 3 (grp "1" (blk "LUN" "0800" "0950")) (grp "2" (blk "LUN" "1000" "1150")))
        (mk-course "B" 3 (grp "1" (blk "MAR" "0800" "0950")) (grp "2" (blk "MAR" "1000" "1150")))
        (mk-course "C" 3 (grp "1" (blk "MIE" "0800" "0950")) (grp "2" (blk "MIE" "1000" "1150")))))
(check "(2+1) x (2+1) x (2+1) - 1 (vacia) = 26 combinaciones"
       (length (all-pairs sint-chico (preference 0 20 '(("A" "1" "2") ("B" "1" "2") ("C" "1" "2")))))
       26)

(define (course-grande n)
  (define day (format "D~a" n))
  (apply mk-course (format "G~a" n) 3
         (for/list ([i (in-range 6)])
           (grp (number->string i) (blk day (format "~a00" (+ 7 (* 2 i))) (format "~a50" (+ 7 (* 2 i))))))))
(define cursos-grandes (for/list ([n (in-range 10)]) (course-grande n)))
(define interes-grande
  (for/list ([n (in-range 10)]) (cons (format "G~a" n) (for/list ([i (in-range 6)]) (number->string i)))))

(newline)
(displayln "Prueba 7: laziness (7^10 - 1 combinaciones posibles, solo se piden 3)")
(define tres
  (for/list ([c (in-stream (generate-combinations cursos-grandes (preference 0 100 interes-grande)))] [i (in-range 3)])
    (combination->pairs c)))
(check "se obtienen 3 sin generar todas" (length tres) 3)
(check "primera = grupo 0 de cada curso" (first tres) (for/list ([n (in-range 10)]) (list (format "G~a" n) "0")))
(check "segunda cambia solo el ultimo curso" (last (second tres)) '("G9" "1"))

(newline)
(displayln "Prueba 8: poda. P y Q chocan siempre entre si")
(define P (mk-course "P" 3 (grp "1" (blk "LUN" "0800" "0950"))))
(define Q (mk-course "Q" 3 (grp "1" (blk "LUN" "0900" "1050"))))
(define chicos (list (mk-course "X" 3 (grp "1" (blk "MAR" "0800" "0950")) (grp "2" (blk "MAR" "1000" "1150")))
                     (mk-course "Y" 3 (grp "1" (blk "MIE" "0800" "0950")) (grp "2" (blk "MIE" "1000" "1150")))
                     (mk-course "Z" 3 (grp "1" (blk "JUE" "0800" "0950")) (grp "2" (blk "JUE" "1000" "1150")))))
(define cursos-poda (append (list P Q) chicos))
(define prefs-poda (preference 0 100 (append '(("P" "1") ("Q" "1")) '(("X" "1" "2") ("Y" "1" "2") ("Z" "1" "2")))))
(define poda (all-pairs cursos-poda prefs-poda))
(check "3 opciones para {P,Q} (ninguno, P, Q) x 27 de {X,Y,Z} - 1 = 80" (length poda) 80)
(check "P y Q nunca aparecen juntos"
       (for/or ([c poda]) (and (has-course? c "P") (has-course? c "Q"))) #f)
(check "coincide con el oraculo" (same-set? poda (oracle cursos-poda prefs-poda)) #t)
(define-values (r cpu ms gc)
  (time-apply (lambda ()
                (for/or ([c (in-stream (generate-combinations (append (list P Q) cursos-grandes)
                                                              (preference 0 100 (append '(("P" "1") ("Q" "1")) interes-grande))))]
                         [i (in-range 1000)])
                  (define p (combination->pairs c))
                  (and (has-course? p "P") (has-course? p "Q"))))
              '()))
(check "en las primeras 1000 de P+Q+10 cursos grandes P y Q no aparecen juntos" (first r) #f)
(printf "   (las primeras 1000 tardaron ~a ms)\n" ms)

(newline)
(displayln "Prueba 9: curso repetido en los intereses")
(check "se unen sus grupos: nunca 2 grupos del mismo curso"
       (quiet (lambda ()
                (all-pairs (list (mk-course "A" 3 (grp "1" (blk "LUN" "0800" "0950")) (grp "2" (blk "MAR" "0800" "0950"))))
                           (preference 0 9 '(("A" "1") ("A" "2"))))))
       '((("A" "1")) (("A" "2"))))

(newline)
(displayln "Fin de las pruebas.")
#lang racket/base

(require racket/contract
         racket/list
         racket/stream
         "Types.rkt"
)

(provide filter-stream
         combination-credits
         credits-in-range?
         filter-by-credits
)

;; Función polimórfica reutilizable
;; Filtra un stream de elementos de tipo A con cualquier predicado.
(define/contract (filter-stream pred s)
  (-> (-> any/c boolean?) stream? stream?)
  (cond
    [(stream-empty? s) empty-stream]

    [(pred (stream-first s))
     (stream-cons (stream-first s) (filter-stream pred (stream-rest s)))
    ]

    [else
     (filter-stream pred (stream-rest s))
    ]
  )
)

;; Créditos de una combinación
(define/contract (credits-of-selection sel courses)
   ;; Contrato
  (-> selection? (listof course?) exact-nonnegative-integer?)

   ;; Encuentra el curso que coincida con el seleccionado
  (define found (findf (lambda(c) (string=? (course-course-code c) (selection-course-code sel))) courses))

   ;; Si ese curso esta en la lista entonces devolvemos su cantidad de creditos
   ;; caso contrario se devolvera como si tuviera un valor de 0 creditos
  (if found
     (course-credits found)
     0
  )
)

(define/contract (combination-credits comb courses)
   ;; Contrato
  (-> combination? (listof course?) exact-nonnegative-integer?)

   ;; Genera una suma recursiva para obtener el total de creditos
   ;; de una combinacion
  (foldl + 0 (map (lambda(sel) (credits-of-selection sel courses)) (combination-selections comb)))
)

 ;; Predicado de rango (devuelve una función)
(define/contract (credits-in-range? lo hi courses)
   ;; Contrato
  (-> exact-nonnegative-integer? exact-nonnegative-integer? (listof course?) (-> combination? boolean?))

   ;; Obtenemos la funcion para saber si la cantidad total de
   ;; credito esta en el rango de la cantidad de creditos de
   ;; preferencia 
  (lambda (comb)
    (define total (combination-credits comb courses))
    (and (>= total lo) (<= total hi))
  )
)

 ;; Filtro final para el pipeline
(define/contract (filter-by-credits combos courses prefs)
   ;; Contrato
  (-> stream? (listof course?) preference? stream?)

   ;; Filtra la lista de combinaciones en base a si la suma de
   ;; sus creditos esta dentro del rango de preferencia devolviendo
   ;; un stream con las combinaciones validas
  (filter-stream
   (credits-in-range? (preference-credits-min prefs) (preference-credits-max prefs) courses)
   combos
  )
)

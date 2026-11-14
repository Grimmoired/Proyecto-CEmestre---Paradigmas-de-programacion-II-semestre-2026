max_percentage('Ingenieria en Computadores', 70).

max_percentage('Ingenieria Fisica', 70).

max_courses_per_area(1).

incompatible_pair('CI3203', 'PI2609').
incompatible_pair('CI1407', 'MA1102').

incompatible_pair('MI2101', 'QU1107').
incompatible_pair('CA2125', 'QU1107').

rule_order([balance, area, incompatibility]).
known_area('Matematicas').
known_area('Programacion').
known_area('Computadores').
known_area('Electronica').
known_area('Ciencias naturales').
known_area('Humanistica').
known_area('Ingenieria').
known_area('Cultural-Deportiva').

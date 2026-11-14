max_percentage('Ingenieria en Computadores', 100).
max_percentage('Ingenieria Fisica', 50).

max_courses_per_area(3).

incompatible_pair('CI1407', 'MA1102').
incompatible_pair('CE1103', 'MA1102').
incompatible_pair('MI2101', 'QU1107').
incompatible_pair('CA2125', 'QU1107').

rule_order([incompatibility, area, balance]).

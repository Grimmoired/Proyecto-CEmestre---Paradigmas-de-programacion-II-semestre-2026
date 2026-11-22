max_percentage('Ingenieria en Computadores', 70).
max_percentage('Ingenieria Fisica', 70).
max_courses_per_area(2).
incompatible_pair('CI3203', 'PI2609').
incompatible_pair('CI1407', 'MA1102').
incompatible_pair('MI2101', 'QU1107').
incompatible_pair('CA2125', 'QU1107').
rule_order([incompatibility, area, balance]).

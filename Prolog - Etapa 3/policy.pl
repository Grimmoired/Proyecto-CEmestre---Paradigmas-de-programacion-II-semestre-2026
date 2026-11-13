
max_percentage('Ingenieria en Computadores', 70). % Regla 1: Porcentaje maximo de creditos por carrera
max_percentage('Ingenieria Fisica', 70).

max_courses_per_area(2). %Regla 2: maximo de cursos por area (N unico)

%Regla 3: pares incompatibles

%Para computadores:
incompatible_pair('CI3203', 'PI2609').
incompatible_pair('CI1407', 'MA1102').

% Para Fisica:
incompatible_pair('MI2101', 'QU1107').
incompatible_pair('CA2125', 'QU1107').

% Orden de evaluacion y catalogo de areas
rule_order([balance, area, incompatibility]).
known_area('Matematicas').
known_area('Programacion').
known_area('Computadores').
known_area('Electronica').
known_area('Ciencias naturales').
known_area('Humanistica').
known_area('Ingenieria').
known_area('Cultural-Deportiva').

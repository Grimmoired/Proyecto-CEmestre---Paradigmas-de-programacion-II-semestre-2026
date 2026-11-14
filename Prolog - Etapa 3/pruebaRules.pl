:- encoding(utf8).
:- ['loader.pl'].
:- ['rules.pl'].

ce('Ingenieria en Computadores').
fi('Ingenieria Fisica').

c(Code, Credits, Career, Area, course(Code, '1', Credits, Career, Area, extra('', []))).

combo(limite_exacto, [A, B]) :-
    ce(CE), fi(FI),
    c('X1', 7, CE, 'Matematicas', A),
    c('X2', 3, FI, 'Programacion', B).
combo(un_credito_encima, [A, B]) :-
    ce(CE), fi(FI),
    c('X1', 8, CE, 'Matematicas', A),
    c('X2', 3, FI, 'Programacion', B).
combo(un_punto_encima, [A, B]) :-
    ce(CE), fi(FI),
    c('X1', 71, CE, 'Matematicas', A),
    c('X2', 29, FI, 'Programacion', B).
combo(una_carrera, [A, B]) :-
    ce(CE),
    c('X1', 4, CE, 'Matematicas', A),
    c('X2', 3, CE, 'Programacion', B).
combo(cero_creditos, [A]) :-
    ce(CE),
    c('SE1100', 0, CE, 'Cultural-Deportiva', A).
combo(carrera_sin_maximo, [A]) :-
    c('X1', 5, 'Otra Carrera', 'Matematicas', A).
combo(dos_misma_area, [A, B]) :-
    ce(CE), fi(FI),
    c('X1', 3, CE, 'Matematicas', A),
    c('X2', 3, FI, 'Matematicas', B).
combo(tres_misma_area, [A, B, C]) :-
    ce(CE), fi(FI),
    c('X1', 3, CE, 'Matematicas', A),
    c('X2', 3, FI, 'Matematicas', B),
    c('X3', 3, FI, 'Matematicas', C).
combo(tres_area_cero_creditos, [A, B, C]) :-
    ce(CE),
    c('X1', 0, CE, 'Cultural-Deportiva', A),
    c('X2', 0, CE, 'Cultural-Deportiva', B),
    c('X3', 0, CE, 'Cultural-Deportiva', C).
combo(par_orden_ab, [A, B]) :-
    ce(CE),
    c('CI3203', 2, CE, 'Humanistica', A),
    c('PI2609', 2, CE, 'Matematicas', B).
combo(par_orden_ba, [B, A]) :-
    ce(CE),
    c('CI3203', 2, CE, 'Humanistica', A),
    c('PI2609', 2, CE, 'Matematicas', B).
combo(par_nuevo, [A, B]) :-
    ce(CE),
    c('CE1103', 4, CE, 'Programacion', A),
    c('MA1102', 4, CE, 'Matematicas', B).
combo(varias, [A, B, C, D]) :-
    ce(CE),
    c('CI3203', 2, CE, 'Humanistica', A),
    c('PI2609', 2, CE, 'Matematicas', B),
    c('X1', 4, CE, 'Matematicas', C),
    c('X2', 4, CE, 'Matematicas', D).

viola(Rule, Name, Detail) :- combo(Name, Cs), violates(Rule, Cs, Detail).
cumple_regla(Rule, Name)  :- combo(Name, Cs), \+ violates(Rule, Cs, _).

test(default, 'balance: justo en el limite (70 %) no viola',
     cumple_regla(balance, limite_exacto)).
test(default, 'balance: un credito encima viola con el detalle correcto',
     ( ce(CE), viola(balance, un_credito_encima, balance(CE, 8, 11, 70)) )).
test(default, 'balance: 71 % viola (un punto encima)',
     viola(balance, un_punto_encima, _)).
test(default, 'balance: una sola carrera (100 %) viola con max 70',
     viola(balance, una_carrera, _)).
test(default, 'balance: total 0 creditos no viola ni da error',
     cumple_regla(balance, cero_creditos)).
test(default, 'balance: carrera sin maximo = sin limite',
     cumple_regla(balance, carrera_sin_maximo)).

test(default, 'area: 2 cursos de la misma area (N=2) no viola',
     cumple_regla(area, dos_misma_area)).
test(default, 'area: 3 cursos de la misma area viola con el detalle',
     viola(area, tres_misma_area, area('Matematicas', 3, 2))).
test(default, 'area: cuenta cursos, no creditos',
     viola(area, tres_area_cero_creditos, area('Cultural-Deportiva', 3, 2))).

test(default, 'incompatibilidad: se detecta en orden A,B',
     viola(incompatibility, par_orden_ab, incompatibility('CI3203', 'PI2609'))).
test(default, 'incompatibilidad: se detecta en orden B,A',
     viola(incompatibility, par_orden_ba, incompatibility('PI2609', 'CI3203'))).
test(default, 'incompatibilidad: el par se reporta una sola vez',
     ( combo(par_orden_ab, Cs),
       aggregate_all(count, violates(incompatibility, Cs, _), 1) )).
test(default, 'incompatibilidad: sin par incompatible no viola',
     cumple_regla(incompatibility, dos_misma_area)).

test(default, 'incompatible(+,+): verifica en los dos ordenes',
     ( incompatible('CI3203', 'PI2609'), incompatible('PI2609', 'CI3203') )).
test(default, 'incompatible(+,+): falla si no son par',
     \+ incompatible('CI3203', 'MA1102')).
test(default, 'incompatible(+,-): companeros de QU1107',
     ( findall(X, incompatible('QU1107', X), Xs), msort(Xs, ['CA2125', 'MI2101']) )).
test(default, 'incompatible(-,+): companeros de QU1107',
     ( findall(X, incompatible(X, 'QU1107'), Xs), msort(Xs, ['CA2125', 'MI2101']) )).
test(default, 'incompatible(-,-): 4 pares declarados salen 8 veces',
     aggregate_all(count, incompatible(_, _), 8)).
test(default, 'career_credits(+,+,-): creditos de una carrera',
     ( combo(limite_exacto, Cs), ce(CE), career_credits(Cs, CE, 7) )).
test(default, 'career_credits(+,-,-): enumera cada carrera una vez',
     ( combo(limite_exacto, Cs), ce(CE), fi(FI),
       findall(Ca-Cr, career_credits(Cs, Ca, Cr), L),
       msort(L, Ordenada), msort([CE-7, FI-3], Ordenada) )).
test(default, 'career_credits(+,-,+): que carrera tiene 3 creditos',
     ( combo(limite_exacto, Cs), fi(FI), findall(Ca, career_credits(Cs, Ca, 3), [FI]) )).
test(default, 'career_credits: falla si la carrera no esta',
     ( combo(una_carrera, Cs), fi(FI), \+ career_credits(Cs, FI, _) )).
test(default, 'area_count(+,-,-): enumera areas con su cantidad',
     ( combo(tres_misma_area, Cs), findall(A-N, area_count(Cs, A, N), ['Matematicas'-3]) )).

test(default, 'violates(-,+,-): enumera todas las violaciones',
     ( combo(varias, Cs), findall(R, violates(R, Cs, _), Rs),
       Rs == [balance, area, incompatibility] )).

test(cambiada, 'politica cambiada: una carrera al 100 % ya no viola (max 100)',
     cumple_regla(balance, una_carrera)).
test(cambiada, 'politica cambiada: FI 3/10 sigue cumpliendo con max 50',
     cumple_regla(balance, limite_exacto)).
test(cambiada, 'politica cambiada: 3 cursos de una area ya no violan (N=3)',
     cumple_regla(area, tres_misma_area)).
test(cambiada, 'politica cambiada: par quitado ya no se detecta',
     cumple_regla(incompatibility, par_orden_ab)).
test(cambiada, 'politica cambiada: par agregado si se detecta',
     viola(incompatibility, par_nuevo, incompatibility('CE1103', 'MA1102'))).

test(real, 'real: todas las combinaciones se evaluan sin error',
     forall(combination(_, Cs), findall(R-D, violates(R, Cs, D), _))).

test(real, 'real: 347 combinaciones cargadas',
     aggregate_all(count, combination(_, _), 347)).
test(real, 'real: 84 combinaciones violan balance',
     aggregate_all(count, ( combination(_, Cs), once(violates(balance, Cs, _)) ), 84)).
test(real, 'real: 0 violan area (ninguna tiene mas de 2 cursos de una area)',
     \+ ( combination(_, Cs), violates(area, Cs, _) )).
test(real, 'real: 305 combinaciones violan incompatibilidad',
     aggregate_all(count, ( combination(_, Cs), once(violates(incompatibility, Cs, _)) ), 305)).
test(real, 'real: se detecta el par CI3203-PI2609 (atomos del JSON unifican)',
     ( combination(_, Cs), violates(incompatibility, Cs, incompatibility(A, B)),
       msort([A, B], ['CI3203', 'PI2609']) )).
test(real, 'real: 42 combinaciones cumplen las tres reglas',
     aggregate_all(count, ( combination(_, Cs), \+ violates(_, Cs, _) ), 42)).
test(real_n1, 'real con N=1: ahora si hay violaciones de area',
     ( combination(_, Cs), violates(area, Cs, area(_, 2, 1)) )).

test(default, 'rules.pl no usa cortes, assert, retract ni globales',
     ( read_file_terms('rules.pl', Terms),
       \+ ( member(T, Terms), sub_term(S, T), forbidden(S) ) )).

forbidden(S) :- S == !.
forbidden(S) :- compound(S), functor(S, F, _),
                memberchk(F, [assert, asserta, assertz, retract, retractall,
                              nb_setval, b_setval]).

read_file_terms(File, Terms) :-
    setup_call_cleanup(open(File, read, S), read_all(S, Terms), close(S)).
read_all(S, Terms) :-
    read_term(S, T, []),
    ( T == end_of_file -> Terms = [] ; Terms = [T | Rest], read_all(S, Rest) ).

run_group(Policy, Failed) :-
    findall(Name, ( test(Policy, Name, Goal), \+ run_one(Name, Goal) ), Failed).

run_one(Name, Goal) :-
    ( catch(Goal, E, (print_message(error, E), fail))
    -> format('  [OK]   ~w~n', [Name])
    ;  format('[FAIL] ~w~n', [Name]), fail ).

main :-
    format('--- policy.pl ---~n'),
    load_policy('policy.pl'),
    run_group(default, F1),
    format('--- tests/policyCambiada.pl ---~n'),
    load_policy('tests/policyCambiada.pl'),
    run_group(cambiada, F2),
    format('--- datos reales: CombinacionesSalidaMixta.json + policy.pl ---~n'),
    load_input('CombinacionesSalidaMixta.json', 'policy.pl'),
    run_group(real, F3),
    format('--- datos reales + tests/policyAreaN1.pl ---~n'),
    load_policy('tests/policyAreaN1.pl'),
    run_group(real_n1, F4),
    append([F1, F2, F3, F4], Failed),
    length(Failed, NF),
    ( NF =:= 0 -> format('TODAS LAS PRUEBAS PASARON~n')
    ; format('HAY ~w FALLOS~n', [NF]), halt(1) ).

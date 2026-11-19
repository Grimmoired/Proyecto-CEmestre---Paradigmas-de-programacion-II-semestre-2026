:- encoding(utf8).
:- ['loader.pl'].
:- ['rules.pl'].
:- ['razon.pl'].

ce('Ingenieria en Computadores').
fi('Ingenieria Fisica').

c(Code, Credits, Career, Area, course(Code, '1', Credits, Career, Area, extra('', []))).

% Politica por defecto (policy.pl): max 70 %, N=2,
% pares CI3203-PI2609, CI1407-MA1102, MI2101-QU1107, CA2125-QU1107.

combo(ok, [A, B]) :-                       % 70 % justo: cumple todo
    ce(CE), fi(FI),
    c('X1', 7, CE, 'Matematicas', A),
    c('X2', 3, FI, 'Programacion', B).
combo(cero_creditos, [A]) :-               % total 0: no hay division
    ce(CE),
    c('SE1100', 0, CE, 'Cultural-Deportiva', A).
combo(solo_balance, [A, B]) :-             % 8/11 = 72 % > 70
    ce(CE), fi(FI),
    c('X1', 8, CE, 'Matematicas', A),
    c('X2', 3, FI, 'Programacion', B).
combo(solo_area, [A, B, C]) :-             % 3 cursos de Matematicas
    ce(CE), fi(FI),
    c('X1', 3, CE, 'Matematicas', A),
    c('X2', 3, FI, 'Matematicas', B),
    c('X3', 3, FI, 'Matematicas', C).
combo(solo_incomp, [A, B, C]) :-           % par CI3203-PI2609
    ce(CE), fi(FI),
    c('CI3203', 2, CE, 'Humanistica', A),
    c('PI2609', 2, FI, 'Matematicas', B),
    c('X1', 2, CE, 'Programacion', C).
combo(varias, [A, B, C]) :-                % balance (80 %) e incompatibilidad
    ce(CE), fi(FI),
    c('CI3203', 4, CE, 'Humanistica', A),
    c('PI2609', 4, CE, 'Matematicas', B),
    c('X1', 2, FI, 'Programacion', C).
combo(una_sola, [A]) :-                    % 100 % de una carrera: quitar deja vacio
    ce(CE),
    c('X1', 5, CE, 'Matematicas', A).

% utilidades de prueba
set_order(L) :- retractall(rule_order(_)), assertz(rule_order(L)).

with_order(New, Goal) :-
    rule_order(Old), set_order(New),
    (catch(Goal, E, (set_order(Old), throw(E)))
        -> set_order(Old)
        ; set_order(Old), fail
    ).

codes(Name, Codes) :-
    combo(Name, Cs),
    findall(Code, removable(Cs, course(Code, _, _, _, _, _)), Codes).

% pruebas con policy.pl por defecto
test(default, 'complies: combinacion valida cumple', ( combo(ok, Cs), complies(Cs))).

test(default, 'rejection_reason: falla si cumple', ( combo(ok, Cs), \+ rejection_reason(Cs, _))).

test(default, 'complies: total 0 creditos cumple', ( combo(cero_creditos, Cs), complies(Cs))).

test(default, 'razon: solo balance', ( combo(solo_balance, Cs), ce(CE), rejection_reason(Cs, reason(balance, balance(CE, 8, 11, 70))))).

test(default, 'razon: solo area', (combo(solo_area, Cs), rejection_reason(Cs, reason(area, area('Matematicas', 3, 2))))).

test(default, 'razon: solo incompatibilidad', (combo(solo_incomp, Cs), rejection_reason(Cs, reason(incompatibility, 
    incompatibility('CI3203', 'PI2609'))) )).

test(default, 'orden normal: varias violaciones reportan balance', ( combo(varias, Cs), rejection_reason(Cs, reason(balance, _)))).

test(default, 'orden cambiado: la misma combinacion reporta incompatibilidad', (combo(varias, Cs), with_order([incompatibility, area, balance],
    rejection_reason(Cs, reason(incompatibility, _))))).

test(default, 'el veredicto no depende del orden', (combo(varias, Cs), \+ complies(Cs),
    with_order([incompatibility, area, balance], \+ complies(Cs)))).

test(default, 'corte: exactamente UNA razon aunque se violen varias reglas', (combo(varias, Cs), aggregate_all(count, rejection_reason(Cs, _), 1))).

test(default, 'quitar: solo area, sirven X2 y X3 (X1 deja balance al 100 %)', codes(solo_area, ['X2', 'X3'])).

test(default, 'quitar: solo incompatibilidad, sirve solo CI3203', codes(solo_incomp, ['CI3203'])).

test(default, 'quitar: varias, sirven CI3203 y PI2609', codes(varias, ['CI3203', 'PI2609'])).

test(default, 'quitar: cero cuando quitar uno solo no alcanza', codes(solo_balance, [])).

test(default, 'quitar: nunca deja el resto vacio', codes(una_sola, [])).

test(default, 'evaluate: combinacion que cumple',
    (combo(ok, Cs), evaluate(combination(1, Cs), result(1, Cs, true, null, [])))).

test(default, 'evaluate: rechazada trae razon y cursos a quitar',
    (
        combo(varias, Cs),
        evaluate(combination(7, Cs), result(7, Cs, false, reason(balance, _),
        [removal('CI3203', '1'), removal('PI2609', '1')]))
    )).

test(default, 'evaluate: rechazada sin salida tiene lista vacia',
    (combo(solo_balance, Cs), evaluate(combination(3, Cs), result(3, Cs, false, _, [])))).

% restricciones del enunciado sobre razon.pl
test(default, 'razon.pl no usa assert, retract ni globales',
    (read_file_terms('razon.pl', Terms), \+ ( member(T, Terms), sub_term(S, T), forbidden(S)))).

test(default, 'razon.pl no duplica reglas ni valores de politica',
    (read_file_terms('razon.pl', Terms), \+ ( member(T, Terms), sub_term(S, T), duplicated_logic(S)))).

% pruebas con datos reales (JSON de Racket + policy.pl)
test(real, 'real: entran N y salen N resultados',
    (
        aggregate_all(count, combination(_, _), N),
        aggregate_all(count, ( combination(Id, Cs), evaluate(combination(Id, Cs), _) ), N)
    )).

test(real, 'real: cumplen + rechazadas = total',
    (
        aggregate_all(count, combination(_, _), N),
        aggregate_all(count, ( combination(_, Cs), complies(Cs) ), Ok),
        aggregate_all(count, ( combination(_, Cs), \+ complies(Cs) ), Bad),
        N =:= Ok + Bad
    )).

test(real, 'real: 42 combinaciones cumplen (igual que las pruebas de reglas)',
    aggregate_all(count, ( combination(_, Cs), complies(Cs) ), 42)).

test(real, 'real: complies coincide con "ninguna regla se viola"',
    forall(combination(_, Cs), (   complies(Cs) -> \+ violates(_, Cs, _) ; violates(_, Cs, _) ))).

test(real, 'real: cada rechazada reporta una razon y cada valida null',
    forall(combination(Id, Cs), (evaluate(combination(Id, Cs), result(Id, Cs, V, R, _)), (V == true -> R == null ; R = reason(_, _))))).

test(real, 'real: todo curso sugerido para quitar deja una combinacion valida',
    forall(( combination(_, Cs), removable(Cs, C) ), ( select(C, Cs, Rest), Rest \== [], complies(Rest)))).

forbidden(S) :- 
    compound(S), functor(S, F, _),
    memberchk(F, [assert, asserta, assertz, retract, retractall, nb_setval, b_setval]).

duplicated_logic(S) :- 
    compound(S), functor(S, F, _), 
    memberchk(F, [max_percentage, max_courses_per_area, incompatible_pair, incompatible]).

read_file_terms(File, Terms) :-
    setup_call_cleanup(open(File, read, S), read_all(S, Terms), close(S)).

read_all(S, Terms) :-
    read_term(S, T, []),
    (T == end_of_file -> Terms = [] ; Terms = [T | Rest], read_all(S, Rest)).

run_group(Policy, Failed) :-
    findall(Name, ( test(Policy, Name, Goal), \+ run_one(Name, Goal) ), Failed).

run_one(Name, Goal) :-
    (catch(Goal, E, (print_message(error, E), fail))
        -> format('  [OK]   ~w~n', [Name])
        ; format('[FAIL] ~w~n', [Name]), fail
    ).

main :-
    format('policy.pl ~n'),
    load_policy('policy.pl'),
    run_group(default, F1),
    format('datos reales: CombinacionesSalidaMixta.json + policy.pl ~n'),
    load_input('CombinacionesSalidaMixta.json', 'policy.pl'),
    run_group(real, F2),
    append(F1, F2, Failed),
    length(Failed, NF),
    (NF =:= 0 -> format('TODAS LAS PRUEBAS PASARON~n')
    ; format('HAY ~w FALLOS~n', [NF]), halt(1) ).
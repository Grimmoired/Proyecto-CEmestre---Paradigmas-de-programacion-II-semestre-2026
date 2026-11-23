:- encoding(utf8).
:- ['loader.pl'].
:- ['rules.pl'].
:- ['reasoning.pl'].

ce('Ingenieria en Computadores').
fi('Ingenieria Fisica').

c(Code, Credits, Career, Area, course(Code, '1', Credits, Career, Area, extra('', []))).

combo(ok, [A, B]) :- ce(CE), fi(FI), c('X1', 7, CE, 'Matematicas', A), c('X2', 3, FI, 'Programacion', B).
combo(cero_creditos, [A]) :- ce(CE), c('SE1100', 0, CE, 'Cultural-Deportiva', A).
combo(solo_balance, [A, B]) :- ce(CE), fi(FI), c('X1', 8, CE, 'Matematicas', A), c('X2', 3, FI, 'Programacion', B).
combo(solo_area, [A, B, C]) :- ce(CE), fi(FI), c('X1', 3, CE, 'Matematicas', A), c('X2', 3, FI, 'Matematicas', B), c('X3', 3, FI, 'Matematicas', C).
combo(solo_incomp, [A, B, C]) :- ce(CE), fi(FI), c('CI3203', 2, CE, 'Humanistica', A), c('PI2609', 2, FI, 'Matematicas', B), c('X1', 2, CE, 'Programacion', C).
combo(varias, [A, B, C]) :- ce(CE), fi(FI), c('CI3203', 4, CE, 'Humanistica', A), c('PI2609', 4, CE, 'Matematicas', B), c('X1', 2, FI, 'Programacion', C).
combo(una_sola, [A]) :- ce(CE), c('X1', 5, CE, 'Matematicas', A).

% Ejecuta Goal con otra politica cargada y siempre restaura policy.pl.
% No hay assert/retract aqui: load_policy/1 (loader.pl) es el unico que carga datos.
with_policy(PolicyFile, Goal) :-
    load_policy(PolicyFile),
    (   catch(Goal, E, ( load_policy('policy.pl'), throw(E) ))
    ->  load_policy('policy.pl')
    ;   load_policy('policy.pl'), fail
    ).

codes(Name, Codes) :-
    combo(Name, Cs),
    findall(Code, removable(Cs, course(Code, _, _, _, _, _)), Codes).

test(default, 'complies: combinacion valida cumple', ( combo(ok, Cs), complies(Cs))).
test(default, 'rejection_reason: falla si cumple', ( combo(ok, Cs), \+ rejection_reason(Cs, _))).
test(default, 'complies: total 0 creditos cumple', ( combo(cero_creditos, Cs), complies(Cs))).
test(default, 'razon: solo balance', ( combo(solo_balance, Cs), ce(CE), rejection_reason(Cs, reason(balance, balance(CE, 8, 11, 70))))).
test(default, 'razon: solo area', (combo(solo_area, Cs), rejection_reason(Cs, reason(area, area('Matematicas', 3, 2))))).
test(default, 'razon: solo incompatibilidad', (combo(solo_incomp, Cs), rejection_reason(Cs, reason(incompatibility, incompatibility('CI3203', 'PI2609'))))).
test(default, 'orden normal: varias violaciones reportan balance', ( combo(varias, Cs), rejection_reason(Cs, reason(balance, _)))).
test(default, 'orden cambiado: la misma combinacion reporta incompatibilidad',
     ( combo(varias, Cs),
       with_policy('tests/policyOrdenCambiado.pl', rejection_reason(Cs, reason(incompatibility, _))) )).
test(default, 'el veredicto no depende del orden',
     ( combo(varias, Cs), \+ complies(Cs),
       with_policy('tests/policyOrdenCambiado.pl', \+ complies(Cs)) )).
test(default, 'with_policy restaura el orden original',
     ( combo(varias, Cs),
       with_policy('tests/policyOrdenCambiado.pl', true),
       rejection_reason(Cs, reason(balance, _)) )).
test(default, 'razon pedida que no es la primera violada falla (corte)',
     ( combo(varias, Cs), \+ rejection_reason(Cs, reason(incompatibility, _)) )).
test(default, 'corte: exactamente UNA razon aunque se violen varias reglas', (combo(varias, Cs), aggregate_all(count, rejection_reason(Cs, _), 1))).
test(default, 'quitar: solo area, sirven X2 y X3 (X1 deja balance al 100 %)', codes(solo_area, ['X2', 'X3'])).
test(default, 'quitar: solo incompatibilidad, sirve solo CI3203', codes(solo_incomp, ['CI3203'])).
test(default, 'quitar: varias, sirven CI3203 y PI2609', codes(varias, ['CI3203', 'PI2609'])).
test(default, 'quitar: cero cuando quitar uno solo no alcanza', codes(solo_balance, [])).
test(default, 'quitar: nunca deja el resto vacio', codes(una_sola, [])).
test(default, 'evaluate: combinacion que cumple', (combo(ok, Cs), evaluate(combination(1, Cs), result(1, Cs, true, null, [])))).
test(default, 'evaluate: rechazada trae razon y cursos a quitar',
    ( combo(varias, Cs),
      evaluate(combination(7, Cs), result(7, Cs, false, reason(balance, _), [removal('CI3203', '1'), removal('PI2609', '1')])) )).
test(default, 'evaluate: rechazada sin salida tiene lista vacia', (combo(solo_balance, Cs), evaluate(combination(3, Cs), result(3, Cs, false, _, [])))).
test(default, 'reasoning.pl no usa assert, retract ni globales',
    (read_file_terms('reasoning.pl', Terms), \+ ( member(T, Terms), sub_term(S, T), forbidden(S)))).
test(default, 'reasoning.pl no duplica reglas ni valores de politica',
    (read_file_terms('reasoning.pl', Terms), \+ ( member(T, Terms), sub_term(S, T), duplicated_logic(S)))).
test(default, 'este archivo de pruebas tampoco usa assert, retract ni globales',
    (read_file_terms('pruebaRazon.pl', Terms), \+ ( member(T, Terms), T \= (test(_,_,_) :- _), T \= test(_,_,_), sub_term(S, T), forbidden(S)))).

forbidden(S) :- compound(S), functor(S, F, _),
    memberchk(F, [assert, asserta, assertz, retract, retractall, nb_setval, b_setval]).
duplicated_logic(S) :- compound(S), functor(S, F, _),
    memberchk(F, [max_percentage, max_courses_per_area, incompatible_pair, incompatible]).
read_file_terms(File, Terms) :- setup_call_cleanup(open(File, read, S), read_all(S, Terms), close(S)).
read_all(S, Terms) :- read_term(S, T, []), (T == end_of_file -> Terms = [] ; Terms = [T | Rest], read_all(S, Rest)).
run_group(Policy, Failed) :- findall(Name, ( test(Policy, Name, Goal), \+ run_one(Name, Goal) ), Failed).
run_one(Name, Goal) :-
    (catch(Goal, E, (print_message(error, E), fail)) -> format('  [OK]   ~w~n', [Name]) ; format('[FAIL] ~w~n', [Name]), fail).
main :-
    load_policy('policy.pl'), run_group(default, F1),
    length(F1, NF),
    (NF =:= 0 -> format('TODAS LAS PRUEBAS PASARON~n') ; format('HAY ~w FALLOS~n', [NF]), halt(1)).
% Uso: swipl -g main -t halt pruebaIntegracion.pl
:- encoding(utf8).
:- use_module(library(process)).
:- if(exists_source(library(json))).
:- use_module(library(json)).
:- else.
:- use_module(library(http/json)).
:- endif.

% run_main(+Args, -ExitCode)
run_main(Args, ExitCode) :-
    current_prolog_flag(executable, Swipl),
    process_create(Swipl, ['main.pl' | Args],
                   [stdout(null), stderr(null), process(Pid)]),
    process_wait(Pid, exit(ExitCode)).

% read_output(+File, -Dict)
read_output(File, Dict) :-
    setup_call_cleanup(open(File, read, S, [encoding(utf8)]),
                       json_read_dict(S, Dict, [value_string_as(atom)]),
                       close(S)).

% run_and_read(+Args, -Dict): corre main.pl, exige codigo 0 y lee la salida
run_and_read(Args, Dict) :-
    last(Args, Output),
    run_main(Args, 0),
    read_output(Output, Dict).

result_by_id(Dict, Id, Result) :-
    member(Result, Dict.results),
    Result.id =:= Id.

reason_rule(Dict, Id, Rule) :-
    result_by_id(Dict, Id, R),
    R.reason \== null,
    Rule = R.reason.rule.

count_rule(Dict, Rule, N) :-
    aggregate_all(count, ( member(R, Dict.results), has_rule(R, Rule) ), N).

% Va en un predicado aparte: dentro de aggregate_all, R.reason se evaluaria
% antes de que member/2 le diera valor a R.
has_rule(R, Rule) :-
    R.reason \== null,
    R.reason.rule == Rule.

out(Name, File) :- atomic_list_concat(['tests/salidas/', Name, '.json'], File).

% --- casos -------------------------------------------------------------
% Cada caso es un predicado propio (t_...). test/2 solo los lista, con su
% nombre. No se pueden escribir los objetivos directamente dentro de test/2
% porque la notacion D.campo se evalua al llamar el hecho, antes de que D
% tenga valor.

test('mixta: entran 347 y salen 347, con ids del 1 al 347', t_mixta_cantidad).
test('mixta: 42 cumplen, 221 incompatibilidad, 84 balance', t_mixta_conteos).
test('valida: la combinacion 73 cumple, sin razon ni cursos a quitar', t_valida).
test('cada rechazada trae regla y mensaje; cada valida trae null', t_forma_razon).
test('incompatibilidad: la combinacion 1 reporta el par PI2609-CI3203', t_incompatibilidad).
test('balance: un plan de una sola carrera supera el 70 %', t_balance).
test('area: con N = 1 aparecen rechazos por area', t_area).
test('varias violaciones: la 10 reporta balance con el orden normal', t_varias_normal).
test('varias violaciones: la 10 reporta incompatibilidad con el orden cambiado', t_varias_cambiado).
test('cambiar el orden no cambia cuantas cumplen', t_orden_veredicto).
test('removable: todo curso sugerido pertenece a la combinacion', t_removable).
test('politica cambiada: la salida refleja los nuevos valores', t_politica_cambiada).
test('vacio: codigo 0 y lista de resultados vacia', t_vacio).
test('curso sin area: codigo 1 y no escribe salida', t_sin_area).
test('JSON roto: codigo 1', t_json_roto).
test('archivo inexistente: codigo 1', t_inexistente).

t_mixta_cantidad :-
    out(mixta, F),
    run_and_read(['CombinacionesSalidaMixta.json', 'policy.pl', F], D),
    D.combinationCount =:= 347,
    length(D.results, 347),
    findall(Id, ( member(R, D.results), Id = R.id ), Ids),
    msort(Ids, Sorted), numlist(1, 347, Sorted).

t_mixta_conteos :-
    out(mixta, F), read_output(F, D),
    D.compliantCount =:= 42,
    count_rule(D, incompatibility, 221),
    count_rule(D, balance, 84).

t_valida :-
    out(mixta, F), read_output(F, D),
    result_by_id(D, 73, R),
    R.complies == true, R.reason == null, R.removable == [].

t_forma_razon :-
    out(mixta, F), read_output(F, D),
    forall(member(R, D.results),
           (   R.complies == true
           ->  R.reason == null
           ;   atom(R.reason.rule), atom(R.reason.message) )).

t_incompatibilidad :-
    out(mixta, F), read_output(F, D),
    result_by_id(D, 1, R),
    R.complies == false,
    R.reason.rule == incompatibility,
    msort([R.reason.courseA, R.reason.courseB], ['CI3203', 'PI2609']).

t_balance :-
    out(ce, F),
    run_and_read(['CombinacionesSalidaCE.json', 'policy.pl', F], D),
    D.results = [_|_],
    forall(member(R, D.results), R.reason.rule == balance).

t_area :-
    out(area_n1, F),
    run_and_read(['CombinacionesSalidaMixta.json', 'tests/policyAreaN1.pl', F], D),
    D.policy.maxCoursesPerArea =:= 1,
    count_rule(D, area, N), N > 0.

t_varias_normal :-
    out(mixta, F), read_output(F, D),
    reason_rule(D, 10, balance).

t_varias_cambiado :-
    out(orden, F),
    run_and_read(['CombinacionesSalidaMixta.json', 'tests/policyOrdenCambiado.pl', F], D),
    D.policy.ruleOrder == [incompatibility, area, balance],
    reason_rule(D, 10, incompatibility).

t_orden_veredicto :-
    out(orden, F), read_output(F, D),
    D.compliantCount =:= 42.

t_removable :-
    out(mixta, F), read_output(F, D),
    forall(( member(R, D.results), member(Q, R.removable) ),
           ( member(S, R.selections),
             S.courseCode == Q.courseCode, S.groupId == Q.groupId )).

t_politica_cambiada :-
    out(cambiada, F),
    run_and_read(['CombinacionesSalidaMixta.json', 'tests/policyCambiada.pl', F], D),
    D.policy.maxCoursesPerArea =:= 3,
    D.compliantCount =\= 42.

t_vacio :-
    out(vacio, F),
    run_and_read(['tests/pruebaEspaciosVacios.json', 'policy.pl', F], D),
    D.combinationCount =:= 0, D.results == [].

t_sin_area :-
    out(sin_area, F),
    ( exists_file(F) -> delete_file(F) ; true ),
    run_main(['tests/pruebaSinArea.json', 'policy.pl', F], 1),
    \+ exists_file(F).

t_json_roto :-
    run_main(['tests/pruebaRota.json', 'policy.pl', 'tests/salidas/rota.json'], 1).

t_inexistente :-
    run_main(['noExiste.json', 'policy.pl', 'tests/salidas/x.json'], 1).

% --- ejecucion ---------------------------------------------------------

run_one(Name, Goal) :-
    (   catch(Goal, E, (print_message(error, E), fail))
    ->  format('  [OK]   ~w~n', [Name])
    ;   format('[FAIL] ~w~n', [Name]), fail
    ).

main :-
    make_directory_path('tests/salidas'),
    findall(Name, ( test(Name, Goal), \+ run_one(Name, Goal) ), Failed),
    length(Failed, NF),
    (   NF =:= 0
    ->  format('TODAS LAS PRUEBAS PASARON~n')
    ;   format('HAY ~w FALLOS~n', [NF]), halt(1)
    ).

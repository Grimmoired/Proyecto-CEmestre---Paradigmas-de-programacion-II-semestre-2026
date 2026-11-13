% swipl -q -g main -t halt test_loader.pl
:- encoding(utf8).
:- ['loader.pl'].

check(Name, Goal) :-
    (catch(Goal, _, fail) -> format('  [OK]   ~w~n', [Name]);
    format('[FAIL] ~w~n', [Name]), nb_setval(failures, 1)).

expect_error(Name, Goal, Part) :-
    catch((call(Goal), M0 = none ), loader_error(M0), true), Message = M0,
    (sub_atom(Message, _, _, _, Part) ->  format('  [OK]   ~w~n', [Name]);
    format('[FAIL] ~w (mensaje: ~q)~n', [Name, Message]), nb_setval(failures, 1)).

main :-
    nb_setval(failures, 0),
    load_input('tests/prueba.json', 'policy.pl'),
    check('entran 3, quedan 3 hechos', aggregate_all(count, combination(_, _), 3)),
    check('credits entero y groupId atomo',
          combination(1, [course('CI3203', '1', 2, _, _, _)|_])),
    check('carrera cargada unifica con max_percentage',
          (combination(1, Cs), forall(member(course(_,_,_,Ca,_,_), Cs), max_percentage(Ca, _)) )),
    check('area con espacio unifica con known_area',
     (combination(1, Cs2), member(course('QU1107',_,_,_,A,_), Cs2), known_area(A) )),
    check('hora conserva cero inicial',
          combination(1, [course(_,_,_,_,_,extra(_, [block('LUN','0730','1120')]))|_])),
    check('curso de 0 creditos se carga', combination(2, [course('SE1100',_,0,_,_,_)])),
    check('par incompatible declarado', incompatible_pair('CI3203', 'PI2609')),
    expect_error('curso sin area', load_combinations('tests/pruebaSinArea.json'), 'area'),
    expect_error('conteo distinto', load_combinations('tests/pruebaMalConteo.json'), 'combinationCount'),
    expect_error('JSON truncado', load_combinations('tests/pruebaRota.json'), 'JSON'),
    expect_error('archivo inexistente', load_combinations('tests/pruebaInexsitente.json'), 'no existe'),
    check('vacio: 0 hechos y sin error',
          ( load_combinations('tests/pruebaEspaciosVacios.json'), \+ combination(_, _) )),
    nb_getval(failures, F),
    (F =:= 0 -> format('TODAS LAS PRUEBAS PASARON~n') ; format('HAY FALLOS~n'), halt(1) ).

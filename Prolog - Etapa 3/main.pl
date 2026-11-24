:- encoding(utf8).
:- initialization(main, main).

:- ['loader.pl', 'rules.pl', 'reasoning.pl', 'export.pl'].

main :-
    current_prolog_flag(argv, Args),
    arg_or_default(Args, 1, 'CombinacionesSalidaMixta.json', CombinationsFile),
    arg_or_default(Args, 2, 'policy.pl', PolicyFile),
    arg_or_default(Args, 3, 'ResultadosProlog.json', OutputFile),
    load_input_or_exit(CombinationsFile, PolicyFile),
    findall(Result,
            ( combination(Id, Courses),
              evaluate(combination(Id, Courses), Result) ),
            Results),
    check_all_evaluated(Results),
    export_or_exit(OutputFile, Results),
    length(Results, N),
    format('Exportacion completada: ~w (~w combinaciones)~n', [OutputFile, N]).

arg_or_default(Args, Position, Default, Value) :-
    (   nth1(Position, Args, Given)
    ->  Value = Given
    ;   Value = Default
    ).

check_all_evaluated(Results) :-
    aggregate_all(count, combination(_, _), Expected),
    length(Results, Obtained),
    (   Obtained =:= Expected
    ->  true
    ;   format(user_error, 'ERROR: se cargaron ~w combinaciones pero se evaluaron ~w~n',
               [Expected, Obtained]),
        halt(1)
    ).

export_or_exit(File, Results) :-
    (   catch(export_results(File, Results), Error,
              ( print_message(error, Error), halt(1) ))
    ->  true
    ;   format(user_error, 'ERROR: algun resultado no se pudo convertir al formato de salida~n', []),
        halt(1)
    ).
:- encoding(utf8).
 
:- if(exists_source(library(json))).
:- use_module(library(json)).
:- else.
:- use_module(library(http/json)).   % Para versiones de prolog anteriores a la 9.3
:- endif.
 
:- dynamic combination/2.
:- dynamic max_percentage/2, max_courses_per_area/1, incompatible_pair/2,
           rule_order/1, known_area/1.

load_input(JsonFile, PolicyFile) :-
    load_policy(PolicyFile),
    load_combinations(JsonFile).
 
load_input_or_exit(JsonFile, PolicyFile) :-
    catch(load_input(JsonFile, PolicyFile),
          loader_error(Message),
          ( format(user_error, 'ERROR: ~w~n', [Message]), halt(1) )).
 
load_combinations(File) :-
    read_combinations(File, _Meta, Combinations),
    retractall(combination(_, _)),
    forall(member(C, Combinations), assertz(C)),
    warn_careers_without_max.
 
load_policy(File) :-
    read_policy(File, Terms),
    retractall(max_percentage(_, _)), retractall(max_courses_per_area(_)),
    retractall(incompatible_pair(_, _)), retractall(rule_order(_)),
    retractall(known_area(_)),
    forall(member(T, Terms), assertz(T)),
    validate_policy.
 
read_policy(File, Terms) :-
    check_file(File),
    setup_call_cleanup(open(File, read, S, [encoding(utf8)]),
             catch(read_terms(S, Terms0), error(syntax_error(E), _),
                    fail_with('sintaxis invalida en ~w: ~w', [File, E])),
             close(S)),
    exclude(is_directive, Terms0, Terms),
    forall(member(T, Terms), valid_policy_term(T, File)).
 
read_terms(S, Terms) :-
    read_term(S, T, []),
    (T == end_of_file -> Terms = [];  Terms = [T|Rest], read_terms(S, Rest)).
 
is_directive((:- _)).
 
valid_policy_term(max_percentage(C, P), _) :- atom(C), number(P), P >= 0, !.
valid_policy_term(max_courses_per_area(N), _) :- integer(N), N >= 0, !.
valid_policy_term(incompatible_pair(A, B), _) :- atom(A), atom(B), !.
valid_policy_term(rule_order(L), _) :- is_list(L), !.
valid_policy_term(known_area(A), _) :- atom(A), !.
valid_policy_term(T, File) :-
    fail_with('termino no valido en ~w: ~q', [File, T]).
 
validate_policy :-
    findall(N, max_courses_per_area(N), Ns),
    (  Ns = [_] -> true; fail_with('politica: debe haber exactamente un max_courses_per_area/1', [])),
    findall(L, rule_order(L), Ls),
    (Ls = [Order] -> true ; fail_with('politica: debe haber exactamente un rule_order/1', []) ),
    Known = [balance, area, incompatibility],
    (forall(member(R, Order), memberchk(R, Known)),
        sort(Order, Unique), length(Unique, K), length(Order, K) ->  true;
    fail_with('politica: rule_order debe usar reglas de ~w sin repetir', [Known])).
 
read_combinations(File, meta(CreditsMin, CreditsMax, Count), Combinations) :-
    check_file(File),
    setup_call_cleanup(open(File, read, S, [encoding(utf8)]),
        catch(json_read_dict(S, Dict, [value_string_as(atom)]),
           error(syntax_error(E), _),
              fail_with('JSON invalido en ~w: ~w', [File, E])),
           close(S)),
    (is_dict(Dict) -> true ; fail_with('el JSON raiz debe ser un objeto', []) ),
    (get_dict(combinations, Dict, List), is_list(List) -> true;
    fail_with('falta la lista "combinations" en ~w', [File])),
    maplist(convert_combination, List, Combinations),
    length(Combinations, Count),
    check_count(Dict, Count),
    check_ids(Combinations),
    (get_dict(creditsMin, Dict, CreditsMin) -> true ; CreditsMin = unknown ),
    (get_dict(creditsMax, Dict, CreditsMax) -> true ; CreditsMax = unknown ),
    (Count =:= 0 -> format(user_error, 'ADVERTENCIA: ~w no contiene combinaciones~n', [File]); true).
 
check_count(Dict, Count) :-
    (get_dict(combinationCount, Dict, Expected) -> ( Expected =:= Count -> true;
          fail_with('combinationCount=~w pero hay ~w combinaciones (archivo truncado?)',
             [Expected, Count])); format(user_error, 'ADVERTENCIA: falta combinationCount~n', [])).
 
check_ids(Combinations) :-
    findall(Id, member(combination(Id, _), Combinations), Ids),
    sort(Ids, Unique), length(Ids, N), length(Unique, K),
    (N =:= K -> true ; fail_with('hay ids de combinacion repetidos', []) ).
 
convert_combination(D, combination(Id, Courses)) :-
    (is_dict(D) -> true ; fail_with('una combinacion no es un objeto JSON', []) ),
    field(no_id, D, id, Id),
    (integer(Id) -> true ; fail_with('combinacion con id no entero: ~q', [Id]) ),
    field(Id, D, selections, Selections),
    (is_list(Selections) -> true ; fail_with('combinacion ~w: selections no es lista', [Id]) ),
    maplist(convert_course(Id), Selections, Courses),
    check_total(Id, D, Courses).
 
convert_course(Id, D, course(Code, GroupId, Credits, Career, Area, extra(Name, Blocks))) :-
    (is_dict(D) -> true ; fail_with('combinacion ~w: un curso no es objeto', [Id]) ),
    field(Id, D, courseCode, Code),
    field(Id, D, groupId, GroupId),
    field(Id, D, credits, Credits),
    field(Id, D, career, Career),
    field(Id, D, area, Area),
    require_text(Id, Code, courseCode),
    require_text(Id, GroupId, groupId),
    require_text(Id, Career, career),
    (atom(Area), Area \== '' ->  true;
    fail_with('combinacion ~w, curso ~w: area vacia o invalida (~q)', [Id, Code, Area])),
    (integer(Credits), Credits >= 0 -> true;
    fail_with('combinacion ~w, curso ~w: credits invalido (~q)', [Id, Code, Credits])),
    warn_area(Area),
    (get_dict(courseName, D, Name) -> true ; Name = '' ),
    (get_dict(blocks, D, Bs), is_list(Bs) -> maplist(convert_block(Id, Code), Bs, Blocks);
    Blocks = []).
 
require_text(Id, V, FieldName) :-
    (atom(V), V \== '' -> true;  fail_with('combinacion ~w: ~w debe ser texto no vacio (llego ~q)', [Id, FieldName, V])).
 
convert_block(Id, Code, B, block(Day, Start, End)) :-
    field(Id, B, day, Day),
    field(Id, B, startTime, Start),
    field(Id, B, endTime, End),
    forall(member(X, [Day, Start, End]),
           (atom(X) -> true;
           fail_with('combinacion ~w, curso ~w: bloque con valor no textual ~q', [Id, Code, X]))).
 
field(Context, D, Key, Value) :-
    (get_dict(Key, D, V) -> Value = V;
    fail_with('combinacion ~w: falta el campo "~w"', [Context, Key])).
 
check_total(Id, D, Courses) :-
    (get_dict(totalCredits, D, Total) -> findall(C, member(course(_, _, C, _, _, _), Courses), Cs),
        sum_list(Cs, Sum),
        (Sum =:= Total -> true;
        format(user_error,'ADVERTENCIA: combinacion ~w: totalCredits=~w pero los cursos suman ~w~n', [Id, Total, Sum])); true).
 
 
warn_area(Area) :-
    (\+ known_area(_) -> true; known_area(Area) -> true;
    format(user_error, 'ADVERTENCIA: area fuera del catalogo cerrado: ~q~n', [Area])).
 
warn_careers_without_max :-
    findall(Career, ( combination(_, Cs), member(course(_,_,_,Career,_,_), Cs) ), All),
    sort(All, Careers),
    forall(( member(Career, Careers), \+ max_percentage(Career, _) ),
           format(user_error,
                  'ADVERTENCIA: carrera ~q sin max_percentage/2 (se asume sin limite)~n',
                  [Career])).
 
check_file(File) :-
    (   exists_file(File) -> true ; fail_with('no existe el archivo ~w', [File]) ).
 
fail_with(Format, Args) :-
    format(atom(Message), Format, Args),
    throw(loader_error(Message)).






















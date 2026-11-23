:- encoding(utf8).

:- if(exists_source(library(json))).
:- use_module(library(json)).
:- else.
:- use_module(library(http/json)).
:- endif.

block_json(block(Day, Start, End),
           _{day: Day, startTime: Start, endTime: End}).

course_json(course(Code, Group, Credits, Career, Area, extra(Name, Blocks)),
            _{courseCode: Code, groupId: Group, credits: Credits, career: Career,
              area: Area, courseName: Name, blocks: BlocksJson}) :-
    maplist(block_json, Blocks, BlocksJson).

removal_json(removal(Code, Group), _{courseCode: Code, groupId: Group}).

reason_json(null, null).
reason_json(reason(balance, balance(Career, CareerCredits, Total, Max)),
            _{rule: balance, career: Career, careerCredits: CareerCredits,
              totalCredits: Total, maxPercentage: Max, message: Message}) :-
    format(atom(Message), '~w tiene ~w de ~w creditos (maximo ~w %)',
           [Career, CareerCredits, Total, Max]).
reason_json(reason(area, area(Area, Count, Max)),
            _{rule: area, area: Area, count: Count, maxCourses: Max, message: Message}) :-
    format(atom(Message), 'Hay ~w cursos del area ~w (maximo ~w)', [Count, Area, Max]).
reason_json(reason(incompatibility, incompatibility(A, B)),
            _{rule: incompatibility, courseA: A, courseB: B, message: Message}) :-
    format(atom(Message), 'Los cursos ~w y ~w son incompatibles', [A, B]).

policy_json(_{maxPercentages: Percentages, maxCoursesPerArea: N,
              incompatiblePairs: Pairs, ruleOrder: Order}) :-
    findall(_{career: C, maxPercentage: P}, max_percentage(C, P), Percentages),
    max_courses_per_area(N),
    findall(_{courseA: A, courseB: B}, incompatible_pair(A, B), Pairs),
    rule_order(Order).

result_json(result(Id, Courses, Complies, Reason, Removable),
            _{id: Id, complies: Complies, totalCredits: Total,
              selections: Selections, reason: ReasonJson,
              removable: RemovableJson}) :-
    total_credits(Courses, Total),
    maplist(course_json, Courses, Selections),
    reason_json(Reason, ReasonJson),
    maplist(removal_json, Removable, RemovableJson).

export_results(File, Results) :-
    policy_json(Policy),
    maplist(result_json, Results, ResultsJson),
    length(Results, Count),
    include(complies_result, Results, Compliant),
    length(Compliant, CompliantCount),
    setup_call_cleanup(
        open(File, write, Stream, [encoding(utf8)]),
        json_write_dict(Stream, _{policy: Policy,
                                  combinationCount: Count,
                                  compliantCount: CompliantCount,
                                  results: ResultsJson}),
        close(Stream)).

complies_result(result(_, _, true, _, _)).
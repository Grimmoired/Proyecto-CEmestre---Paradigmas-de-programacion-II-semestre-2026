:- encoding(utf8).

% rejection_reason(+Courses, -Reason)
% Falla si la combinacion no viola ninguna regla
rejection_reason(Courses, Reason) :-
    rule_order(Order),
    first_violation(Order, Courses, Reason).

 % busca la primera regla que es violada
 % y el detaller de la violacion
first_violation([Rule|_], Courses, reason(Rule, Detail)) :-
    violates(Rule, Courses, Detail),
    !.
first_violation([_|Rules], Courses, Reason) :-
    first_violation(Rules, Courses, Reason).

% complies(+Courses)
% cumple si NO existe razon de rechazo
complies(Courses) :-
    \+ rejection_reason(Courses, _).

% removable(+Courses, -Course)
% Course es un curso que, al quitarlo, hace que el resto cumpla todas
% las reglas y reutiliza complies/1 para que no se duplica ninguna regla
removable(Courses, Course) :-
    select(Course, Courses, Rest),
    Rest \== [],
    complies(Rest).



% evaluate(+Combination, -Result)
evaluate(combination(Id, Courses), result(Id, Courses, Complies, Reason, Removable)) :-
    (rejection_reason(Courses, Why)
        -> Complies = false, Reason = Why, findall(removal(Code, Group), removable(Courses, course(Code, Group, _, _, _, _)), Removable)
        ; Complies = true, Reason = null, Removable = []
    ).
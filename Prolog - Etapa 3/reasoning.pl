:- encoding(utf8).
 
rejection_reason(Courses, Reason) :-
    rule_order(Order),
    first_violation(Order, Courses, Reason).
first_violation([Rule|_], Courses, Reason) :-
    violates(Rule, Courses, Detail),
    !,
    Reason = reason(Rule, Detail).
first_violation([_|Rules], Courses, Reason) :-
    first_violation(Rules, Courses, Reason).
complies(Courses) :- \+ rejection_reason(Courses, _).
removable(Courses, Course) :-
    select(Course, Courses, Rest),
    Rest \== [],
    complies(Rest).
evaluate(combination(Id, Courses), result(Id, Courses, Complies, Reason, Removable)) :-
    (rejection_reason(Courses, Why)
        -> Complies = false, Reason = Why, findall(removal(Code, Group), removable(Courses, course(Code, Group, _, _, _, _)), Removable)
        ; Complies = true, Reason = null, Removable = []).

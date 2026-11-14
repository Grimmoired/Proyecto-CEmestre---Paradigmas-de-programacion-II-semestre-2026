:- encoding(utf8).

violates(balance, Courses, Detail) :-
    balance_violation(Courses, Detail).
violates(area, Courses, Detail) :-
    area_violation(Courses, Detail).
violates(incompatibility, Courses, Detail) :-
    incompatibility_violation(Courses, Detail).

balance_violation(Courses, balance(Career, CareerCredits, Total, Max)) :-
    total_credits(Courses, Total),
    career_credits(Courses, Career, CareerCredits),
    max_percentage(Career, Max),
    CareerCredits * 100 > Max * Total.

total_credits(Courses, Total) :-
    findall(Credits, member(course(_, _, Credits, _, _, _), Courses), CreditList),
    sum_list(CreditList, Total).

career_credits(Courses, Career, CareerCredits) :-
    careers_in(Courses, Careers),
    member(Career, Careers),
    findall(Credits,
            member(course(_, _, Credits, Career, _, _), Courses),
            CreditList),
    sum_list(CreditList, CareerCredits).

careers_in(Courses, Careers) :-
    findall(Career, member(course(_, _, _, Career, _, _), Courses), All),
    sort(All, Careers).

area_violation(Courses, area(Area, Count, Max)) :-
    max_courses_per_area(Max),
    area_count(Courses, Area, Count),
    Count > Max.

area_count(Courses, Area, Count) :-
    areas_in(Courses, Areas),
    member(Area, Areas),
    findall(Code, member(course(Code, _, _, _, Area, _), Courses), Codes),
    length(Codes, Count).

areas_in(Courses, Areas) :-
    findall(Area, member(course(_, _, _, _, Area, _), Courses), All),
    sort(All, Areas).

incompatible(A, B) :-
    incompatible_pair(A, B).
incompatible(A, B) :-
    incompatible_pair(B, A).

incompatibility_violation(Courses, incompatibility(CodeA, CodeB)) :-
    course_pair(Courses, course(CodeA, _, _, _, _, _), course(CodeB, _, _, _, _, _)),
    incompatible(CodeA, CodeB).

course_pair(Courses, First, Second) :-
    append(_, [First | Rest], Courses),
    member(Second, Rest).

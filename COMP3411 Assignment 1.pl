%Jeremy Chen, z5016815, Assignment 1

%question1

%This predicate iterates through the array recursively, if it encounters a number below or equal to 0, it squares it and adds it to the sum,
%but if it encounters a positive number, it is not square and added to the sum. 

sumsq_neg(Numbers, Sum) :-
    Numbers = [Head | Tail],
    sumsq_neg(Tail, X),
    Head > 0,
    Sum is X.
        
sumsq_neg(Numbers, Sum) :-
    Numbers = [Head | Tail],
    sumsq_neg(Tail, X),
    Head =< 0,
	Squared is *(Head,Head),
    Sum is +(X,Squared).
	
sumsq_neg(Numbers, Sum) :-
    Numbers = [],
    Sum is 0.
    
%question2

%I used 2 predicates here, all_like_all and one_like_all, basically it works similar to a nested loop, first it calls one_like_all to check
%if one person likes everything, then it calls all_like_all to repeat one_like_all for every member in the Who_List. If all_like_all is true after 
%going through everything, then true is returned, otherwise false.

all_like_all(Who_List, _) :-
    Who_List = [].
    
all_like_all(_, What_List) :-
    What_List = [].
    
all_like_all(Who_List, What_List) :-
    Who_List = [Head | Tail],
    all_like_all(Tail, What_List),
    one_like_all(Head, What_List).
   
one_like_all(First, Second) :-
    Second = [AHead | ATail],
    one_like_all(First, ATail),
    likes(First, AHead).
    
one_like_all(First, _) :-
    First = [].
    
one_like_all(_, Second) :-
    Second = [].
    
%question3 

%For this question, first I used sqrt_table to check if the values were all positive. Then, while N was less than M, I found the square root of N
%then I added the value to the Result as [number, Square root of number]. N was continously decrementing, once N had reached M, it appended the last value
%and stopped calling sqrt_table.

sqrt_table(N, M, Result) :-
    N > 0,
    M > 0,
    N > M,
    appendvalue(N, M, Result).
    
sqrt_table(N, M, Result) :-
    N > 0,
    M > 0,
    N = M,
    lastappend(N, Result).
    
appendvalue(First, Second, Result) :- 
    X is sqrt(First),   
    Result = [Head | Tail],
    Head = [First, X],
    Counter is -(First, 1),
    sqrt_table(Counter, Second, Tail).
    
lastappend(First, Result) :-
    X is sqrt(First),
    Result = [[First, X]].
    
%question4

%for this question, I went through the list recursively. I checked for 2 values, X and Y, where Y comes directly after X. Now, if Y is equal to X + 1, 
%then I run a find predicate, which recursively calls itself to find the last value where the next number in the sequence is not successive. It then adds the 
%[X, Final] value to newList. If Y does not equal to X + 1, then we can just add X to NewList straight away since it is a stand-alone value. 

chop_up(List, NewList) :-
    List = [],
    NewList = [].

chop_up(List, NewList) :-
    List = [Item],
    NewList = [Item].

chop_up(List, NewList) :-
	List = [Head | Tail],
	Tail = [NextHead | _],
	NextHead =\= +(Head, 1),
	chop_up(Tail,SubList),
	NewList = [Head | SubList].
	
chop_up(List, NewList) :-
    List = [Head | Tail],
	Tail = [NextHead | _],
	NextHead =:= +(Head, 1),
	find(Tail, Head, Head, NewList).

find(NextTail, Head, Original, NewList) :-
    NextTail = [NewHead | NewTail],
    NewHead =:= +(Head,1),
    find(NewTail, NewHead, Original, NewList).

find(NextTail, Head, Original, NewList) :-
    NextTail = [NewHead | _],
    NewHead =\= +(Head,1),
	chop_up(NextTail, SubList),
    NewList = [[Original,Head] | SubList].	
	
find(NextTail, Head, Original, NewList) :-
	NextTail = [],
	NewList = [[Original,Head]].
		
%question5

%for this question, I used the tree function and checked what the operation was. If it was an operator like +,-,/,*, then I 
%recursively went through the tree through both the left and right branches until I reached the bottom. I executed the respective operation,
%on the left and right nodes for each step of the recursion. If the operator value was a number, then we reached a leaf and the recursion ends 
%Similarly if we reach a node with the operator position of 'z', then it's also a leaf and the Eval is just the value. 

tree_eval(Value, Tree, Eval) :-
    Tree = tree(_,z,_),
    Eval = Value.

tree_eval(Value, Tree, Eval) :-
    Tree = tree(L, '+', R),
	operation(Value, Eval, L, R, 1).

tree_eval(Value, Tree, Eval) :-
    Tree = tree(L, '-', R),
    operation(Value, Eval, L, R, 2).

tree_eval(Value, Tree, Eval) :-
    Tree = tree(L, '/', R),
	operation(Value, Eval, L, R, 3).
	
tree_eval(Value, Tree, Eval) :-
    Tree = tree(L, '*', R),
   	operation(Value, Eval, L, R, 4).	
	
tree_eval(_, Tree, Eval) :-
    Tree = tree(_,Eval,_).	
	
operation(Value, Eval, L, R, Number) :-
	tree_eval(Value, L, First),
    tree_eval(Value, R, Second),
	evaluate(Number, Eval, First, Second).

evaluate(Number, Eval, First, Second) :-
	Number =:= 1,
	Eval is +(First,Second).

evaluate(Number, Eval, First, Second) :-
	Number =:= 2,
	Eval is -(First,Second).

evaluate(Number, Eval, First, Second) :-
	Number =:= 3,
	Eval is /(First,Second).

evaluate(Number, Eval, First, Second) :-
	Number =:= 4,
	Eval is *(First,Second).	
	


	
	

	

	

	
	
	


    




        
    

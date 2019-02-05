% N-Queens Problem

%      ?- N = 8, n_queens(N, Qs), labeling([ff], Qs).
%      Qs = [1, 5, 8, 6, 3, 7, 2, 4] .

%      ?- N = 20, n_queens(N, Qs), labeling([ff], Qs).
%      Qs = [1, 3, 5, 14, 17, 4, 16, 7, 12, 18, 15, 19, 6, 10, 20, 11, 8, 2, 13, 9] .

%      ?- N = 100, time((n_queens(N,Qs),labeling([ff],Qs))).
%      % 2,304,144 inferences, 0.553 CPU in 0.555 seconds (100% CPU, 4168963 Lips)
%      Qs = [1, 3, 5, 57, 59, 4, 64, 7, 58|...] .

% from: https://www.metalevel.at/queens/

%:- use_module(library(clpfd)).

% Francois FAGES (fages@dmi.ens.fr)
% 13th June 96
%
%
% This directory contains a prototype implementation of
% Constraint Logic Programming over finite domains CLP(FD),
% on top of Prolog (currently Sicstus Prolog version 3 or 2).
%
% This system has been realized only to serve *teaching purposes*,
% as a companion to the book of Francois FAGES,
% "Programmation Logique par Contraintes",
% Collection Cours de l'Ecole Polytechnique,
% Ellipse, Paris. 1996.
%
% The source code is freely distributed without any warranty of any kind.
% Bug reports can be sent to fages@dmi.ens.fr, but there is no
% warranty that they will be fixed in new releases,
% nor that the system will be ported on top of other systems than
% Sicstus Prolog.
%
% The system has been designed for a pedagogical purpose only.
% Much more efficient CLP(FD) systems are available on the market,
% and it is recommended to use these other systems for more
% advanced experimental works.
%
% In order to install the system, just load the file make_clpFD.pl
% under Sicstus Prolog.
%
% Not all the CLP(FD) predicates described in the appendix of the book
% have been implemented yet, a good exercise is to complete that library.

% Francois FAGES
% Jan 96
% Alexandre FREY
% April 96

'.='(S,T) :-
        eqln(S,T).

'.=\\='(X,Y) :- noteq(X,Y).

'.<='(S,T) :-
        '.>='(T,S).

'.>='(S,T) :-
        supln(S,T).

'.>'(X,Y) :- '.>='(X,Y+1).
'.<'(X,Y) :- '.<='(X+1,Y).


% Affichage des domaines geles sur les variables au format d'entree

portray(_:dom(_Min,_Max,_,_,_,_,_,_,X)):-
    write('domain('),write(X),
    get_mutable(Min,_Min),get_mutable(Max,_Max),
    write(',['),write(Min),write(','),write(Max),write('])').
%    domainvalues(X,L),write(',enum('),write(L),write('))')).



portray(_:(_=V)):-(V==0),!,write(t).


portray(_:noteq(X,Y,C)) :-!,
	write(X),write('.=\='),write(Y),
	(C==0
    ->
	true
    ;
	write('+'),write(C)).

portray(_:elementI(I,L,X)) :-
        write(element(I,L,X)).

portray(_:elementX(_,_,_)) :- write(t).

portray(_:atmost(I,L,X)) :- write(atmost(I,L,X)).

portray(_:card(I,L,X)) :- write(card(I,L,X)).


% Affichage des contraintes gelees sous le format d'entree

portray(_:supX(X,Y,C)) :-
        write(X),write('.>='),write(Y),
        (C>0 -> write('+'),write(C)
         ;(C<0 -> write(C);true)).

portray(_:supY(_,_,_)):- write('t').

% On affiche ca joliement
portray(_:sup_constraint(M,C)) :-
	% on affiche la contrainte que si M est le plus grand monome de C
	% Comme ca, on affiche pas 36 fois la meme contrainte
	portray_constraint(M,C,C).

portray_constraint(_,_*1,C) :-
%	write([M]),
	pp_ln(C, 0, 0, 0).

portray_constraint(M,P+N,C) :-
	((M @< N) ->
%	    write(redond) ;
	    write(t) ;
	    portray_constraint(M,P,C)).

pp_ln(P+A*B, Pos, Neg, C)  :-
	integer(B),
	C1 is C+A*B,
	pp_ln(P, Pos, Neg, C1).

pp_ln(P+1*X, 0, Neg, C) :-
	!,
	pp_ln(P, X, Neg, C).
pp_ln(P+1*X, Pos, Neg, C) :-
	pp_ln(P, Pos+X, Neg, C).

pp_ln(P+(-1)*X, Pos, 0, C) :-
	!,
	pp_ln(P, Pos, X, C).
pp_ln(P+(-1)*X, Pos, Neg, C) :-
	pp_ln(P, Pos, Neg+X, C).

pp_ln(P+A*X, 0, Neg, C) :-
	A>0,
	!,
	pp_ln(P, A*X, Neg, C).
pp_ln(P+A*X, Pos, Neg, C) :-
	A>0,
	pp_ln(P, Pos+A*X, Neg, C).

pp_ln(P+A*X, Pos, 0, C) :-
	A<0,
	!,
	A1 is -A,
	pp_ln(P, Pos, A1*X, C).
pp_ln(P+A*X, Pos, Neg, C) :-
	A1 is -A,
	pp_ln(P, Pos, Neg+A1*X, C).

% Cas de base (il y a toujours une constante)
pp_ln(A*B, Pos, Neg, C) :-
	C1 is C+A*B,
	pp_ln(0, Pos, Neg, C1).

pp_ln(0, Pos, Neg, 0) :-
	write(Pos.>=Neg).
pp_ln(0, 0, Neg, C) :-
	C>0,
	!,
	write(C.>=Neg).
pp_ln(0, Pos, 0, C) :-
	C<0,
	!,
	C1 is -C,
	write(Pos.>=C1).
pp_ln(0, Pos, Neg, C) :-
	C>0,
	write(Pos+C.>=Neg).
pp_ln(0,Pos, Neg, C) :-
	C<0,
	C1 is -C,
	write(Pos.>=Neg+C1).

% Francois FAGES
% Jan 96
% May 96

% Representation du domaine des variables par un predicat
% dom(Min,Max,Biais,BitVecteur,Vinst,Vmin,Vmax,Vmid,X)
% attache a la variable par freeze, lu par frozen,
% et execute lors de l'instanciation de la variable.

dom(_Min,_Max,Biais,BV,Vinst,_Vmin,_Vmax,_Vmid,X):-
    get_mutable(Min,_Min),get_mutable(Max,_Max),
    get_mutable(f(Vmin),_Vmin),
    get_mutable(f(Vmax),_Vmax),get_mutable(f(Vmid),_Vmid),
    integer(X),
    X>=Min,
    X=<Max,
	( nonvar(Biais) ->
	    memberBV(X,Biais,BV);
	    true
	),
    f(Vinst,Vmin,Vmax,Vmid)=f(0,0,0,0).

% Compatibilite Sicstus versions 3 et 2
mysetarg(I,D,V):-
    arg(I,D,A),
    update_mutable(V,A).

createdomain(X,Min,Max):-
     create_mutable(Min,_Min),create_mutable(Max,_Max),
     create_mutable(f(_),_Vmin),
     create_mutable(f(_),_Vmax),create_mutable(f(_),_Vmid),
     freeze(X,dom(_Min,_Max,_,_,_,_Vmin,_Vmax,_Vmid,X)).


getdomain(X,D) :-
    var(X),
    frozen(X,L),
    (L=true
     ->
     createdomain(X,0,4294967292),
     getdomain(X,D)
     ;
     (L=(_:dom(_,_,_,_,_,_,_,_,_))
      ->
      L=(_:D)
      ;
      write('Erreur, predicats geles sur variable de domaine '),
      write(X),write(L),nl,
      fail)).

% Predicat de gel a utiliser sur une variable de domaine
freeze_domain_var(X,G) :-
        (var(X) -> freezeinst(X,G) ; G).


domainvar_p(X) :- frozen(X,(_:D)),functor(D,dom,9).


% Declaration des domaines

domain(X,enum(L)) :- var(X),!, min_list(L,M), max_list(L,N),
		     domain(X,[M,N]), M1 is M+1, make_holes(M1,N,L,X).

domain(X,[M,M]) :- var(X),!, M>=0, X=M.

domain(X,[Min,Max]) :-
    var(X),!,
    Min>=0,
    Min<Max,
    (domainvar_p(X) -> setmin(X,Min), setmax(X,Max) ; createdomain(X,Min,Max)).

domain(X,D) :-
    integer(X),!,
    (D=[Min,Max] -> Min>=0, Min=<X, X=<Max
                  ; D=enum(L), member(X,L), !).

domain(X,D):-var(X),!,write('Erreur: domaine incorrect '),write(X),write(D).

domain([],_).

domain([X|L],D) :- domain(X,D), domain(L,D).

make_holes(M,N,_,_):- M>=N, !.
make_holes(M,N,L,X):- (member(M,L) -> true ; rmvalue(X,M)),
                      M1 is M+1,
                      make_holes(M1,N,L,X).

% Acces au min et au max

minvalue(X,Min) :- (integer(X) -> Min=X ; getdomain(X,dom(_Min,_,_,_,_,_,_,_,_)),get_mutable(Min,_Min)).
maxvalue(X,Max) :- (integer(X) -> Max=X ; getdomain(X,dom(_,_Max,_,_,_,_,_,_,_)),get_mutable(Max,_Max)).
minmaxvalue(X,Min,Max) :-
     (integer(X)
      ->
      Min=X , Max=X
      ;
      getdomain(X,dom(_Min,_Max,_,_,_,_,_,_,_))),get_mutable(Min,_Min),get_mutable(Max,_Max).

% Modification du min et du max

setmin(X,M):-
    integer(X),!,
    X>=M.
setmin(X,M):-
    getdomain(X,D),
    D=dom(_Min,_Max,Biais,BV,_,_Vmin,_,_,_),
    get_mutable(Min,_Min),get_mutable(Max,_Max),get_mutable(f(Vmin),_Vmin),
        (M>Min ->
                first_up(Biais,BV,Max,M,Rmin),
                (Rmin=Max ->
                           X=Rmin
                        ; (Rmin<Max ->
                                  mysetarg(1,D,Rmin),
                                  Vmin=0
                                ; fail))
        ; M=<Max).

setmax(X,M):-
    integer(X),!,
    X=<M.

setmax(X,M):-
    getdomain(X,D),
    D=dom(_Min,_Max,Biais,BV,_,_,_Vmax,_,_),
    get_mutable(Min,_Min),get_mutable(Max,_Max),get_mutable(f(Vmax),_Vmax),
        (M<Max ->
                first_down(Biais,BV,Min,M,Rmax),
                (Rmax=Min ->
                           X=Rmax
                        ; (Rmax>Min ->
                                  mysetarg(2,D,Rmax),
                                  Vmax=0
                                ; fail))
        ; M>=Min).


% Gel sur les variables indicatrices de modification du domaine

instlist([],[]).
instlist([X|L],[V|L2]):-
        getdomain(X,dom(_,_,_,_,V,_,_,_,_)),
        !,
        instlist(L,L2).
instlist([_|L],L2):-
        instlist(L,L2).


instminmaxmidlist([],[]).
instminmaxmidlist([X|L],[V1,V2,V3|L2]):-
        getdomain(X,D),
        D=dom(_,_,_,_,_,_Vmin,_Vmax,_Vmid,_),
        !,
        get_mutable(f(Vmin),_Vmin),
        get_mutable(f(Vmax),_Vmax),
        get_mutable(f(Vmid),_Vmid),
        (var(Vmin)->V1=Vmin;mysetarg(6,D,f(V1))),
        (var(Vmax)->V2=Vmax;mysetarg(7,D,f(V2))),
        (var(Vmid)->V3=Vmid;mysetarg(8,D,f(V3))),
        instminmaxmidlist(L,L2).
instminmaxmidlist([_|L],L2):-
        instminmaxmidlist(L,L2).

freezeinst(X,G):-
    getdomain(X,D),
    D=dom(_,_,_,_,Vinst,_,_,_,_),
    freeze(Vinst,G).

freezemin(X,G):-
    getdomain(X,D),
    D=dom(_,_,_,_,_,_Vmin,_,_,_),get_mutable(f(Vmin),_Vmin),
    (var(Vmin)->V=Vmin;mysetarg(6,D,f(V))),
    freeze(V,G).

freezemax(X,G):-
    getdomain(X,D),
    D=dom(_,_,_,_,_,_,_Vmax,_,_),get_mutable(f(Vmax),_Vmax),
    (var(Vmax)->V=Vmax;mysetarg(7,D,f(V))),
    freeze(V,G).

freezemid(X,G):-
    getdomain(X,D),
    D=dom(_,_,_,_,_,_,_,_Vmid,_),get_mutable(f(Vmid),_Vmid),
    (var(Vmid)->V=Vmid;mysetarg(8,D,f(V))),
    freeze(V,G).

freezedom(V,G):-
    freezemin(V,X=0),freezemax(V,X=0),freezemid(V,X=0),
    freeze(X,G).

freezedom_l([],_):-!.
freezedom_l(L,G) :- waitdom_or(L,V),freeze(V,G).

waitdom_or([],_).
waitdom_or([X|L],V) :-(var(V)->freezedom(X,V=0),waitdom_or(L,V);true).


freezeinst_l([],_):-!.
freezeinst_l(L,G) :- waitinst_or(L,V),freeze(V,G).

waitinst_or([],_).
waitinst_or([X|L],V) :-(var(V)->freezeinst(X,V=0),waitinst_or(L,V);true).

freeze_or([],_):-!.
freeze_or(L,G) :- wait_or(L,V),freeze(V,G).

wait_or([],_).
wait_or([X|L],V) :-(var(V)->freeze(X,V=0),wait_or(L,V);true).


% Francois FAGES
% Jan 96

% Contrainte X.>=Y+-C
% (cas particulier des inequations lineaires a 2 variables)


supXYC(X,Y,C) :-
    supX(X,Y,C),
    supY(X,Y,C).

% Contrainte gelee sur la diminution du max de X

supX(X,Y,C):- integer(X),!,(integer(Y)->X >= Y+C;M is X-C,setmax(Y,M)).
supX(X,Y,C):- integer(Y),!,M is Y+C,setmin(X,M). % FC forward checking
supX(X,Y,C):-
    maxvalue(Y,MaxY),
    minvalue(X,MinX),
    maxvalue(X,MaxX),
    (MaxY=<MinX-C
     ->
     true                                        % EL elimination
     ;
     M is MaxX-C,
     setmax(Y,M),                                % LA looking-ahead
     (maxvalue(X,MaxX)
      ->
      freezemax(X,supX(X,Y,C))
      ;
      supX(X,Y,C))).

% Contrainte gelee sur l'augmentation du min de Y

supY(X,Y,C):- integer(X),!,(integer(Y)->X >= Y+C;M is X-C,setmax(Y,M)).
supY(X,Y,C):- integer(Y),!,M is Y+C,setmin(X,M). % FC forward checking
supY(X,Y,C):-
    minvalue(X,MinX),
    minvalue(Y,MinY),
    maxvalue(Y,MaxY),
    (MinX>=MaxY+C
     ->
     true
     ;
     M is MinY+C,
     setmin(X,M),
     (minvalue(Y,MinY)
      ->
      freezemin(Y,supY(X,Y,C))
      ;
      supY(X,Y,C))).


% Yan GEORGET
% Robert HARLEY
% April 96
% Modified by Francois FAGES
% May 96


initBV(X) :-
	getdomain(X,D),
	D=dom(_Min,_Max,Biais,BV,_,_,_,_,_),
        get_mutable(Min,_Min),get_mutable(Max,_Max),
	( nonvar(Biais) ->
	    true
	;
	    Biais = Min ,
	    M is Max-Min+1, % nb de bits necessaires
%	    BV is (1<<M)-1
	    functor(BV,v,M)
	).

%------------------------------------------------------------------------

is_indomain(X,V):- integer(X), !, X=V.
is_indomain(X,V):-
	  getdomain(X,D),
	  D=dom(_Min,_Max,Biais,BV,_,_,_,_,_),
          get_mutable(Min,_Min),get_mutable(Max,_Max),
          V>=Min,
          Max>=V,
          (var(Biais)
           -> true
          ; P is V-Biais+1,
           arg(P,BV,Bit),
           var(Bit)).

%------------------------------------------------------------------------

rmvalue_l([],_) :-
	!.
rmvalue_l([X|L],V) :-!,
	rmvalue(X,V),
	rmvalue_l(L,V).

rmvalue(X,V) :- integer(X),!,X=\=V.
rmvalue(X,V) :-
	getdomain(X,D),
	D=dom(_Min,_Max,Biais,BV,_,_,_,_Vmid,_),
        get_mutable(Min,_Min),get_mutable(Max,_Max),
        get_mutable(f(Vmid),_Vmid),
    (V<Min,!;
     V>Max,!;
     V = Min,!, V1 is V+1, setmin(X,V1) ;
     V = Max,!, V1 is V-1, setmax(X,V1) ;
     (var(Biais)-> initBV(X);true),
     B is V-Biais+1,
     arg(B,BV,Bit),
     (var(Bit)-> Bit=0, Vmid=0;true)).

%------------------------------------------------------------------------

memberBV(V,Biais,BV) :-
%	B is 1<<(V-Biais),
%	(BV/\B) =\= 0.
        B is V-Biais+1,
        arg(B,BV,Bit),
        var(Bit).


%------------------------------------------------------------------------



first_down(_,BV,_,Max,Max):-var(BV),!.
first_down(_,_,Min,Min,Min) :- !.
first_down(B,BV,_,I,I) :-
	C is I-B+1,
        arg(C,BV,Var),
        var(Var),!.
first_down(B,BV,Min,I,Rmax) :-
	I>Min,
	J is I-1,
	first_down(B,BV,Min,J,Rmax).


first_up(_,BV,_,Min,Min):-var(BV),!.
first_up(_,_,Max,Max,Max) :- !.
first_up(B,BV,_,I,I) :-
        C is I-B+1,
        arg(C,BV,Var),
        var(Var),!.
first_up(B,BV,Max,I,Rmin) :-
	I<Max,
	J is I+1,
	first_up(B,BV,Max,J,Rmin).

%------------------------------------------------------------------------

deletedomain(_,[]):- !.
deletedomain(X,[V|L]) :-
	rmvalue(X,V),
	deletedomain(X,L).

% Robert HARLEY
% April 96


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%  alldifferent([X1,...,Xn])


alldifferent([]):-!.
alldifferent([E|L]):-
        all(E,L),
        alldifferent(L).

all(_,[]):-!.
all(E,[E2|L]):-
        noteq(E,E2,0),
        all(E,L).



        %%%%%%%%%%%%%%%%%%%%%%%
        %   X =\= Y+-C
        %

noteq(X, Y) :-(var(Y);integer(Y)),!,
        noteq(X,Y,0).
noteq(X, Y+N) :-
        integer(N), !,
        noteq(X,Y,N).
noteq(X, Y-N) :-
        integer(N), !,
        noteq(Y, X, N).


noteq(X,Y,C) :-
        integer(X),integer(Y),!,X=\=Y+C;
        integer(X),!,V is X-C,rmvalue(Y,V);
        integer(Y),!,V is Y+C,rmvalue(X,V);
        getdomain(X,dom(_,_,_,_,X1,_,_,_,_)),
        getdomain(Y,dom(_,_,_,_,Y1,_,_,_,_)),
        freeze(X1,G=0),
        freeze(Y1,G=0),
        freeze(G,noteq(X,Y,C)).


% Yan GEORGET
% Robert HARLEY
% April 96

labeling(_,_,_) :- write('Error: labeling/3 is not yet implemented.'),fail.
labeling(_,_) :- write('Error: labeling/2 is not yet implemented.'),fail.
indomain(_,_) :- write('Error: indomain/2 is not yet implemented.'),fail.

labeling([]).
labeling([X | L]) :-
	indomain(X),
	labeling(L).


size_dv(H,SH):-
	(
	    integer(H)
	->
	    SH=1
	;
	    getdomain(H,dom(_Min,_Max,Bias,Bit,_,_,_,_,_)),
	    get_mutable(Min,_Min),get_mutable(Max,_Max),
	    Max1 is Max+1,
	    size(Min,Max1,Bias,Bit,0,SH)
	).

size(Min,Max,_,Bit,_,SH) :- var(Bit),!,SH is Max-Min+1.
size(Max,Max,_,_,SH,SH):-!.
size(I,Max,Bias,Bit,J,SH):-
	I<Max,
	C is I-Bias+1,
	arg(C,Bit,Arg),
	(var(Arg)->J1 is J+1;J1=J),
	I1 is I+1,
	size(I1,Max,Bias,Bit,J1,SH).



labelingff([]) :- !.
labelingff(L) :-
	deleteff(F,L,Ls),!,
	indomain(F),
	labelingff(Ls).


indomain(X) :-integer(X),!.
indomain(X) :-
	getdomain(X,dom(_Min,_Max,Bias,Bit,_,_,_,_,_)),
        get_mutable(Min,_Min),get_mutable(Max,_Max),
	in(V,Min,Max),
	(var(Bit)
    ->
	true
    ;
	C is V-Bias+1,
	arg(C,Bit,Arg),
	var(Arg)),
	X=V.

domainvalues(X,L):-domvals(X,L).

domvals(X,L):-integer(X),!,L=[X].
domvals(X,L) :-
	getdomain(X,dom(_Min,_Max,Bias,Bit,_,_,_,_,_)),
        get_mutable(Min,_Min),get_mutable(Max,_Max),
	domvals(Min,Max,Bias,Bit,L).

domvals(Max,Max,_,_,[Max]).
domvals(Min,Max,Bias,Bit,L):-
	Max>Min,
	Min1 is Min+1,
	(var(Bit)
    ->
	L=[Min|L1]
    ;
	C is Min-Bias+1,
	arg(C,Bit,Arg),
	(var(Arg)->L=[Min|L1];L=L1)),
	domvals(Min1,Max,Bias,Bit,L1).


% First-fail principle sur taille du domaine

deleteff(X,[Y|L],L1) :-
	size_dv(Y,SY),
	deleteff(X,L,Y,SY,L1).

deleteff(X,L,X,1,L):-!.
deleteff(X,[],X,_,[]) :- !.
deleteff(X,[Z|L],Y,J,L1) :-
	size_dv(Z,I),
	(J =< I->L1=[Z|L2],H1=Y,K=J;L1=[Y|L2],H1=Z,K=I),
	deleteff(X,L,H1,K,L2).

in(Min,Min,_).
in(X,Min,Max) :-
        Min<Max,
        M is Min+1,
        in(X,M,Max).



% Yan GEORGET
% April 96
% Modified by Francois Fages
% May 96

% ce fichier contient les contraintes : atmost,card,element
% Propagation en looking-ahead LA pour element
% Partial looking ahead PLA pour atmost et card
% (Variante looking ahead LA en commentaire pour atmost et card.)

%------------------------------------------------------------------------
% separe dans un liste de variables de domaines
% celles qui sont instanciees et celles qui ne le sont pas

separe([],[],[]) :- !.
separe([X|L],[X|L1],L2):-
	integer(X),
	!,
	separe(L,L1,L2).
separe([X|L],L1,[X|L2]):-
	separe(L,L1,L2).

%------------------------------------------------------------------------
% contrainte atmost

atmost(N,L,V) :-
	separe(L,LI,LNI),
	atmost(N,LI,LNI,V).


% dans ce predicat on utilise l'information provenant du fait que
% certaines variables de domaine sont instanciees

atmost(N,[],LNI,V) :-
	!,
	do_atmost(N,LNI,V).
atmost(N,[XI|LI],LNI,V) :-
	(XI = V ->
	    N1 is N-1,
	    N1>=0,
	    atmost(N1,LI,LNI,V) ;
	 atmost(N,LI,LNI,V)
	).


% ce predicat est equivalent a atmost(N,L,V)
% quand aucune des variables de la liste L n'est instanciee

do_atmost(0,L,V) :-
	!,
	rmvalue_l(L,V).
do_atmost(N,L,V) :-
%	acceptable(L,V,NV,LV), % Look ahead LA
                            % on compte le nb de variables
                            % qui peuvent avoir la valeur V
        LV=L,length(LV,NV), % Partial look ahead PLA
	( N >= NV ->
	    true ;          % la contrainte sera toujours satisfaite
%           freezedom_l(LV,atmost(N,LV,V))   % si LA
	    freezeinst_l(LV,atmost(N,LV,V))  % si PLA
	).

%------------------------------------------------------------------------
% compte le nb de variables d'une liste L qui peuvent prendre la valeur V

acceptable([],_,0,[]) :- !.

acceptable([X|L],V,N1,[X|LV]) :-
	is_indomain(X,V),
	!,
	acceptable(L,V,N,LV),
	N1 is N+1 .
acceptable([_|L],V,N,LV) :-
	acceptable(L,V,N,LV).

%------------------------------------------------------------------------
% contrainte card

card(N,L,V) :-
	separe(L,LI,LNI),
	card(N,LI,LNI,V).


% dans ce predicat on utilise l'information provenant du fait que
% certaines variables de domaine sont instanciees

card(N,[],LNI,V) :-
	!,
	do_card(N,LNI,V).
card(N,[XI|LI],LNI,V) :-
	(XI = V ->
	    N1 is N-1,
	    N1>=0,
	    card(N1,LI,LNI,V) ;
	    card(N,LI,LNI,V)
	).


% ce predicat est equivalent a card(N,L,V)
% quand aucune des variables de la liste L n'est instanciee

do_card(0,L,V) :-
	!,
	rmvalue_l(L,V).

do_card(N,L,V) :-
%	acceptable(L,V,NV,LV),   % Looking ahead LA
       LV=L,length(LV,NV),       % Partial look ahead PLA
	( N < NV
        ->
%        freezedom_l(LV,card(N,LV,V)) % Si LA
        freezeinst_l(LV,card(N,LV,V)) % Si PLA
        ; (N=NV -> domain(LV,[V,V]) ; fail)).

%------------------------------------------------------------------------
% contrainte element
% j'ai choisi pour de raisons de simplicite
% de separer les causes de reveil

element(I,L,X) :-
	elementI(I,L,X),
	elementX(I,L,X).


% contrainte gelee sur I

elementI(I,L,X) :-
	integer(I),
	!,
	nth(I,L,V),
        V=X.

elementI(I,L,X) :-
        domainvalues(I,LI),
	values_of_index(LI,L,LV), % on calcule les valeurs possibles pour X
	domain(X,enum(LV)),      % on modifie en consequence
                                 % le domaine de X
	(domainvalues(I,LI) -> % on itere jusqu'a stabilisation
	    freezedom(I,elementI(I,L,X)) ;
	    elementI(I,L,X)
	).

elementX(I,L,X) :-
        domainvalues(X,LX),
	index_of_values(LX,L,LI), % on calcule les valeurs possibles pour I
	domain(I,enum(LI)),      % on modifie en consequence
	                         % le domaine de I
	(domainvalues(X,LX) -> % on itere jusqu'a stabilisation
	    (integer(X)-> true; freezedom(X,elementX(I,L,X))) ;
	    elementX(I,L,X)
	).


%------------------------------------------------------------------------
% calcul des valeurs a partir des index

values_of_index([],_,[]).
values_of_index([I|LI],L,[X|LX]) :-
        nth(I,L,X),
        !,
	values_of_index(LI,L,LX).
values_of_index([_|LI],L,LX):-
        values_of_index(LI,L,LX).


% calcul des index a partir des valeurs

index_of_values([],_,[]).
index_of_values([X|LX],L,LI) :-
        findall(I,nth(I,L,X),L1),
        index_of_values(LX,L,L2),
        append(L1,L2,LI).


% Francois FAGES (fages@dmi.ens.fr)
% May 91

% :- forward(p(g/d,...,g/d)).
% :- lookahead(p(g/d,...,g/d)).

% Attention les predicats doivent etre declares DYNAMIC
% avant leur definition (pour des arguments instancies),
% et les declarations forward/lookahead
% doivent etre places APRES leur definition.


%       FORWARD CHECKING

% rajoute en tete de la definition du predicat G
% une regle qui implante le forward checking sur les variables de domaine

forward(G) :-
	head(G,Head,Listevar),
	asserta((Head :-
			listdomainvar(Listevar,L),
			length(L,N),
			N>0,
			!,
			forwardclause(Head,L,N))).

forwardclause(Head,L,N) :-
	(N=1
	->
		L=[V],
		domainvalues(V,Listeval),
		my_copy_term(Head,Headc,[V,Vc]),
		forwardchecking(Listeval,Vc,Headc,L2),
		deletedomain(V,L2)
	;
                freezeinst_l(L,Head)).

% construction de la tete de clause p(X1,...,Xn)
% et de la liste L des arguments pouvant etre des variables de domaine
head(G,Head,L) :-
	functor(G,P,N),
	functor(Head,P,N),
	listdomainarg(N,G,Head,L).

listdomainarg(0,_,_,[]):-!.

listdomainarg(N,G,H,L):-
	N1 is N-1,
	(arg(N,G,g)	% ground term
	->
		L=L1
	;
		arg(N,H,V),
		L=[V|L1]),
	listdomainarg(N1,G,H,L1).


% filtrage des variables de domaine
listdomainvar([],[]).
listdomainvar([X|L],L2) :-
	listdomainvar(L,L1),
	((nonvar(X);memberof(X,L1))
    ->
	L2=L1
    ;
	L2=[X|L1]).

memberof(X,[Y|L]):-(X==Y->true;memberof(X,L)).

mapdomainvalues([],[],[],[]).
mapdomainvalues([V|L],[D|Ldom],[V,Z|Lassoc],[Z|Lc]):-
	domainvalues(V,D),
	mapdomainvalues(L,Ldom,Lassoc,Lc).

mapdeletedomain([],[]).
mapdeletedomain([X|L],[L1|L2]):-
	deletedomain(X,L1),
	mapdeletedomain(L,L2).


% Calcul du nouveau domaine
% en mettant dans Liste les valeurs a` supprimer
forwardchecking([],_,_,[]).
forwardchecking([X|L],V,Head,Liste):-
	(forwardcheck(X,V,Head)
	->
		Liste=[X|L1]
	;
		Liste=L1),
	forwardchecking(L,V,Head,L1).

forwardcheck(X,X,Head) :- call(Head),!,fail.
forwardcheck(_,_,_).


%                   LOOKING AHEAD

% rajoute en tete de la definition du predicat G
% une regle qui implante le look ahead sur les variables de domaine
lookahead(G) :-
	head(G,Head,Listevar),
	asserta((Head :-
			listdomainvar(Listevar,L),
			length(L,N),
			N>0,
			!,
			lookaheadclause(Head,L,N))).

lookaheadclause(Head,L,N) :-
	(N=1
	->
                forwardclause(Head,L,N)
	;
		mapdomainvalues(L,Listedom,Lassoc,Lc),
		my_copy_term(Head,Headc,Lassoc),
		lookingahead(Listedom,Lc,Headc,L2,Listedom,Lc),
% gel sur variables avant reductions des domaines pour test iteration
		instminmaxmidlist(L,LV),
		mapdeletedomain(L,L2),
		freeze_or(LV,Head)).


% Calcul du nouveau domaine des variables de LV
% en mettant dans L2 les valeurs a` supprimer pour chaque variable
% appel	lookingahead(Listedom,Lc,Headc,L2,Listedom,Lc),

lookingahead([],[],_,[],_,_).
lookingahead([D|LD],[V|LV],Head,[DomV|ListeDom],LD0,Lc0):-
	lookingaheadV(D,V,Head,DomV,LD0,Lc0),
	lookingahead(LD,LV,Head,ListeDom,LD0,Lc0).

% essai des valeurs possibles de V
lookingaheadV([],_,_,[],_,_).
lookingaheadV([X|D],V,Head,Liste,LD,LV):-
	(lookaheadunsat(X,LD,V,LV,Head)
	->
		Liste=[X|L1]
	;
		Liste=L1),
	lookingaheadV(D,V,Head,L1,LD,LV).

% essai beton de toutes les valeurs possibles des variables
lookaheadunsat(X,LD,V,LV,Head) :- X=V,lookaheadsat(LD,LV,Head),!,fail.
lookaheadunsat(_,_,_,_,_).

lookaheadsat([],[],Head):-
	call(Head).
lookaheadsat([D|LD],[V|LV],Head):-
	member(V,D),
	lookaheadsat(LD,LV,Head).



%                 UTIL.

my_copy_term(X,V,Lassoc) :-
	var(X),
	!,
	varcopy(X,Lassoc,V).
my_copy_term(X,Y,Lassoc) :- functor(X,F,N), functor(Y,F,N),
				copy_arg(N,X,Y,Lassoc).

copy_arg(0,_,_,_):- !.
copy_arg(N,X,Y,Lassoc) :-
	arg(N,X,A),
	my_copy_term(A,B,Lassoc),
	arg(N,Y,B),
	N1 is N-1,
	copy_arg(N1,X,Y,Lassoc).

varcopy(X,[Y,Z|L],U) :-
	(X==Y
	->
		U=Z
	;
		varcopy(X,L,U)).

% Alexandre FREY
% April 96

% calcule une forme canonique d'un polynome
% base0 ::= X^N.                    ou N est un nombre
% base  ::= 1 | base*base0.         ou les base0 sont tries par @<
% monome ::= base*N.                ou N est un nombre
% polynome ::= 0 | polynome+monome. ou les monomes sont tries par @<

canon(N, C) :-
	number(N),
	(N=0 ->
	    C=0 ;
	    C=0 + 1*N).

canon(X, 0+1*X^1*1) :-
	var(X),
	!.	     % pour eviter que X ne s'unifie avec la clause suivante

canon(T1+T2, C) :-
	canon(T1, C1),
	canon(T2, C2),
	'P+P'(C1, C2, C).
canon(T1-T2, C) :-
	canon(T1+(-T2), C).

canon(-T, C) :-
	canon(T, C1),
	'P*X'(C1, -1, C).

canon(T1*T2, C) :-
	canon(T1, C1),
	canon(T2, C2),
	'P*P'(C1, C2, C).
canon(T^N,C) :-
	number(N),
	canon(T,C1),
	'P^N'(C1,N,C).

'P^N'(_,0,C) :-
	!,
	canon(1,C).
'P^N'(P,N,C) :-
	N_1 is N-1,
	'P^N'(P,N_1,C1),
	'P*P'(C1, P, C).

'P+P'(P, 0, P).
'P+P'(P, Q+M*A, R) :-
	'P+P'(P, Q, R1),
	'P+M'(R1, M*A, R).

'P*P'(_, 0, 0).
'P*P'(P, Q+M, R) :-
	'P*P'(P, Q, R1),
	'P*M'(P, M, R2),
	'P+P'(R1, R2, R).


'P*M'(P, M*A, R) :-
	'P*B'(P, M, R1),
	'P*X'(R1, A, R).

'P*X'(0, _, 0).
'P*X'(P+M*A, B, Q+M*C) :-
	number(B),
	C is A * B,
	'P*X'(P, B, Q).

'P*B'(0,_,0).
'P*B'(P+M*A, N, Q+M2*A) :-
	'B*B'(M, N, M2),
	'P*B'(P,N,Q).

% un monome de base est un multiensemble de variables
% ici represente par une liste (construite avec 1 et *)
% de couple Variable^Exposent
% triee avec l'ordre standard @< sur les Variables
% Rmq : chaque fois qu'une unification est faite, il faut mettre
% a jour le monome !

% la multiplication d'un monome canonique et d'un monome monovarie
% Rmq : * associe a gauche donc la multiplication naturelle est
% a droite
'B*B0'(1, X^N, 1*X^N).
'B*B0'(M*X^N, Y^P, M*X^Q) :-
	Y == X,
	Q is N+P.
'B*B0'(M*X^N, Y^P, M*X^N*Y^P) :-
	X @< Y. % completement arbitraire
'B*B0'(M*X^N, Y^P, R*X^N) :-
	X @> Y,
	'B*B0'(M, Y^P, R).

%la multiplication de deux monomes canoniques
'B*B'(M, 1, M).
'B*B'(M, P*Q, R) :-
	'B*B0'(M, Q, R1),
	'B*B'(R1, P, R).

% L'addition d'un monome (avec coefficient) a un polynome
'P+M'(0, M*A, 0+M*A).
'P+M'(P+M*A, M1*A1, R) :-
	M == M1,
	B is A + A1,
	(B = 0 ->
	    R = P ;
	    R = P+M*B).
'P+M'(P+M*A, N*B, P+M*A+N*B) :-
	M @< N.
'P+M'(P+M*A, N*B, R+M*A) :-
	M @> N,
	'P+M'(P, N*B, R).

%Pretty printing
pp(0+M,T) :-
	!,		% pour eviter des unification intempestives plus tard
	pp_M(M,T).
pp(M+N,T1+T2) :-
	!,		% idem
	pp(M,T1),
	pp_M(N,T2).

pp(0,0).

pp_M(M*1, T) :-
	!,
	pp_B(M,T).
pp_M(1*A, A) :- !.
pp_M(M*A,A*T1) :-
	pp_B(M, T1).
pp_B(1,1).
pp_B(1*M, T) :-
	!,           % pour eviter d'avoir une solution supplementaire
	pp_B0(M,T).
pp_B(M*N,T*U) :-
	pp_B(M,T),
	pp_B0(N,U).
pp_B0(X^1,X) :- !.
pp_B0(X^N,X^N).

% Alexandre FREY
% April 96


% contraintes lineaires


% forme canonique lineaire, plus sympathique
% echoue si M n'est pas lineaire
canon_ln(M, C) :-
	canon(M, C1),
	canon_ln2(C1, C).

canon_ln2(0, 0*1).
canon_ln2(0+1*A, A*1).        % le terme constant est toujours tout a gauche
canon_ln2(P+1*X^1*A, Q+A*X) :-
	canon_ln2(P, Q).

% calcule la valeur d'une fonction affive etant donnee la
% valeur sur les monomes
value(_, A*B, R) :- integer(B), !, R is A*B.
value(What, P+M, R) :- !,
	value(What, P, R1),
	value(What, M, R2),
	R is R1 + R2.

% le min
value(min, A*X, Min) :-
	(A>0 ->
	    minvalue(X, MinX),
	    Min is A*MinX;
	    maxvalue(X, MaxX),
	    Min is A*MaxX).

% le max
value(max, A*X, Max) :-
	(A>0 ->
	    maxvalue(X, MaxX),
	    Max is A*MaxX;
	    minvalue(X, MinX),
	    Max is A*MinX).

% la valeur sauf pour le monome M
value(except(M, What), N, R) :-
	(M == N ->
	    R = 0 ;
	    value(What, N, R) ).

% Modification du min d'un (vrai) monome.
set_monome_min(A*X, Min) :-
	(A>0
         ->
	 quotient_ceiling(Min, A, R),
	 setmin(X, R)
         ;
	 quotient_floor(Min, A, R),
	 setmax(X, R)).


% Comme d'habitude, les operations de division et de modulo
% sont debiles
quotient_floor(A, B, R) :-
	(A>=0 ->
	    (B>0 ->
		R is A//B ;
		R is (A-B-1)//B)
	    ;
	    (B>0 ->
		R is (A-B+1)//B;
		R is A//B) ).

quotient_ceiling(A, B, R) :-
	quotient_floor(-A, B, R1),
	R is -R1.


freezemax_monome(A*X, G) :-
	(A>0 ->
	    freezemax(X, G)
	    ;
	    freezemin(X, G) ).


% contrainte que M (doit etre lineaire) est positif
positive_ln(M) :-
	canon_ln(M, C),
    (testXYC(C,X,Y,V)
     ->
     supXYC(X,Y,V)
     ;
	% poser pour chaque monome dans C, poser la contrainte
	% sur la diminution de son maximum
	sup_constraint(C, C)
     ).

testXYC(C*1+1*X+ -1*Y,X,Y,C1):-!,C1 is -C.
testXYC(C*1+ -1*X+1*Y,Y,X,C1):-!,C1 is -C.
testXYC(C*1+1*X,X,0,C1):-!,C1 is -C.
testXYC(C*1+ -1*X,C,X,0).


sup_constraint(P+M, C) :-
	!,
	sup_constraint(M, C),
	sup_constraint(P, C).

% Comprend le cas constant
sup_constraint(M, C) :-
	% le but a ete reveille, par diminution du max de M dans C
	% Calculer la nouvelle valeur du maximum de C
	value(max, C, MaxC),

        % ajuster le minimum de tous les monomes de C (Looking-Ahead)
	% (Y compris M lui-meme --> Forward Checking)
	% Question : vaut-il mieux (comme ici) recalculer le max de C
	% a chaque fois ou bien passer a adjust_minimum la valeur MaxC ?
	% adjust_minimum(C),

	% Essai en passer le max de C
	adjust_minimum2(C,C,MaxC),


	% Peut-on maintenant simplifier la contrainte ?
	%
	value(min, C, MinC),
	(MinC>=0 ->
	    % succes !
	    true
	;
	value(max, C, New_MaxC),
	(New_MaxC <0 ->
	    % echec !
	    fail
	;
	% remarque : on a traite le cas ou C etait instantiee

	% Est-ce qu'on a change le maximum de C ?
	(New_MaxC = MaxC ->
	    % non, le calcul des domaines a converge

	    % Est-ce que M est maintenant instantie ?
	    (ground(M) ->
		% oui, donc, on a propage tout ce qu'on pouvait sur les
		% monomes de C (qui en contient forcement)
		% donc, on peut arreter la et laisser tomber cette contrainte
		true
	        ;
		% non, on gele sur la modif du maximum de M
		freezemax_monome(M, sup_constraint(M,C)) )

	    ;
	    % oui, on repart pour un tour
	    % Ca converge forcement car le domaine est fini
	    % (Et il y a un but gele par variable, donc en espace, ok)
	    sup_constraint(M,C) ) ) ).

adjust_minimum(C) :-
	adjust_minimum(C, C).

% terme constant ou monome avec variable instancie
adjust_minimum(_*B, _) :- integer(B), !.

adjust_minimum(P+M,C) :- !,
	adjust_minimum(P, C),
	adjust_minimum(M, C).

% M est un vrai monome C = M + D.
adjust_minimum(M, C) :-
	value(except(M, max), C, MaxD),
	set_monome_min(M, -MaxD).


%autre possibilite : on ne recalcule pas le max de C a chaque fois
adjust_minimum2(_*B,_,_) :-
	integer(B),
	!.
adjust_minimum2(P+M,C,MaxC) :- !,
	adjust_minimum2(P,C,MaxC),
	adjust_minimum2(M,C,MaxC).

adjust_minimum2(M,_,MaxC) :-
	value(max, M, MaxM),
	set_monome_min(M, MaxM-MaxC).


% L'interface avec l'exterieur,
supln(S,T) :-
	positive_ln(S-T).

eqln(S,T) :-
	supln(S,T),
	supln(T,S).


% il peut se faire qu'une contrainte est rendue inutile
% par modification du min d'un monome
% Ce predicat reveille artificiellement  toutes les contraintes
% associees aux variables de L
sweep([]).
sweep([A|L]) :-
	integer(A),
	sweep(L).
sweep([X|L]) :-
	getdomain(X,dom(_,_,_,_,_,_Vmin,_Vmax,_,_)),
        get_mutable(f(Vmin),_Vmin),get_mutable(f(Vmax),_Vmax),
        f(Vmin,Vmax)=f(0,0),
	sweep(L).

% Francois FAGES
% May 96

or(_,_):- write('Error: or/2 is not yet implemented.').
impose(_):- write('Error: impose/1 is not yet implemented.').
cond(_,_,_):- write('Error: cond/3 is not yet implemented.').


% Francois FAGES
% Jan 96

% Optimisation par Separation-Evaluation ("Branch and Bound")
% On suppose que le cout minimum est donne par la plus petite
% valeur du domaine de la variable de cout
% (hypothese verifiee pour les systemes de contraintes aX<bY+c
%  et pour les buts dont les succes sont clos)

minimise(G,C) :-
    retractall(cout(_)),
    retractall(coutopt(_)),
    assert(cout(_),_),
    V.=C,
    boucle(G,V).
minimise(G,C):-
    clause(coutopt(V),_),
    write('Cout optimal '),write(V),nl,
    C.=V,
    G.

boucle(G,V):-
    clause(cout(C),_),
    retractall(cout(_)),
    V.<C,
    (G
     ->
     minvalue(V,Copt),   % HYPOTHESE de COMPLETUDE du SOLVEUR
     write('Solution de cout '),write(Copt),nl,
     assert(cout(Copt)),
     retractall(coutopt(_)),
     assert(coutopt(Copt)),
     fail).
boucle(G,V):-
    clause(cout(_),_),
    boucle(G,V).

minimise(_,_,_):- write('Error: minimise/3 is not yet implemented.'),fail.
minimise_maximum(_,_):- write('Error: minimise_maximum is not yet implemented.'),fail.
minimise_maximum(_,_,_):- write('Error: minimise_maximum is not yet implemented.'),fail.



% usage: intRange(1,5, K).
intRange(N, M, K) :- N =< M, K = N.
intRange(N, M, K) :- N < M, N1 is N+1, intRange(N1, M, K).

n_queens(N, Qs) :- length(Qs, N), intRange(1,N,Qs), safe_queens([Qs]).


safe_queens([]).
safe_queens([Q|Qs]) :-
        write(Q),
        safe_queens(Qs, Q, 1),
        safe_queens(Qs).

safe_queens([], _, _).
safe_queens([Q|Qs], Q0, D0) :-
        Q0 #\= Q,
        abs(Q0 - Q) #\= D0,
        D1 #= D0 + 1,
        safe_queens(Qs, Q0, D1).



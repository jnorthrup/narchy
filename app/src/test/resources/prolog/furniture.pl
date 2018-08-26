% https://www.scribd.com/doc/27776648/5-Simple-Prolog-Programs

ako(chair,furniture).
ako(chair,seat).
isa(your_chair,chair).
isa(you,person).
made_of(your_chair,wood).
colour(wood,brown).
belongs_to(your_chair,you).

?- made_of(your_chair,X), colour(X,Colour).
?- isa(your_chair,X), ako(X,seat).
?- belongs_to(your_chair,X), isa(X,person).

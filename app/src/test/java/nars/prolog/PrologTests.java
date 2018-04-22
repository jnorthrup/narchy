//package nars.prolog;
//
//import alice.tuprolog.*;
//import nars.NAR;
//import nars.Param;
//import nars.nar.Default;
//import nars.op.Operator;
//import org.junit.jupiter.api.Test;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * Created by me on 3/3/16.
// */
//public class PrologTests {
//
//    @Test
//    public void testHello() {
//        new PrologAgent("go :- write('hello, world!'), nl.\n").run(":-solve(go).");
//    }
//
//    @Test
//    public void testAnd() {
//        assertEquals(
//                "yes.",
//                new PrologAgent("t(a).\nt(b).\nt(c).\n",
//                        "t(a), t(b), t(c).").run().toString());
//    }
//
//    @Test
//    public void testAnd2() {
//        assertEquals(
//                "yes.",
//                new PrologAgent("a.\nb.\nc.\n",
//                        "a, b, c.").run().toString());
//    }
//
//    @Test
//    public void testInequality() {
//        assertEquals(
//                "no.",
//                new PrologAgent("a.\nb.",
//                        "a=b.").run().toString());
//        assertEquals(
//                "yes.",
//                new PrologAgent("a.\nb.",
//                        "a=a.").run().toString());
//        assertEquals(
//                "yes.",
//                new PrologAgent("a.\nb.",
//                        "not(a=b).").run().toString());
//    }
//
//    @Test
//    public void maze2() {
//
//        String theory =
//                "arc(a,b).\n" +
//                        "arc(a,d).\n" +
//                        "arc(b,e).\n" +
//                        "arc(d,g).\n" +
//                        "arc(g,h).\n" +
//                        "arc(e,f).\n" +
//                        "arc(f,i).\n" +
//                        "arc(e,h).\n" +
//                        "path(X,X,[X]).\n" +
//                        "path(X,Y,[X|Q]):-arc(X,Z),path(Z,Y,Q).";
//
//        assertEquals("yes.\n" +
//                        "X / [a,b,e]",
//                new PrologAgent(theory, "path(a,e,X).").run().toString());
//
//        assertEquals("yes.\n" +
//                        "X / [a,b,e,f]",
//                new PrologAgent(theory, "path(a,f,X).").run().toString());
//
//        assertEquals("no.", new PrologAgent(theory, "path(z,f,X).").run().toString());
//
//
//    }
//
//    @Test
//    public void testMisc() {
//        Var varX = new Var("X"), varY = new Var("Y");
//        Struct atomP = new Struct("p");
//        Struct list = new Struct(atomP, varY);    // should be [p|Y]
//        System.out.println(list); // prints the list [p|Y]
//        Struct fact = new Struct("p", new Struct("a"), new Int(5));
//        Struct goal = new Struct("p", varX, new Var("Z"));
//        Prolog engine = new Prolog();
//        boolean res = goal.unify(engine, fact);    // should be X/a, Y/5
//        System.out.println(goal);  // prints the unified term p(a,5)
//        System.out.println(varX);  // prints the variable binding X / a
//        Var varW = new Var("W");
//        res = varW.unify(engine, varY);    // should be Z=Y
//        System.out.println(varY);        // prints just Y, since it is unbound
//        System.out.println(varW);        // prints the variable binding W / Y
//
//    }
//
//    /**
//     * http://www.cs.unm.edu/~luger/ai-final2/CH8_Natural%20Language%20Processing%20in%20Prolog.pdf
//     */
//    @Test
//    public void testCFG1() throws MalformedGoalException, NoSolutionException, NoMoreSolutionException {
//
//        String theory =
//                "utterance(X) :- sentence(X, [ ]).\n" +
//                "sentence(Start, End) :- nounphrase(Start, Rest), verbphrase(Rest, End).\n" +
//                "nounphrase([Noun | End], End) :- noun(Noun).\n" +
//                "nounphrase([Article, Noun | End], End) :- article(Article), noun(Noun).\n" +
//                "verbphrase([Verb | End], End) :- verb(Verb).\n" +
//                "verbphrase([Verb | Rest], End) :- verb(Verb), nounphrase(Rest, End).\n" +
//                "article(a).\n" +
//                "article(the).\n" +
//                "noun(man).\n" +
//                "noun(dog).\n" +
//                "verb(likes).\n" +
//                "verb(bites).";
//
//        List<Term> solutions =  new PrologAgent(theory).solutions("utterance([the, man, likes, X]).");
//
//        assertEquals(
//                "[utterance([the,man,likes,man]), utterance([the,man,likes,dog])]",
//                solutions.toString());
//
//    }
//
//    @Test public void testHanoi1() {
////        String theory =
////            "hanoi(1,A,B,C,[to(A,B)|Zs],Zs).\n" +
////            "hanoi(N,A,B,C,Xs,Zs):- " +
////                "N>1," +
////                "N1 is N - 1," +
////                "hanoi(N1,A,C,B,Xs,[to(A,B)|Ys])," +
////                "hanoi(N1,C,B,A,Ys,Zs).";
//
//        new PrologAgent("hanoi(N) :- move(N, left, centre, right).\n" +
//                "        move(0, _, _, _) :- !.\n" +
//                "        move(N, A, B, C) :- M is N-1, move(M, A, C, B), inform(M, A, B), move(M, C, B, A).\n" +
//                "        inform(L, X,Y) :-write([L, ': ', move, disk, from, X, to, Y]), nl.")
//                .run("hanoi(3).");
//
//
//
//
//
//        NAR n = new Default();
//        n.DEFAULT_BELIEF_PRIORITY = 0.1f;
//        n.DEFAULT_GOAL_PRIORITY = 0.9f;
//        n.DEFAULT_QUESTION_PRIORITY = 0.9f;
//        //n.logBudgetMin(System.out, 0f);
//        n.on("move", Operator.auto((g,b)->{
//            System.err.println(g + "\t" + b);
//        }));
//        n.input(
//            "move(3, (l, c, r)). %1.0;0.99%",
//            "move(0, #x)! %1.0;0.99%",
//            //"(?x ==> move(0, ?y))?",
//            "((move(sub($x,1), ($a, $c, $b)) && move(sub($x,1), ($c, $b, $a))) ==> move($x, ($a, $b, $c))). %1.00;0.99%"
//        );
//        n.run(500);
//
//    }
//}
//
//
//

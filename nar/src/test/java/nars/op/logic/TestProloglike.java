package nars.op.logic;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.time.Tense.ETERNAL;

/**
 * Created by me on 4/17/17.
 */
@Disabled
class TestProloglike {





    /**
     * http:
     */
    @Test
    void testProloglike1() throws Narsese.NarseseException {

        NAR n = NARS.tmp();
        /*
        fun(X) :- red(X), car(X).
        fun(X) :- blue(X), bike(X).
        car(vw_beatle).
        car(ford_escort).
        bike(harley_davidson).
        red(vw_beatle).
        red(ford_escort).
        blue(harley_davidson).
        */

        n.freqResolution.set(0.1f);
        n.believe(
                "((red($x) && car($x))==>fun($x))",
                "((blue($x) && bike($x))==>fun($x))",
                "(car($x) <=> (--,bike($x)))",
                "(red($x) <=> (--,blue($x)))",
                "car(vw_beatle)", "car(ford_escort)", "bike(harley_davidson)", "red(vw_beatle)", "blue(ford_escort)", "blue(harley_davidson)"
        );
        
        n.questionPriDefault.set(0.99f);
        n.question("fun(?x)", ETERNAL, (q, a) -> {
            
            System.out.println(a.proof());
        });
        n.run(1000);


    }

    @Test
    void testRiddle1() throws Exception {
        
        NAR n = NARS.tmp();

        n.termVolumeMax.set(1024);
        n.log();
        n.inputNarsese(
                TestProloglike.class.getResource("einsteinsRiddle.nal")
        );
        n.run(128);


    }

    @Test
    void testMetagol() throws Exception {
        NAR n = NARS.tmp();

        
        n.log();
        n.inputNarsese(
                TestProloglike.class.getResource("metagol.nal")
        );

        n.input("grandparent(ann,amelia).",
                "grandparent(steve,amelia).",
                "grandparent(ann,spongebob).",
                "grandparent(steve,spongebob).",
                "grandparent(linda,amelia).",
                "--grandparent(amy,amelia).",
                "parent(ann,andy).",
                "parent(steve,andy).",
                "parent(ann,amy).",
                "$0.99 identity(?x,?y)?",
                "$0.99 identity(grandparent,?y)?",
                "$0.99 curry(?x,?y)?",
                "$0.99 curry(#x,#y)?",
                "$0.99 curry(grandparent,?y)?"
        );
        n.run(1024);


    }

    @Test
    void testTuring() throws Narsese.NarseseException {
        /*
        http:
        Pure Prolog is based on a subset of first-order predicate logic, Horn clauses, which is Turing-complete. The completeness of Prolog can be shown by using it to simulate a Turing machine:

        turing(Tape0, Tape) :-
            perform(q0, [], Ls, Tape0, Rs),
            reverse(Ls, Ls1),
            append(Ls1, Rs, Tape).

        perform(qf, Ls, Ls, Rs, Rs) :- !.
        perform(Q0, Ls0, Ls, Rs0, Rs) :-
            symbol(Rs0, Sym, RsRest),
            once(rule(Q0, Sym, Q1, NewSym, Action)),
            action(Action, Ls0, Ls1, [NewSym|RsRest], Rs1),
            perform(Q1, Ls1, Ls, Rs1, Rs).

        symbol([], b, []).
        symbol([Sym|Rs], Sym, Rs).

        action(left, Ls0, Ls, Rs0, Rs) :- left(Ls0, Ls, Rs0, Rs).
        action(stay, Ls, Ls, Rs, Rs).
        action(right, Ls0, [Sym|Ls0], [Sym|Rs], Rs).

        left([], [], Rs0, [b|Rs0]).
        left([L|Ls], Ls, Rs, [L|Rs]).
        A simple example Turing machine is specified by the facts:

        rule(q0, 1, q0, 1, right).
        rule(q0, b, qf, 1, stay).
        This machine performs incrementation by one of a number in unary encoding: It loops over any number of "1" cells and appends an additional "1" at the end. Example query and result:

        ?- turing([1,1,1], Ts).
        Ts = [1, 1, 1, 1] ;

        This illustrates how any computation can be expressed declaratively as a sequence of state transitions, implemented in Prolog as a relation between successive states of interest.
        */

        NAR n = NARS.tmp();
        n.termVolumeMax.set(100);
        n.log();
        n.input("( perform(q0, (), $Ls, $Tape0, $Rs) ==> turing($Tape0, concat(reverse($Ls), $Rs))).");
        n.input("((&&, symbol($Rs0, #Sym, #RsRest),once(rule($Q0, #Sym, #Q1, #NewSym, #Action)),action(#Action, $Ls0, #Ls1, concat(#NewSym,#RsRest), #Rs1),perform(#Q1, #Ls1, $Ls, #Rs1, $Rs)) ==> perform($Q0, $Ls0, $Ls, $Rs0, $Rs)).");
        n.input("symbol((), b, ()).\n" +
        "        symbol(concat(#Sym,#Rs), #Sym, #Rs).\n" +
                '\n' +
        "        (left($Ls0, $Ls, $Rs0, $Rs) ==> action(#left, $Ls0, $Ls, $Rs0, $Rs)).\n" +
        "        action(stay, #Ls, #Ls, #Rs, #Rs).\n" +
        "        action(right, #Ls0, concat(#Sym,#Ls0), concat(#Sym,#Rs), #Rs).\n" +
                '\n' +
        "        left((), (), #Rs0, concat(b,#Rs0)).\n" +
        "        left(concat(#L,#Ls), #Ls, #Rs, concat(#L,#Rs)).\n" +


                '\n' +
        "        rule(q0, 1, q0, 1, right).\n" +
        "        rule(q0, b, qf, 1, stay).\n" +


                '\n' +
        "        turing((1,1,1), #Ts)?\n");
        n.run(100);

    }


}

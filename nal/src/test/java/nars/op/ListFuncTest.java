package nars.op;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.eval.Evaluation;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;

abstract class ListFuncTest {
    
    protected final NAR n = NARS.shell();

    static class AppendTest extends ListFuncTest {

        @Test
        void testAppendTransform() {


            assertEquals(
                    Set.of($$("(x,y)")),
                    Evaluation.eval($$("append((x),(y))"), n));
            assertEquals(
                    Set.of($$("append(#x,(y))")),
                    Evaluation.eval($$("append(#x,(y))"), n));

        }

        @Test
        void testAppendResult() {


            assertEquals(
                    Set.of($$("append((x),(y),(x,y))")),
                    Evaluation.eval($$("append((x),(y),#what)"), n));


            assertEquals(
                    Set.of($$("(append((x),(y),(x,y)) && ((x,y)<->solution))")),
                    Evaluation.eval($$("(append((x),(y),#what) && (#what<->solution))"), n));

        }


        @Test
        void testTestResult() {


            assertEquals(
                    Set.of($$("append((x),(y),(x,y))")),
                    Evaluation.eval($$("append((x),(y),(x,y))"), n));

            assertEquals(
                    Set.of($$("append(x,y,(x,y))")),
                    Evaluation.eval($$("append(x,y,(x,y))"), n));

            assertEquals(
                    Set.of(),
                    Evaluation.eval($$("append((x),(y),(x,y,z))"), n));

        }

        @Test
        void testAppendTail() {


            assertEquals(
                    Set.of($$("append((x),(y),(x,y))")),
                    Evaluation.eval($$("append((x),#what,(x,y))"), n));


            assertEquals(
                    Set.of($$("append(x,(y),(x,y))")),
                    Evaluation.eval($$("append(x,#what,(x,y))"), n));

        }

        @Test
        void appendNoSolution() {
            assertEquals(
                    Set.of(),
                    Evaluation.eval($$("append((z),#what,(x,y))"), n));
        }

        @Test
        void appendEmptyListIsOnlySolution() {

            assertEquals(
                    Set.of($$("(append((x),(),(x)) && (()<->solution))")),
                    Evaluation.eval($$("(append((x),#what,(x)) && (#what<->solution))"), n));

        }

        @Test
        void testAppendHeadAndTail() {
            assertEquals(
                    Set.of(
                            $$("append((x,y,z),(),(x,y,z))"),
                            $$("append((x,y),(z),(x,y,z))"),
                            $$("append((x),(y,z),(x,y,z))"),
                            $$("append((),(x,y,z),(x,y,z))")
                    ),
                    Evaluation.eval($$("append(#x,#y,(x,y,z))"), n));
        }

        @Test
        void testAppendHeadAndTailMulti1() {


            assertEquals(
                    Set.of(
                            $$("(append((),(x,y),(x,y)),append((a),(b),(a,b)))"),
                            $$("(append((x),(y),(x,y)),append((),(a,b),(a,b)))"),
                            $$("(append((),(x,y),(x,y)),append((a,b),(),(a,b)))"),
                            $$("(append((x),(y),(x,y)),append((a,b),(),(a,b)))"),
                            $$("(append((x,y),(),(x,y)),append((a,b),(),(a,b)))"),
                            $$("(append((),(x,y),(x,y)),append((),(a,b),(a,b)))"),
                            $$("(append((x),(y),(x,y)),append((a),(b),(a,b)))"),
                            $$("(append((x,y),(),(x,y)),append((a),(b),(a,b)))"),
                            $$("(append((x,y),(),(x,y)),append((),(a,b),(a,b)))")
                    ),
                    Evaluation.eval($$("(append(#x,#y,(x,y)), append(#a,#b,(a,b)))"), n));

        }

        @Test
        void testAppendHeadAndTailMulti2() {
            assertEquals(
                    Set.of(
                            $$("(append((),(x,y),(x,y)),append((),(x,b),(x,b)))"),
                            $$("(append((x),(y),(x,y)),append((x),(b),(x,b)))")
                    ),
                    Evaluation.eval($$("(append(#x,#y,(x,y)), append(#x,#b,(x,b)))"), n));
        }

        @Test
        void testAppendHeadAndTailMulti3() {
            assertEquals(
                    Set.of(
                            $$("(append((),(x,y),(x,y)) && append((),(x,b),(x,b)))"),
                            $$("(append((x),(y),(x,y)) && append((x),(b),(x,b)))")
                    ),
                    Evaluation.eval($$("(&&,append(#x,#y,(x,y)),append(#a,#b,(x,b)),equal(#x,#a))"), n));

        }

        @Test
        void testAppendHead() {


            assertEquals(
                    Set.of($$("append((x),(y),(x,y))")),
                    Evaluation.eval($$("append(#what,(y),(x,y))"), n));

            assertEquals(
                    Set.of($$("append((),(x,y),(x,y))")),
                    Evaluation.eval($$("append(#what,(x,y),(x,y))"), n));

        }

        @Disabled
        @Test
        void testHanoi() throws Narsese.NarseseException {
            /*
            http:
            An analytic solution to the Towers of Hanoi

            In the case of the Towers of Hanoi, there is a simple analytic solution based on the following observation: suppose we are able to solve the problem for n –1 disks, then we can solve it for n disks also: move the upper n –1 disks from the left to the middle peg [12] , move the remaining disk on the left peg to the right peg, and move the n –1 disks from the middle peg to the right peg. Since we are able to solve the problem for 0 disks, it follows by complete induction that we can solve the problem for any number of disks. The inductive nature of this argument is nicely reflected in the following recursive program:

              :-op(900,xfx,to).

              % hanoi(N,A,B,C,Moves) <-Moves is the list of moves to
              %                        move N disks from peg A to peg C,
              %                        using peg B as intermediary peg

              hanoi(0,A,B,C,[]).

              hanoi(N,A,B,C,Moves):-
                N1 is N-1,
                hanoi(N1,A,C,B,Moves1),
                hanoi(N1,B,A,C,Moves2),
                append(Moves1,[A to C|Moves2],Moves).

            For instance, the query ?-hanoi(3,left,middle,right,M) yields the answer

              M = [left to right, left to middle, right to middle,
            left to right,
            middle to left, middle to right, left to right ]

            The first three moves move the upper two disks from the left to the middle peg, then the largest disk is moved to the right peg, and again three moves are needed to move the two disks on the middle peg to the right peg.
             */

            NAR n = NARS.tmp();
            n.termVolumeMax.set(48);

            int levels =
                    2;


            //n.log();
            n.input("hanoi(0,#A,#B,#C,()).");
            n.input(
                    "((&&" +
                            ",hanoi(add($N,-1),$A,$C,$B,#Moves1)" +
                            ",hanoi(add($N,-1),$B,$A,$C,#Moves2)" +
                            ",append(#Moves1,append(to($A,$C),#Moves2),$Moves)) ==> hanoi($N,$A,$B,$C,$Moves)).");

            for (; levels > 0; levels--)
                n.input("hanoi(" + levels + ",left,middle,right,#M)?");

            n.run(2000);

            /*


            hanoi(N,A,B,C,Moves):-
                    N1 is N-1,
                    hanoi(N1,A,C,B,Moves1),
                    hanoi(N1,B,A,C,Moves2),
                    append(Moves1,[A to C|Moves2],Moves).
            */
        }


    }

    static class ReverseTest extends ListFuncTest {

        @Test
        void testReverseForwards() {
            assertEquals(
                    Set.of($$("reverse((x,y),(y,x))")),
                    Evaluation.eval($$("reverse((x,y),#1)"), n));
        }

        @Test
        void testReverseInline() {
            assertEquals(
                    Set.of($$("(y,x)")),
                    Evaluation.eval($$("reverse((x,y))"), n));
        }

        @Test
        void testReverseBackwards() {
            assertEquals(
                    Set.of($$("reverse((y,x),(x,y))")),
                    Evaluation.eval($$("reverse(#1,(x,y))"), n));
        }

        @Test
        void testReverseTrue() {

            assertEquals(
                    Set.of($$("reverse((y,x),(x,y))")),
                    Evaluation.eval($$("reverse((y,x),(x,y))"), n));
        }

        @Test
        void testReverseFalse() {

            assertEquals(
                    Set.of($$("(--,reverse((x,y),(x,y)))")),
                    Evaluation.eval($$("reverse((x,y),(x,y))"), n));

        }

        @Test
        void testReverseReverse() {
            assertEquals(
                    Set.of($$("(reverse((x,y),(y,x))&&reverse((y,x),(x,y)))")),
                    Evaluation.eval($$("(reverse((x,y),#1) && reverse(#1,#2))"), n));
        }

        @Test
        void testReverseStatement() {
            assertEquals(
                    Set.of($$("(y-->x)")),
                    Evaluation.eval($$("reverse((x-->y))"), n));
            assertEquals(
                    Set.of($$("(y==>x)")),
                    Evaluation.eval($$("reverse((x==>y))"), n));
        }

    }

    static class MemberTest extends ListFuncTest {
        //TODO
    }
    static class SubTest extends ListFuncTest {
        //TODO
    }
    static class SubsTest extends ListFuncTest {
        //TODO
    }
}

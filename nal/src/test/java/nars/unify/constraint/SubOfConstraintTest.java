package nars.unify.constraint;

import nars.$;
import nars.term.Term;
import nars.term.util.Conj;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.subterm.util.SubtermCondition.Event;
import static nars.subterm.util.SubtermCondition.EventLast;
import static nars.term.util.TermTest.assertEq;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubOfConstraintTest {

    @Test
    void testEventOf_Commutive() {

        SubOfConstraint c = new SubOfConstraint($.varPattern(1), $.varPattern(2), Event, +1);
        assertTrue(c.valid($$("(x&&y)"), $$("x")));
        assertTrue(!c.valid($$("(x&&y)"), $$("z")));

        assertTrue(c.valid($$("(x &&+1 (y&|z))"), $$("(y&|z)")));
        assertTrue(c.valid($$("(x &&+1 (y&|z))"), $$("x")));
        assertTrue(c.valid($$("(x &&+1 (y&|z))"), $$("y")));

    }

    @Test
    void testLastEventOf_Commutive() {

        SubOfConstraint c = new SubOfConstraint($.varPattern(1), $.varPattern(2), EventLast, +1);
        assertTrue(!c.invalid(
                (Term) $$("(x&&y)"),
                (Term) $$("x")));
        assertTrue(!c.invalid(
                (Term) $$("(x&|y)"),
                (Term) $$("x")));
        assertTrue(c.invalid(
                (Term) $$("(x&|y)"),
                (Term) $$("z")));

        assertTrue(c.invalid(
                (Term) $$("((--,((_1-->_2)&|(_3-->_2)))&|_4(_2))"),
                (Term) $$("((_1-->_2)&&(_3-->_2))")));

//        assertTrue(!c.invalid(
//                (Term)$$("(((_1-->_2)&|(_3-->_2))&|_4(_2))"),
//                (Term)$$("((_1-->_2)&|(_3-->_2))")));
//        assertTrue(!c.invalid(
//                (Term)$$("(((_1-->_2)&|(_3-->_2))&|_4(_2))"),
//                (Term)$$("((_1-->_2)&&(_3-->_2))")));
    }

    @Test
    void testLastEventOf_Seq() {

        SubOfConstraint c = new SubOfConstraint($.varPattern(1), $.varPattern(2), EventLast, +1);
        assertTrue(c.invalid(
                (Term) $$("(x &&+1 y)"),
                (Term) $$("x")));
        assertTrue(!c.invalid(
                (Term) $$("(x &&+1 y)"),
                (Term) $$("y")));


        {
            Term a = $$("(x &&+1 (y&&z))");
            Term b = $$("y");
            assertEq("(x &&+1 z)", Conj.withoutEarlyOrLate(a, b, false, false));
            assertTrue(!c.invalid(
                    a,
                    b));
        }

        assertTrue(!c.invalid(
                (Term) $$("(x &&+1 (y&|z))"),
                (Term) $$("y")));
        assertTrue(!c.invalid(
                (Term) $$("(x &&+1 (y&|z))"),
                (Term) $$("(y&|z)")));

//        assertTrue(!c.invalid(
//                (Term) $$("(x &&+1 (y&&z))"),
//                (Term) $$("(y&&z)")));
//

    }

    @Test
    void testLastEventOfNeg_Commutive() {

        SubOfConstraint c = new SubOfConstraint($.varPattern(1), $.varPattern(2), EventLast, -1);
        assertTrue(!c.invalid(
                (Term) $$("(--x&&y)"),
                (Term) $$("x")));

    }
}
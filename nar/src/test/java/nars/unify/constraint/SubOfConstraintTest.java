package nars.unify.constraint;

import nars.$;
import org.junit.jupiter.api.Test;

import static nars.$.*;
import static nars.subterm.util.SubtermCondition.Event;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubOfConstraintTest {

    @Test
    void testEventOf_Commutive() {

        SubOfConstraint c = new SubOfConstraint($.INSTANCE.varPattern(1), $.INSTANCE.varPattern(2), Event, +1);
		assertTrue(c.valid(INSTANCE.$$("(x&&y)"), INSTANCE.$$("x"), null));
		assertTrue(!c.valid(INSTANCE.$$("(x&&y)"), INSTANCE.$$("z"), null));

		assertTrue(c.valid(INSTANCE.$$("(x &&+1 (y&|z))"), INSTANCE.$$("(y&|z)"), null));
		assertTrue(c.valid(INSTANCE.$$("(x &&+1 (y&|z))"), INSTANCE.$$("x"), null));
		assertTrue(c.valid(INSTANCE.$$("(x &&+1 (y&|z))"), INSTANCE.$$("y"), null));

    }

//    @Test
//    void testLastEventOf_Commutive() {
//
//        SubOfConstraint c = new SubOfConstraint($.varPattern(1), $.varPattern(2), EventLast, +1);
//        assertTrue(!c.invalid(
//                (Term) $$("(x&&y)"),
//                (Term) $$("x")));
//        assertTrue(!c.invalid(
//                (Term) $$("(x&|y)"),
//                (Term) $$("x")));
//        assertTrue(c.invalid(
//                (Term) $$("(x&|y)"),
//                (Term) $$("z")));
//
//        assertTrue(c.invalid(
//                (Term) $$("((--,((_1-->_2)&|(_3-->_2)))&|_4(_2))"),
//                (Term) $$("((_1-->_2)&&(_3-->_2))")));
//
////        assertTrue(!c.invalid(
////                (Term)$$("(((_1-->_2)&|(_3-->_2))&|_4(_2))"),
////                (Term)$$("((_1-->_2)&|(_3-->_2))")));
////        assertTrue(!c.invalid(
////                (Term)$$("(((_1-->_2)&|(_3-->_2))&|_4(_2))"),
////                (Term)$$("((_1-->_2)&&(_3-->_2))")));
//    }

//    @Test
//    void testLastEventOf_Seq() {
//
//        SubOfConstraint c = new SubOfConstraint($.varPattern(1), $.varPattern(2), EventLast, +1);
//        assertTrue(c.invalid(
//                (Term) $$("(x &&+1 y)"),
//                (Term) $$("x")));
//        assertTrue(!c.invalid(
//                (Term) $$("(x &&+1 y)"),
//                (Term) $$("y")));
//
//
//        {
//            Term a = $$("(x &&+1 (y&&z))");
//            Term b = $$("y");
//            assertEq("(x &&+1 z)", Conj.withoutEarlyOrLate(a, b, false));
//            assertNull( Conj.withoutEarlyOrLate(b, a, false));
//            assertTrue(!c.invalid(
//                    a,
//                    b));
//        }
//
//        assertTrue(!c.invalid(
//                (Term) $$("(x &&+1 (y&|z))"),
//                (Term) $$("y")));
//        assertTrue(!c.invalid(
//                (Term) $$("(x &&+1 (y&|z))"),
//                (Term) $$("(y&|z)")));
//
////        assertTrue(!c.invalid(
////                (Term) $$("(x &&+1 (y&&z))"),
////                (Term) $$("(y&&z)")));
////
//
//    }
//
//    @Test
//    void testLastEventOfNeg_Commutive() {
//
//        SubOfConstraint c = new SubOfConstraint($.varPattern(1), $.varPattern(2), EventLast, -1);
//        assertTrue(!c.invalid(
//                (Term) $$("(--x&&y)"),
//                (Term) $$("x")));
//
//    }
}
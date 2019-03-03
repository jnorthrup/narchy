package nars.nal.nal4;

import nars.NAR;
import nars.NARS;
import nars.term.util.SetSectDiff;
import nars.test.NALTest;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.SECTe;
import static nars.Op.SECTi;
import static nars.term.atom.Bool.Null;
import static nars.term.util.TermTest.assertEq;

/** recursive NAL3 operations within inner products */
public class NAL4FuzzyProduct extends NALTest {
    static final int cycles = 100;

    @Override
    protected NAR nar() {
        NAR n = NARS.tmp(4);
        n.termVolumeMax.set(9);
        return n;
    }

    private static void testUnionSect() {
        assertEq("(x|y)",
                SetSectDiff.intersect(SECTi, false, $$("x"), $$("y")));
        assertEq("(x&y)",
                SetSectDiff.intersect(SECTe, true, $$("x"), $$("y")));
        assertEq("(x~y)",
                SetSectDiff.intersect(SECTi, false, $$("x"), $$("y").neg()));
        assertEq("(y~x)",
                SetSectDiff.intersect(SECTi, false, $$("x").neg(), $$("y")));
        assertEq("(x-y)",
                SetSectDiff.intersect(SECTe, true, $$("x"), $$("y").neg()));
    }
    private static void testUnionProduct() {
        assertEq("(x,(y|z))",
                SetSectDiff.intersectProd(SECTi, false, $$("(x,y)"), $$("(x,z)")));
        assertEq("(x,(y&z))",
                SetSectDiff.intersectProd(SECTe, false, $$("(x,y)"), $$("(x,z)")));

        assertEq("(x,(y-z))",
                SetSectDiff.intersectProd(SECTe, true, $$("(x,y)"), $$("(x,z)").neg()));
        assertEq(Null,
                SetSectDiff.intersectProd(SECTe, false, $$("(x,y)"), $$("(x,z)").neg()));

    }
    @Test
    void testIntersectionOfProductSubterms2() {

        testUnionSect();
        testUnionProduct();

        test
            .believe("((x,y)-->a)")
            .believe("((x,z)-->a)")
            .mustBelieve(cycles, "((x,(y&z))-->a)", 1, 0.81f)
            .mustBelieve(cycles, "((x,(y|z))-->a)", 1, 0.81f)
            .mustBelieve(cycles, "(((x,y)|(x,z))-->a)", 1.0f, 0.81f)
            .mustBelieve(cycles, "(((x,y)&(x,z))-->a)", 1.0f, 0.81f)
        ;
    }

    @Test
    void testIntersectionOfProductSubtermsRecursive() {
        test.termVolMax(15);
        test
                .believe("((x,(y,x))-->a)")
                .believe("((x,(z,x))-->a)")
                .mustBelieve(cycles, "((x,((y&z),x))-->a)", 1, 0.81f)
                .mustBelieve(cycles, "((x,((y|z),x))-->a)", 1, 0.81f)
                .mustBelieve(cycles, "(((x,(y,x))|(x,(z,x)))-->a)", 1.0f, 0.81f)
                .mustBelieve(cycles, "(((x,(y,x))&(x,(z,x)))-->a)", 1.0f, 0.81f)
        ;
    }

    @Test
    void testIntersectionOfProductSubterms1() {
        test
                .believe("((x)-->a)", 1.0f, 0.9f)
                .believe("((y)-->a)", 1.0f, 0.9f)
                .mustBelieve(cycles, "(((x&y))-->a)", 1.0f, 0.81f)
                .mustBelieve(cycles, "(((x|y))-->a)", 1.0f, 0.81f)
                .mustBelieve(cycles, "(((x)&(y))-->a)", 1.0f, 0.81f)
                .mustBelieve(cycles, "(((x)|(y))-->a)", 1.0f, 0.81f)
        ;
    }


    @Test
    void testIntersectionOfProductSubterms2ReverseIntensional() {
        test.termVolMax(11);

        test
            .believe("f((x|y),z)", 1.0f, 0.9f)
            .mustBelieve(cycles, "((x|y)-->(f,/,z))", 1.0f, 0.9f)
            .mustBelieve(cycles, "(x-->(f,/,z))", 1.0f, 0.81f)
            .mustBelieve(cycles, "(y-->(f,/,z))", 1.0f, 0.81f)
            .mustBelieve(cycles, "f(x,z)", 1.0f, 0.81f)
            .mustBelieve(cycles, "f(y,z)", 1.0f, 0.81f)
        ;

    }

}

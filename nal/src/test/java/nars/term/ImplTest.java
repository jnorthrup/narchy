package nars.term;

import nars.$;
import nars.Narsese;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.$.$$;
import static nars.Op.False;
import static nars.Op.Null;
import static nars.Op.True;
import static nars.term.TermTest.assertEq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** tests specific to implication compounds TODO */
public class ImplTest {
    @Test
    public void testInvalidImpls() {
        for (String s : new String[]{
                "(--y =|> y)",
                "(--(x &| y) =|> y)",
                "(--(--x &| y) =|> y)"
        })
            assertEquals(False, $$(s));
    }

    @Test
    void testReducibleImplFactored() {
        assertEq("((x&|y)=|>z)", "((x &| y) =|> (y &| z))");
        assertEq("((x&|y)=|>z)", "((x &| y) ==> (y &| z))");
    }

    @Test
    void testReducibleImplFactored2() {
        assertEq("((x&&y)==>z)", "((y && x) ==> (y && z))");
        assertEq("((&&,a,x,y)==>z)", "((&&, x, y, a) ==> (y && z))");
        assertEq("((y &&+1 x)=|>(z &&+1 y))", "((y &&+1 x)=|>(z &&+1 y))");
    }

    @Test
    void testReducibleImplFactoredPredShouldRemainIntact() {

        for (String cp : new String[]{"&&", "&|", " &&+- "}) {
            assertEq("((x&&y) ==>+1 (y" + cp + "z))", "((y&&x) ==>+1 (y" + cp + "z))");
            assertEq("(a ==>+1 (b &&+1 (y" + cp + "z)))", "(a ==>+1 (b &&+1 (y" + cp + "z)))");
        }


    }

    @Test
    void testReducibleImpl() {

        assertEq("(--,((--,x)==>y))", "(--x ==> (--y && --x))");

        assertEq("(x=|>y)", "(x ==>+0 (y &| x))");
        assertEq(True, "((y &| x) =|> x)");
        assertEq("(--,((--,$1)=|>#2))", "((--,$1)=|>((--,$1)&|(--,#2)))");
    }

    @Test
    void testReducibleImplConjCoNeg() {
        for (String i : new String[]{"==>", "=|>"}) {
            for (String c : new String[]{"&&", "&|"}) {
                assertEq(False, "(x " + i + " (y " + c + " --x))");
                assertEq(False, "(--x " + i + " (y " + c + " x))");
                assertEq(False, "((y " + c + " --x) " + i + " x)");
                assertEq(False, "((y " + c + " x) " + i + " --x)");
            }
        }
    }


    @Test
    void testReducibleImplParallelNeg() {
        assertEq("(--,((--,x)=|>y))", "(--x =|> (--y &| --x))");
        assertEq(True, "((--y &| --x) =|> --x)");

    }

    @Test void testInvalidCircularImpl() throws Narsese.NarseseException {
        assertNotEquals(Null, $("(x(intValue,(),1) ==>+10 ((--,x(intValue,(),0)) &| x(intValue,(),1)))"));
        assertEq("(--,(x(intValue,(),1)=|>x(intValue,(),0)))", "(x(intValue,(),1) =|> ((--,x(intValue,(),0)) &| x(intValue,(),1)))");
    }
    @Test void testInvalidCircularImpl2() throws Narsese.NarseseException {
        assertEq("(--,(x(intValue,(),1)==>x(intValue,(),0)))", "(x(intValue,(),1) ==> ((--,x(intValue,(),0)) &| x(intValue,(),1)))");
    }

    @Test
    void testImplInImplDTernal() {
        assertEq("(((--,(in))&&(happy))==>(out))", "((--,(in)) ==> ((happy)  ==> (out)))");
    }

    @Test
    void testImplInImplDTemporal() {
        assertEq("(((--,(in)) &&+1 (happy)) ==>+2 (out))", "((--,(in)) ==>+1 ((happy) ==>+2 (out)))");
    }

    @Test
    void testImplInConjPos() throws Narsese.NarseseException {
        String s = "((c==>a)&&a)";
        assertEquals(


                s,
                $.$(s).toString());
    }

    @Test
    void testImplInConjNeg() throws Narsese.NarseseException {
        String s = "((--,(c==>a))&&(--,a))";
        assertEquals(

                s,
                $.$(s).toString());
    }

    @Test
    void testImplInConj2xPos() throws Narsese.NarseseException {
        String s = "((c==>a)&&(d==>a))";
        assertEquals(

                s,
                $.$(s).toString());
    }

    @Test
    void testImplInConj2xNeg() throws Narsese.NarseseException {
        String s = "((--,(c==>a))&&(--,(d==>a)))";

        assertEquals(

                s,
                $.$(s).toString());
    }


}

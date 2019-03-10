package nars.term.var;

import nars.$;
import nars.io.IO;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.unify.UnifyAny;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.term.util.TermTest.assertEq;
import static nars.term.var.CommonVariable.common;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Created by me on 9/9/15.
 */
class CommonVariableTest {


    private static final Variable p1 = $.varDep(1);
    private static final Variable p2 = $.varDep(2);
    private static final Variable p3 = $.varDep(3);
    private static final Variable c12 = common(p1, p2);


    @Test
    void commonVariableTest1() {


        Variable p1p2 = common(p1, p2);
        assertEquals("##1#2", p1p2.toString());
        assertSerialize(p1p2);

        assertSame(p1p2, common(p1, p1p2)); //subsumed, same instance

        Variable  p2p1 = common(p2, p1);
        assertEquals("##1#2", p2p1.toString());
        assertSerialize(p2p1);

        Variable p2p3p1 = common(p2,  common(p3, p1));
        assertEquals("##1#2#3", p2p3p1.toString());
        assertSerialize(p2p3p1);

    }

    @Test
    void testInvalid() {
        assertThrows(Throwable.class, ()-> {
            Variable p1p1 = common(p1, p1);

            assertEquals("#x1y1", p1p1.toString());
        });
    }


    @Test
    void CommonVariableDirectionalityPreserved() {
        assertEquals(c12, common(p2, p1));
    }

    @Test
    void CommonVariableOfCommonVariable() {

        Variable c123 = common( c12,  p3);
        assertSerialize(c123);

        assertEquals("##1#2#3 class nars.term.var.CommonVariable", (c123 + " " + c123.getClass()));


        Variable c1232 = common(c123, p2);
        assertSerialize(c123);
        assertEquals("##1#2#3", c1232.toString());

    }

    private static void assertSerialize(Atomic c123) {
        byte[] bb = c123.bytes();
        assertEq(
                c123,
                IO.bytesToTerm(bb)
        );
    }

    @Test void testUnifyCommonVar_DepIndep() {
        UnifyAny u = new UnifyAny();
        assertTrue(
                $$("x($1,#1)").unify($$("x(#1,$1)"), u)
        );
        u.xy.values().forEach(c -> assertSerialize((CommonVariable)c));
        assertEquals("{$1=##1$1, #1=##1$1, #2=##2$2, $2=##2$2}$0", u.toString());
        System.out.println(u);
    }

}
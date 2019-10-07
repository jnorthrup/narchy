package nars.term.var;

import nars.$;
import nars.io.IO;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.unify.UnifyAny;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

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

    private static void assertSerialize(Variable x) {
        byte[] bb = ((Atomic)x).bytes();
        Term y = IO.bytesToTerm(bb);
        assertEq(
                x,
                y
        );
    }

    @Test void UnifyCommonVar_DepIndep() {
        String vx = "$", vy = "#";
        Set<String> uu = new TreeSet();
        for (int i = 0; i < 16; i++) {
            UnifyAny u = new UnifyAny();
            assertTrue(
                    $$("x(" + vx + "1," + vy + "1)").unify($$("x(" + vy + "1," + vx + "1)"), u)
            );
            uu.add(u.toString());
            u.xy.values().forEach(c -> assertSerialize((Variable)c));
            //System.out.println(u);
        }

        assertEquals(1, uu.size());
        assertEquals("[{$1=#1, $2=#2}$0]"
            ,uu.toString());

//        assertEquals(1, uu.size());
//        assertEquals(
//                "[{#1=$1, #2=$2}$0]", uu.toString());


    }

}
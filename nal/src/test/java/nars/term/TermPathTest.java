package nars.term;

import nars.$;
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.junit.jupiter.api.Test;

import static nars.$.*;
import static nars.term.util.TermTest.assertEq;
import static org.junit.jupiter.api.Assertions.assertSame;

public class TermPathTest {

    @Test
    void test1() {
        assertEq(
                "(#1-->f)",
                INSTANCE.$$("f(x)").replaceAt(ByteLists.immutable.of((byte)0), $.INSTANCE.varDep(1))
        );
    }

    @Test
    void test2() {
        assertEq(
                "f(#1)",
                INSTANCE.$$("f(x)").replaceAt(ByteLists.immutable.of((byte)0, (byte)0), $.INSTANCE.varDep(1))
        );
    }
    @Test
    void test3() {
        assertEq(
                "f(x,#1)",
                INSTANCE.$$("f(x,y)").replaceAt(ByteLists.immutable.of((byte)0, (byte)1), $.INSTANCE.varDep(1))
        );
    }
    @Test
    void testUnmodified() {
        Term x = INSTANCE.$$("x");
        Term p = $.INSTANCE.p(x);
        assertSame(
                p,
                p.replaceAt(ByteLists.immutable.of((byte)0), x)
        );
    }

}

package nars.term;

import nars.$;
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.term.util.TermTest.assertEq;
import static org.junit.jupiter.api.Assertions.assertSame;

public class TermPathTest {

    @Test
    void test1() {
        assertEq(
                "(#1-->f)",
                $$("f(x)").replaceAt(ByteLists.immutable.of((byte)0), $.varDep(1))
        );
    }

    @Test
    void test2() {
        assertEq(
                "f(#1)",
                $$("f(x)").replaceAt(ByteLists.immutable.of((byte)0, (byte)0), $.varDep(1))
        );
    }
    @Test
    void test3() {
        assertEq(
                "f(x,#1)",
                $$("f(x,y)").replaceAt(ByteLists.immutable.of((byte)0, (byte)1), $.varDep(1))
        );
    }
    @Test
    void testUnmodified() {
        Term x = $$("x");
        Term p = $.p(x);
        assertSame(
                p,
                p.replaceAt(ByteLists.immutable.of((byte)0), x)
        );
    }

}

package nars.term.buffer;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import static nars.$.*;
import static nars.term.util.TermTest.assertEq;

class TermeratorTest {

    @Test
    void subst1() {
        Termerator t = new Termerator(INSTANCE.$$("(x-->y)"));
        t.is(INSTANCE.$$("x"), INSTANCE.$$("z"));
        assertEq("(z-->y)", t.term());
    }

    @Test
    void substPermute1() {
        Termerator t = new Termerator(INSTANCE.$$("(x-->y)"));
        t.canBe(INSTANCE.$$("x"),
                INSTANCE.$$("a"), INSTANCE.$$("b"));

        assertEq("[(a-->y),(b-->y)]", Lists.newArrayList(t.iterator()).toString());
    }
    @Test
    void substPermute2() {
        Termerator t = new Termerator(INSTANCE.$$("(x-->y)"));
        t.canBe(INSTANCE.$$("x"),
                INSTANCE.$$("a"), INSTANCE.$$("b"));
        t.canBe(INSTANCE.$$("y"),
                INSTANCE.$$("c"), INSTANCE.$$("d"));

        assertEq("[(a-->c),(a-->d),(b-->c),(b-->d)]", Lists.newArrayList(t.iterator()).toString());
    }

}
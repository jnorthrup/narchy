package nars.term.buffer;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.term.util.TermTest.assertEq;

class TermeratorTest {

    @Test
    void subst1() {
        Termerator t = new Termerator($$("(x-->y)"));
        t.is($$("x"), $$("z"));
        assertEq("(z-->y)", t.term());
    }

    @Test
    void substPermute1() {
        Termerator t = new Termerator($$("(x-->y)"));
        t.canBe($$("x"),
                $$("a"), $$("b"));

        assertEq("[(a-->y),(b-->y)]", Lists.newArrayList(t.iterator()).toString());
    }
    @Test
    void substPermute2() {
        Termerator t = new Termerator($$("(x-->y)"));
        t.canBe($$("x"),
                $$("a"), $$("b"));
        t.canBe($$("y"),
                $$("c"), $$("d"));

        assertEq("[(a-->c),(a-->d),(b-->c),(b-->d)]", Lists.newArrayList(t.iterator()).toString());
    }

}
package nars.term;

import nars.$;
import nars.Narsese;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AnonTest {

    @Test
    public void testAtoms() throws Narsese.NarseseException {
        assertAnon("0", "a");
        assertAnon("#1", $.varDep(1)); //unchanged
        assertAnon("0", $.the(2)); //int remaps to internal int
    }

    @Test
    public void testCompounds() throws Narsese.NarseseException {
        assertAnon("(0-->1)", "(a-->b)");

        assertAnon("(0-->#1)", "(a-->#1)");

        assertAnon("(((0-->(1,2,#1))==>(3,4)),?2)",
                "(((a-->(b,c,#2))==>(e,f)),?1)");

    }

    static Anon assertAnon(String expect, String test) throws Narsese.NarseseException {
        return assertAnon(expect, $(test));
    }

    static Anon assertAnon(String expect, Term x) {
        Anon a = new Anon();
        Term y = a.put(x);
        Term z = a.get(y);
        assertEquals(expect, y.toString());
        assertEquals(x, z);
        return a;
    }
}
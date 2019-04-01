package nars.term;

import nars.Narsese;
import nars.Op;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static nars.$.*;
import static nars.Op.or;
import static nars.Op.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * test target hash and structure bits
 */
class TermHashTest {

    @Test
    void testStructureIsVsHas() throws Narsese.NarseseException {

        assertTrue(inh("a", "b").has(Op.ATOM));
        assertTrue(inh(p("a"), $("b"))
                .hasAny(or(Op.ATOM, Op.PROD)));

        assertFalse(inh(p("a"), $("b"))
                .isAny(or(SIM, Op.PROD)));
        assertNotSame(inh(p("a"), $("b"))
                .op(), Op.PROD);

        assertSame(inh("a", "b").op(), INH);
        assertTrue(inh("a", "b").has(INH));
        assertTrue(inh("a", "b").has(Op.ATOM));
        assertFalse(inh("a", "b").has(SIM));
    }

    @Test
    void testHasAnyVSAll() throws Narsese.NarseseException {
        @Nullable Term iii = impl(inh("a", "b"), $("c"));
        assertTrue(iii.hasAll(or(IMPL, INH)));
        assertFalse(iii.hasAll(or(IMPL, SIM)));
        assertTrue(iii.hasAny(or(IMPL, INH)));

    }














}

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

        assertTrue(INSTANCE.inh("a", "b").hasAny(Op.ATOM));
        assertTrue(INSTANCE.inh(INSTANCE.p("a"), INSTANCE.$("b"))
                .hasAny(or(Op.ATOM, Op.PROD)));

        assertFalse(INSTANCE.inh(INSTANCE.p("a"), INSTANCE.$("b"))
                .isAny(or(SIM, Op.PROD)));
        assertNotSame(INSTANCE.inh(INSTANCE.p("a"), INSTANCE.$("b"))
                .op(), Op.PROD);

        assertSame(INSTANCE.inh("a", "b").op(), INH);
        assertTrue(INSTANCE.inh("a", "b").hasAny(INH));
        assertTrue(INSTANCE.inh("a", "b").hasAny(Op.ATOM));
        assertFalse(INSTANCE.inh("a", "b").hasAny(SIM));
    }

    @Test
    void testHasAnyVSAll() throws Narsese.NarseseException {
        @Nullable Term iii = INSTANCE.impl(INSTANCE.inh("a", "b"), INSTANCE.$("c"));
        assertTrue(iii.hasAll(or(IMPL, INH)));
        assertFalse(iii.hasAll(or(IMPL, SIM)));
        assertTrue(iii.hasAny(or(IMPL, INH)));

    }














}

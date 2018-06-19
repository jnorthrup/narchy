package nars.term.compound;

import nars.Op;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LazyCompoundTest {

    @Test
    void testSimple() {
        assertEquals("(a,b)", new LazyCompound()
                .compound(Op.PROD, $$("a"), $$("b")).get().toString());
    }
    
    @Test
    void testTemporal() {
        assertEquals("(a==>b)", new LazyCompound()
                .compound(Op.IMPL, $$("a"), $$("b")).get().toString());

        assertEquals("(a ==>+1 b)", new LazyCompound()
                .compound(Op.IMPL, 1, $$("a"), $$("b")).get().toString());
    }
    @Test
    void testCompoundInCompound() {
        assertEquals("(a,{b,c})", new LazyCompound()
                .compound(Op.PROD, (byte)2).add($$("a"))
                    .compound(Op.SETe, (byte)2).addAll($$("b"), $$("c"))
                        .get().toString());
    }
}
package nars.term.var;

import nars.$;
import nars.term.Variable;
import org.junit.jupiter.api.Test;

import static nars.Op.VAR_DEP;
import static org.junit.jupiter.api.Assertions.*;

class SpecialOpVariableTest {

    @Test
    void test1() {
        Variable i = $.INSTANCE.varIndep(1);
        Variable d = $.INSTANCE.varDep(1);
        SpecialOpVariable s = new SpecialOpVariable(i, VAR_DEP);
        UnnormalizedVariable u = new UnnormalizedVariable(VAR_DEP, "#$1");

        assertEquals("#$1", s.toString());
        assertTrue(!s.equals(i));
        assertTrue(!s.equals(d));

        assertArrayEquals(s.bytes(), u.bytes());
        assertEquals(s, u);


    }
}
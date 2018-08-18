package nars.concept.dynamic;

import nars.NAR;
import nars.NARS;
import nars.table.dynamic.DynamicTruthTable;
import nars.term.Term;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractDynamicTaskTest {
    protected final NAR n = NARS.shell();

    protected boolean isDynamicTable(String t) {
        return isDynamicTable($$(t));
    }

    protected boolean isDynamicTable(Term $$) {
        return n.conceptualize($$).beliefs().tableFirst(DynamicTruthTable.class) != null;
    }
    protected void assertDynamicTable(String s) {
        assertTrue(isDynamicTable(s));
    }
    protected void assertDynamicTable(Term t) {
        assertTrue(isDynamicTable(t));
    }
}

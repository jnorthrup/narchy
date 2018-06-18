package nars.op;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by me on 3/30/17.
 */
class DepIndepVarIntroductionTest {

    private final NAR n = NARS.shell();

    @Test
    void testIntroduceIndepVar() throws Narsese.NarseseException {

        assertEquals("[((a-->$X)==>(b-->$X))]",
                introduce("((a-->c)==>(b-->c))", 16).toString());

        assertEquals("[((a-->$X)=|>(b-->$X))]",
                introduce("((a-->c)=|>(b-->c))", 16).toString());
    }

    @Test
    void testIntroduceIndepVar2() throws Narsese.NarseseException {
        assertEquals("[((a-->($X,#1))=|>(b-->($X,#1))), ((a-->$X)=|>(b-->$X))]",
                introduce("((a-->(x,#1))=|>(b-->(x,#1)))", 16).toString());
    }

    @Test
    void testIntroduceDepVar() throws Narsese.NarseseException {

        assertEquals("[((a-->#Y)&&(b-->#Y))]",
                introduce("(&&,(a-->c),(b-->c))", 16).toString());

        assertEquals("[((#Y-->a)&&(#Y-->b))]",
                introduce("(&&,(c-->a),(c-->b))", 16).toString());

        assertEquals("[(&&,(c-->#Y),(#Y-->a),(#Y-->b))]",
                introduce("(&&,(x-->a),(x-->b),(c-->x))", 16).toString());

        
        assertEquals("[(&&,(c-->(a,b,#Y)),(#Y-->a),(#Y-->b)), (&&,(c-->(a,#Y,x)),(x-->a),(x-->#Y)), (&&,(c-->(#Y,b,x)),(x-->b),(x-->#Y))]",
                introduce("(&&,(x-->a),(x-->b),(c-->(a,b,x)))", 32).toString());

    }

    private TreeSet<Term> introduce(String term, int iterations) {
        TreeSet<Term> s = new TreeSet();
        for (int i = 0; i < iterations; i++) {
            Term u = $.func("varIntro", $.$$(term).normalize()).eval(n, false);
            if (u!=null)
                s.add(u);
        }
        return s;
    }
}
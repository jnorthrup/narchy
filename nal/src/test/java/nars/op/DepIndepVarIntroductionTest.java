package nars.op;

import nars.$;
import nars.NAR;
import nars.NARS;
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
    void testIntroduceIndepVar() {

        assertEquals("[((a-->$1)==>(b-->$1))]",
                introduce("((a-->c)==>(b-->c))", 16).toString());

        assertEquals("[((a-->$1)=|>(b-->$1))]",
                introduce("((a-->c)=|>(b-->c))", 16).toString());
    }

    @Test
    void testIntroduceIndepVar2() {
        assertEquals("[((a-->($_v,#1))=|>(b-->($_v,#1))), ((a-->$_v)=|>(b-->$_v))]",
                introduce("((a-->(x,#1))=|>(b-->(x,#1)))", 16).toString());
    }

    @Test
    void testIntroduceDepVar() {

        assertEquals("[((a-->#1)&&(b-->#1))]",
                introduce("(&&,(a-->c),(b-->c))", 16).toString());

        assertEquals("[(((a,#1)-->#_v)&&((b,#1)-->#_v))]",
                introduce("(&&,((a,#1)-->c),((b,#1)-->c))", 16).toString());

        assertEquals("[((#1-->a)&&(#1-->b))]",
                introduce("(&&,(c-->a),(c-->b))", 16).toString());

        assertEquals("[(&&,(c-->#1),(#1-->a),(#1-->b))]",
                introduce("(&&,(x-->a),(x-->b),(c-->x))", 16).toString());

        
        assertEquals("[(&&,(c-->(a,b,#1)),(#1-->a),(#1-->b)), (&&,(c-->(a,#1,x)),(x-->a),(x-->#1)), (&&,(c-->(#1,b,x)),(x-->b),(x-->#1))]",
                introduce("(&&,(x-->a),(x-->b),(c-->(a,b,x)))", 32).toString());

    }

    private TreeSet<Term> introduce(String term, int iterations) {
        TreeSet<Term> s = new TreeSet();
        for (int i = 0; i < iterations; i++) {
            Term u = $.func("varIntro", $.$$(term).normalize()).eval(n);
            if (u!=null)
                s.add(u);
        }
        return s;
    }
}
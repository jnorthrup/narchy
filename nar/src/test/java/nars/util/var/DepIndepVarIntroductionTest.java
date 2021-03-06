package nars.util.var;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.term.Term;
import nars.term.Terms;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import static nars.$.*;
import static nars.term.util.TermTest.assertEq;
import static nars.util.var.DepIndepVarIntroduction.depIndepFilter;
import static nars.util.var.DepIndepVarIntroduction.nonNegdepIndepFilter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by me on 3/30/17.
 */
class DepIndepVarIntroductionTest {



    @Test
    void testIntroduceIndepVar() {

        assertEquals("[((a-->$1)==>(b-->$1))]",
                introduce("((a-->c)==>(b-->c))", 16).toString());

    }

    @Test
    void testIntroduceIndepVar2() {
        String x = "((a-->(x,#1))==>(b-->(x,#1)))";
        Term input = INSTANCE.$$(x);
        @Nullable Term[] r = Terms.nextRepeat(input.subterms(), 2, depIndepFilter);
        assertEquals(2, r.length);
        Arrays.sort(r);
        assertEq("(x,#1)", r[0]);
        assertEq("x", r[1]);


        assertEquals("[((a-->($_v,#1))==>(b-->($_v,#1))), ((a-->$_v)==>(b-->$_v))]",
                introduce(x, 128).toString());
    }

    @Test
    void testDontIntroduceIndepVarInNeg() {
        String x = "((a,--(x,#1))==>(b,--(x,#1)))";
        Term input = INSTANCE.$$(x);
        @Nullable Term[] r = Terms.nextRepeat(input.subterms(), 2, nonNegdepIndepFilter);
        assertNotNull(r);
        assertEquals(2, r.length);
        Arrays.sort(r);
        assertEq("(x,#1)", r[0]);
        assertEq("x", r[1]);

    }

    @Test
    void testSubtermScore() {
        assertEquals("{y=3, x=4}",
                Terms.subtermScore(INSTANCE.$$("((x,x,x,x),(y,y,y))"), 2, new ToIntFunction<Term>() {
                    @Override
                    public int applyAsInt(Term t1) {
                        return 1;
                    }
                }).toString());
    }

    @Test
    void testSubtermScore_Intrinsic() {
        assertEquals("{%1=4, %2=3}",
            Terms.subtermScore(INSTANCE.$$("((%1,%1,%1,%1),(%2,%2,%2))"), 2, new ToIntFunction<Term>() {
                @Override
                public int applyAsInt(Term t) {
                    return 1;
                }
            }).toString());
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

    private final NAR n = NARS.shell();

    private SortedSet<Term> introduce(String term, int iterations) {
        Supplier<SortedSet> collectionFactory = TreeSet::new;
        SortedSet sortedSet = collectionFactory.get();
        for (int i = 0; i < iterations; i++) {
            Term varIntro = n.eval($.INSTANCE.func("varIntro", INSTANCE.$$(term).normalize()));
            if (varIntro != null) {
                sortedSet.add(varIntro);
            }
        }
        return(SortedSet<Term>) sortedSet;
    }
}
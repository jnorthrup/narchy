package nars.term.util.builder;

import jcog.WTF;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.util.TermTest;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * compares a TermBuilder impl's results against HeapTermBuilder
 */
public class VerifyingTermBuilder extends TermBuilder {

    private final TermBuilder a, b;

    public VerifyingTermBuilder(TermBuilder a, TermBuilder b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public Term compound(Op o, int dt, Term... u) {
        Term aa = a.compound(o, dt, u);
        Term bb = b.compound(o, dt, u);
        if (!equals(aa, bb)) {
            equals(aa, bb); //temporary for re-debugging
//            Term aaa = a.compound(o, dt, u); //temporary for re-debugging
//            Term bbb = b.compound(o, dt, u); //temporary for re-debugging
            throw new WTF(o + " " + Arrays.toString(u) + " dt=" + dt + " inequal:\n" + aa + "\n" + bb);
        }
        return aa;
    }

    @Override
    protected Subterms subterms(@Nullable Op inOp, Term... u) {
        Subterms aa = a.subterms(inOp, u);
        Subterms bb = b.subterms(inOp, u);
        if (!equals(aa, bb))
            throw new WTF(Arrays.toString(u) + (inOp != null ? " (inOp=" + inOp + ") " : "") + " inequal:\n" + aa + "\n" + bb);
        return aa;
    }

    protected boolean equals(Term x, Term y) {
        try {
            TermTest.assertEq(x, y);
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    protected boolean equals(Subterms x, Subterms y) {
        try {
            TermTest.assertEq(x, y);
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

}

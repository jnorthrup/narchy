package nars.term.buffer;

import jcog.data.list.FasterList;
import nars.$;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.compound.LightCompound;
import nars.term.util.transform.AbstractTermTransform;
import nars.term.util.transform.InlineFunctor;
import nars.term.util.transform.InstantFunctor;
import nars.term.util.transform.TermTransform;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

import static nars.Op.INH;
import static nars.Op.PROD;
import static nars.term.atom.Bool.Null;

public class EvalTermBuffer extends TermBuffer {

    private final FasterList<DeferredEval> eval = new FasterList();
    public boolean evalInline = true;

    @Override
    public void clear(boolean code, boolean uniques) {
        super.clear(code, uniques);
        if (code) {
            this.eval.clear();
        }
    }

    private static final TermTransform DeferredEvaluator = new AbstractTermTransform.NegObliviousTermTransform() {
        @Override
        protected Term applyPosCompound(Compound x) {
            if (x instanceof DeferredEval) {
                DeferredEval e = (DeferredEval) x;


                Term y = e.eval();
                if (y instanceof Bool)
                    return y;
                else
                    return apply(y); //recurse
            } else
                return super.applyPosCompound(x);
        }

        @Override
        public boolean evalInline() {
            return true;
        }
    };

    public final TermBuffer evalInline(boolean b) {
        this.evalInline = b;
        return this;
    }
    private static final class DeferredEval extends LightCompound {
        //private static final class DeferredEval extends LighterCompound {

        final static AtomicInteger serial = new AtomicInteger(0);

        /** https://unicode-table.com/en/1F47E/ */
        final static Atom DeferredEvalPrefix = Atomic.atom("⚛");
        //final static String DeferredEvalPrefix = ("⚛");

        private final InlineFunctor f;
        private Subterms args;

        /** cached value, null if not computed yet */
        private transient Term value = null;

        DeferredEval(InlineFunctor f, Subterms args) {
            super(PROD, DeferredEvalPrefix , Int.the(serial.incrementAndGet()));
            this.f = f;
            this.args = args;
        }


        @Override
        public @Nullable Term normalize(byte varOffset) {
            return null;
        }

        @Override
        public boolean isNormalized() {
            return true;
        }

        @Override
        public String toString() {
            return "(" + sub(0) + "=" + f + "(" + args + "))";
        }

        public final Term eval() {
            if (this.value!=null) {
                return this.value; //cached
            } else {

                Term argsY = DeferredEvaluator.apply($.pFast(args)); //recurse apply to arguments before eval
                Term e;
                if (argsY == Null)
                    e = Null;
                else {
                    e = f.applyInline(this.args = ((Subterms)argsY));
                    if (e == null)
                        e = Null; //HACK
                }
                return this.value = e;
            }
        }
    }

    /** adds a deferred evaluation */
    private Term eval(Term[] s) {

        InlineFunctor func = (InlineFunctor) s[1];
        Subterms args = s[0].subterms();

        boolean deferred = !(func instanceof InstantFunctor);

        if (deferred) {
            DeferredEval e = new DeferredEval(func, args);
            eval.add(e); //TODO check for duplicates?
            return e;
        } else {

            Term e = func.applyInline(args);
            if (e == null)
                e = Null;
            return e;
        }
    }

    @Override
    public Term get(int volMax) {
        Term y = super.get(volMax);

        if (y instanceof Compound && !eval.isEmpty()) {
            y = DeferredEvaluator.apply(y);
        }

        return y;
    }

    protected boolean evalInline() {
        return evalInline;
    }

    protected Term nextCompound(Op op, int dt, Term[] subterms, byte[] bytes, int[] range, int start) {
        if (op == INH && evalInline() && subterms[1] instanceof InlineFunctor && subterms[0].op() == PROD) {

            return eval(subterms);

        }
        return super.nextCompound(op, dt, subterms, bytes, range, start);
    }

}

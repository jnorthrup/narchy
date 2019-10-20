package nars.term.buffer;

import jcog.data.list.FasterList;
import nars.$;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.theBool;
import nars.term.atom.theInt;
import nars.term.compound.LightCompound;
import nars.term.util.transform.InlineFunctor;
import nars.term.util.transform.InstantFunctor;
import nars.term.util.transform.RecursiveTermTransform;
import nars.term.util.transform.TermTransform;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

import static nars.Op.INH;
import static nars.Op.PROD;
import static nars.term.atom.theBool.Null;

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

    private static final TermTransform DeferredEvaluator = new RecursiveTermTransform.NegObliviousTermTransform() {
        @Override
		public Term applyPosCompound(Compound x) {
            if (x instanceof DeferredEval) {
                var e = (DeferredEval) x;


                var y = e.eval();
                //recurse
                return y instanceof theBool ? y : apply(y);
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

        static final AtomicInteger serial = new AtomicInteger(0);

        /** https://unicode-table.com/en/1F47E/ */
        static final Atom DeferredEvalPrefix = Atomic.atom("⚛");
        //final static String DeferredEvalPrefix = ("⚛");

        private final InlineFunctor f;
        private Subterms args;

        /** cached value, null if not computed yet */
        private transient Term value = null;

        DeferredEval(InlineFunctor f, Subterms args) {
            super(PROD, DeferredEvalPrefix , theInt.the(serial.incrementAndGet()));
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

                var argsY = DeferredEvaluator.apply($.pFast(args)); //recurse apply to arguments before eval
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

        var func = (InlineFunctor) s[1];
        var args = s[0].subterms();

        var deferred = !(func instanceof InstantFunctor);

        if (deferred) {
            var e = new DeferredEval(func, args);
            eval.add(e); //TODO check for duplicates?
            return e;
        } else {

            var e = func.applyInline(args);
            if (e == null)
                e = Null;
            return e;
        }
    }

    @Override
    public Term term(int volMax) {
        var y = super.term(volMax);

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

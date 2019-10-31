package nars.unify.mutate;

import jcog.TODO;
import nars.$;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.unify.Unify;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * AIKR choicepoint used in deciding possible mutations to apply in deriving new compounds
 */
@FunctionalInterface public interface Termutator {

    /**
     * match all termutations recursing to the next after each successful one
     */
    void mutate(Termutator[] chain, int current, Unify u);

    default int getEstimatedPermutations() {
        return -1; /* unknown */
    }

    /** @return null to terminate the entire chain (CUT);
     * this instance for no change
     * or a reduced version (or NullTermutator for NOP) */
    default @Nullable Termutator preprocess(Unify u) {
        return this;
    }

    abstract class AbstractTermutator extends ProxyTerm implements Termutator {

        AbstractTermutator(Atom klass, Term... keyComponents) {
            super($.pFast(klass, keyComponents.length == 1 ? keyComponents[0] :
                    $.pFast(keyComponents)));
        }

        @Override
        public final boolean the() {
            return false;
        }

        @Override
        public String toString() {
            return ref.toString();
        }

        @Override
        public final Subterms subterms() {
            return ref.subterms();
        }

        @Override
        public Subterms subtermsDirect() {
            return ((Compound)ref).subtermsDirect();
        }

        @Override
        public final int dt() {
            return ref.dt();
        }

        @Override
        public Op op() {
            return ref.op();
        }

        @Override
        public Term unneg() {
            return ifDifferentElseThis(ref.unneg());
        }

        @Override
        public   Term ifDifferentElseThis(Term u) {
            //continue proxying
            return u == ref ? this : u;
        }

        @Override
        public @Nullable Term replaceAt(ByteList path, Term replacement) {
            throw new TODO();
        }

        @Override
        public @Nullable Term replaceAt(ByteList path, int depth, Term replacement) {
            throw new TODO();
        }

        @Override
        public @Nullable Term normalize(byte varOffset) {
            return ifDifferentElseThis(ref.normalize(varOffset));
        }

        @Override
        public int volume() {
            return ref.volume();
        }

        @Override
        public int complexity() {
            return ref.complexity();
        }

        @Override
        public int structure() {
            return ref.structure();
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof ProxyTerm)
                o = ((ProxyTerm)o).ref;
    //        if (o instanceof Termed)
    //            o = ((Termed)o).term();
            return ref.equals(o);
        }

        @Override
        public int hashCode() {
            return ref.hashCode();
        }

        @Override
        public Term root() {
            return ifDifferentElseThis(ref.root());
        }

        @Override
        public Term concept() {
            return ifDifferentElseThis(ref.concept());
        }

        @Override
        public boolean isCommutative() {
            return ref.isCommutative();
        }

        @Override
        public void appendTo(Appendable w) throws IOException {
            ref.appendTo(w);
        }

        @Override
        public boolean isNormalized() {
            return ref.isNormalized();
        }

        @Override
        public Term sub(int i) {
            return ref.sub(i);
        }

        @Override
        public int subs() {
            return ref.subs();
        }

        @Override
        public final int compareTo(Term t) {
            return ref.compareTo(t);
        }

        @Override
        public int vars() {
            return ref.vars();
        }

        @Override
        public int varIndep() {
            return ref.varIndep();
        }

        @Override
        public int varDep() {
            return ref.varDep();
        }

        @Override
        public int varQuery() {
            return ref.varQuery();
        }

        @Override
        public int varPattern() {
            return ref.varPattern();
        }
    }

    /** constant result for return from preprocess() call
     * */
    static Termutator result(boolean success) {
        return success ? Termutator.ELIDE : null;
    }

    Termutator[] CUT = new Termutator[0];

    Termutator ELIDE = new AbstractTermutator(Atomic.atom("ELIDE")) {
        @Override public void mutate(Termutator[] chain, int current, Unify u) {
            u.tryMutate(chain, current);
        }
    };
}

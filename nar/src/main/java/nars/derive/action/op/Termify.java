package nars.derive.action.op;

import jcog.TODO;
import nars.$;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.util.transform.Retemporalize;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Derivation target construction step of the derivation process that produces a derived task
 * <p>
 * Each rule corresponds to a unique instance of this
 * <p>
 * runtime instance. each leaf of each NAR's derivation tree should have
 * a unique instance, assigned the appropriate cause id by the NAR
 * at initialization.
 */
public final class Termify extends ProxyTerm {

    /** conclusion template */
    public final Term pattern;

    /** fully eternalized conclusion template for completely non-temporal premises */
    private final Term patternEternal;

    private static final Atom TERMIFY = Atomic.atom(Termify.class.getSimpleName());

    public Termify(Term pattern, Truthify truth) {
        super($.func(TERMIFY, pattern, truth.ref));

        this.pattern = pattern;

        this.patternEternal = Retemporalize.retemporalizeXTERNALToDTERNAL.apply(pattern);

//        if (!(pattern.equals(patternEternal) || pattern.root().equals(patternEternal.root())))
//            throw new TermTransformException(pattern, patternEternal, "pattern eternalization mismatch");
    }

    public final Term pattern(boolean temporal) {
        return temporal ? pattern : patternEternal;
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
    public final Term ifDifferentElseThis(Term u) {
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

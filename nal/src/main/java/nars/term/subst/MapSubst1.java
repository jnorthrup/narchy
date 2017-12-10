package nars.term.subst;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 1-pair substitution
 */
public class MapSubst1 implements Subst {

    private final Term from;
    private final Term to;

    public MapSubst1(Map.Entry<Term,Term> e) {
        this(e.getKey(), e.getValue());
    }

    /**
     * creates a substitution of one variable; more efficient than supplying a Map
     */
    public MapSubst1(/*@NotNull*/ Term from, /*@NotNull*/ Term to) {
        //assert(!from.equals(to)): "pointless substitution";
        if (from.equals(to))
            throw new RuntimeException("pointless substitution");

        this.from = from;
        this.to = to;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public final Term transform(Compound x, Op op, int dt) {
        if (x.equals(from))
            return to;
        else
            return !x.impossibleSubTerm(from) ? Subst.super.transform(x, op, dt) : x;
    }

    @Override
    public @Nullable Term xy(Term t) {
        return t.equals(from) ? to : null;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

//    @Override
//    public boolean put(/*@NotNull*/ Unify copied) {
//        throw new UnsupportedOperationException();
//    }
}

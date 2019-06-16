package nars.term.anon;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.var.NormalizedVariable;

/** the shift is reset immediately after a putShift, so that calling put() after will involve no shift */
public class AnonWithVarShift extends CachedAnon {

    /** structure mask with bits enabled for what variables are to be shifted */
    final int mask;

    /** enable */
    boolean shifting = false, hasShifted = false;

    /** offsets */
    int indepShift = 0, depShift = 0, queryShift = 0;

//    private int mustAtomize = Integer.MAX_VALUE;

    public AnonWithVarShift(int cap, int variableStructure) {
        super(cap);
        this.mask = variableStructure;
    }

//    public void mustAtomize(int mustAtomize) {
//        this.mustAtomize = mustAtomize;
//    }

//    @Override
//    protected boolean intern(Term x) {
//        return super.intern(x) || (mustAtomize!=Integer.MAX_VALUE && !x.hasAny(mustAtomize));
//    }

    @Override
    protected void invalidate() {
        super.invalidate();
        hasShifted = false;
    }

    @Override
    protected Term putAnon(Term x) {
        if (shifting && (x instanceof NormalizedVariable)) {
            Op o = x.op();
            if (o.isAny(mask)) {
                int shift;
                switch (o) {
                    case VAR_DEP:
                        shift = depShift;
                        break;
                    case VAR_INDEP:
                        shift = indepShift;
                        break;
                    case VAR_QUERY:
                        shift = queryShift;
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
                if (shift != 0) {
                    NormalizedVariable v = ((NormalizedVariable) x);
                    int newID = v.id() + shift;
                    assert (newID < Byte.MAX_VALUE - 3); //to be safe
                    x = v.normalizedVariable((byte) newID);
                }
            }
        }
        return super.putAnon(x);
    }



    nars.term.anon.AnonWithVarShift shift(Term t) {


        if (t.hasAny(mask)) {
            t.recurseTermsOrdered(b-> b.hasAny(mask), s -> {
                if (s instanceof NormalizedVariable) {
                    byte serial = ((NormalizedVariable) s).id();
                        switch (s.op()) {
                            case VAR_DEP:
                                depShift = Math.max(depShift, serial);
                                break;
                            case VAR_INDEP:
                                indepShift = Math.max(indepShift, serial);
                                break;
                            case VAR_QUERY:
                                queryShift = Math.max(queryShift, serial);
                                break;
                            default:
                                throw new UnsupportedOperationException();
                        }
                }
                return true;
            }, null);
            if (shifting = (depShift + indepShift + queryShift) > 0) {
                invalidate();
                hasShifted = true;
            }
        }
        return this;
    }

    @Override
    protected boolean cache(Compound x, boolean putOrGet) {
        return (!hasShifted || !x.hasAny(mask));
        //return true;
        //return false;
        //return !putOrGet || (!hasShifted || !x.hasAny(mask));
    }

    public Term putShift(Term x, Term base) {
        //TODO only shift if the variable bits overlap, but if disjoint not necessary
        if (x.hasAny(mask))
            shift(base);
        Term y = put(x);
        if (shifting) {
            depShift = indepShift = queryShift = 0;
            shifting = false;
        }
        return y;
    }


}

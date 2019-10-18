package nars.term.anon;

import nars.Op;
import nars.term.Term;
import nars.term.var.NormalizedVariable;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Predicate;

/** the shift is reset immediately after a putShift, so that calling put() after will involve no shift */
public class AnonWithVarShift extends CachedAnon {

    /** structure mask with bits enabled for what variables are to be shifted */
    final int mask;

    /** enable */
    boolean shifting = false;

    /** offsets */
    int indepShift = 0;
    int depShift = 0;
    int queryShift = 0;


    public AnonWithVarShift(int cap, int variableStructure) {
        super(cap);
        this.mask = variableStructure;
    }


    @Override
    protected Term putIntrin(Term x) {
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
                    assert (newID < Byte.MAX_VALUE - 3): "shifted normalized variable ID out of range: " + newID; //to be safe
                    x = v.normalizedVariable((byte) newID);
                }
            }
        }
        return super.putIntrin(x);
    }



    static Shiftability shiftability(Term base, int mask) {
        Shiftability s = new Shiftability();
        base.recurseTermsOrdered(b-> b.hasAny(mask), s, null);
        return s;
    }

//    @Override
//    protected boolean cache(Compound x, boolean putOrGet) {
//        return (!hasShifted || !x.hasAny(mask));
//        //return true;
//        //return false;
//        //return !putOrGet || (!hasShifted || !x.hasAny(mask));
//    }

    public Term putShift(Term x, Term base, @Nullable Random rng) {

        //TODO only shift if the variable bits overlap, but if disjoint not necessary
        if (x.hasAny(mask)) {
            int subMask = x.structure() & this.mask;
            if (subMask!=0) {
                Shiftability s = shiftability(base, subMask);
                if (rng == null) {
                    //fully disjoint: shift so any new variables do not coincide with existing
                    depShift = s.depShiftMax;
                    indepShift = s.indepShiftMax;
                    queryShift = s.queryShiftMax;
                } else {
                    //random
                    depShift = s.depShiftMax > 0 ? rng.nextInt(s.depShiftMax+1) : 0;
                    indepShift = s.indepShiftMax > 0 ? rng.nextInt(s.indepShiftMax+1) : 0;
                    queryShift = s.queryShiftMax > 0 ? rng.nextInt(s.queryShiftMax+1) : 0;
                }
                if (shifting = (depShift + indepShift + queryShift) > 0) {
                    invalidateShifted(subMask);
                }
            }
        }

        Term y = put(x);
        shiftZero();
        return y;
    }

    private void invalidateShifted(int mask) {
        boolean pe = putCache.isEmpty();
        boolean ge = getCache.isEmpty();


        //remove terms from cache that involve shifted vars
        //TODO dont remove if #1,$1 etc... ?

        if (!pe) //HACK UnifiedMap doesnt fast fail if size==1
            putCache.removeIf(this::involvesMask);
        if (!ge) //HACK UnifiedMap doesnt fast fail if size==1
            getCache.removeIf(this::involvesMask);
    }

    private boolean involvesMask(Term k, Term v) {
        return k.hasAny(mask);
    }

    private void shiftZero() {
        this.depShift = this.indepShift = this.queryShift = 0; //reset
    }

    private static class Shiftability implements Predicate<Term> {
        int depShiftMax = 0;
        int indepShiftMax = 0;
        int queryShiftMax = 0;

        @Override
        public boolean test(Term s) {
            if (s instanceof NormalizedVariable) {
                byte serial = ((NormalizedVariable) s).id();
                switch (s.op()) {
                    case VAR_DEP:
                        depShiftMax = Math.max(depShiftMax, serial);
                        break;
                    case VAR_INDEP:
                        indepShiftMax = Math.max(indepShiftMax, serial);
                        break;
                    case VAR_QUERY:
                        queryShiftMax = Math.max(queryShiftMax, serial);
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }
            return true;
        }
    }
}

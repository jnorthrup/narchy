package nars.term.compound;

import nars.Op;
import nars.The;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.transform.Retemporalize;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;

import static jcog.Util.hashCombine;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;


/**
 * on-heap, caches many commonly used methods for fast repeat access while it survives
 */
abstract public class CachedCompound extends SeparateSubtermsCompound implements The {

    /**
     * subterm vector
     */
    private final Subterms subterms;

    public final byte op;

    private final short _volume;
    private final int _structure;

    public static Compound newCompound(Op op, int dt, Subterms subterms) {

        Compound c;
        if (!op.temporal && !subterms.hasAny(Op.Temporal)) {
            //assert (dt == DTERNAL);
            if (subterms.isNormalized())
                c = new SimpleCachedCompound(op, subterms);
            else
                c = new UnnormalizedCachedCompound(op, subterms);
        } else {
//            if (subterms.sub(subterms.subs() - 1) instanceof Interval) {
//                c = new Sequence(subterms);
//            } else {
                c = new TemporalCachedCompound(op, dt, subterms);
//            }
        }

        return c;
    }

    /** non-temporal but unnormalized */
    public static class UnnormalizedCachedCompound extends CachedCompound {

        UnnormalizedCachedCompound(Op op, Subterms subterms) {
            super(op, DTERNAL, subterms);
        }

        @Override
        public final int dt() {
            return DTERNAL;
        }

        @Override
        public final Term temporalize(Retemporalize r) {
            return this;
        }

        @Override
        public final boolean hasXternal() {
            return false;
        }

        @Override
        public final int eventRange() {
            return 0;
        }

        @Override
        public final Term eventFirst() {
            return this;
        }

        @Override
        public final Term eventLast() {
            return this;
        }


        /** @see Term.eventsAND */
        @Override public final boolean eventsAND(LongObjectPredicate<Term> each, long offset, boolean decomposeConjDTernal, boolean decomposeXternal) {
            return each.accept(offset, this);
        }

        @Override
        public boolean eventsOR(LongObjectPredicate<Term> each, long offset, boolean decomposeConjDTernal, boolean decomposeXternal) {
            return each.accept(offset, this);
        }
    }

    public final static class SimpleCachedCompound extends UnnormalizedCachedCompound {

        SimpleCachedCompound(Op op, Subterms subterms) {
            super(op, subterms);
        }

        @Override
        public final Term root() {
            return this;
        }

        @Override
        public final Term concept() {
            return this;
        }
        @Override
        public final boolean equalsRoot(Term x) {
            return equals(x);
        }

    }




    /**
     * caches a reference to the root for use in terms that are inequal to their root
     */
    public static class TemporalCachedCompound extends CachedCompound {

        protected final int dt;

        protected TemporalCachedCompound(Op op, int dt, Subterms subterms) {
            super(op, dt, subterms);

            this.dt = dt;

            //HACK TEMPORARY for debug
//            if (op==CONJ && dtSpecial(dt) && subterms.subs()>1 && subterms.sub(0).compareTo(subterms.sub(1)) > 1)
//                throw new WTF();

//            {
//                if (volume() != anon().volume())
//                    throw new WTF("anon differ: "+toString()+'\n'+anon());
//
//                try {
//                    eventsAND((when, whta) -> {
//                        return true;
//                    }, 0, true, true);
//                    eventsAND((when, whta) -> {
//                        return true;
//                    }, 0, false, true);
//                } catch (Throwable t) {
//                    Util.nop();
//                }

//            }
//                if (dt!=DTERNAL && dt!=XTERNAL && dt%10!=0) //DITHERING TEST
//                    throw new WTF();

        }

        @Override
        public final int dt() {
            return dt;
        }

        @Override
        public boolean hasXternal() {
            return dt()==XTERNAL || (hasAny(Op.Temporal) && subterms().hasXternal());
        }
    }


    private CachedCompound(/*@NotNull*/ Op op, int dt, Subterms subterms) {
        super(dt == DTERNAL ? subterms.hashWith(op) : hashCombine(subterms.hashWith(op), dt) );
        this.op = op.id;
        this.subterms = subterms;

        this._structure = subterms.structure() | op.bit;

        this._volume = (short) subterms.volume();
    }


    abstract public int dt();

    @Override
    public final int indexOf(Term t) {
        return !impossibleSubTerm(t) ? super.indexOf(t) : -1;
    }

    @Override
    public final int volume() {
        return _volume;
    }

    @Override
    public final int structure() {
        return _structure;
    }

    @Override
    public final Subterms subterms() {
        return subterms;
    }

    @Override
    public final int vars() {
        return hasVars() ? super.vars() : 0;
    }

    @Override
    public final int varPattern() {
        return hasVarPattern() ? subterms().varPattern() : 0;
    }

    @Override
    public final int varQuery() {
        return hasVarQuery() ? subterms().varQuery() : 0;
    }

    @Override
    public final int varDep() {
        return hasVarDep() ? subterms().varDep() : 0;
    }

    @Override
    public final int varIndep() {
        return hasVarIndep() ? subterms().varIndep() : 0;
    }


    @Override
    public final Op op() {
        return Op.the(op);
    }

    @Override
    public final int opID() {
        return op;
    }
}

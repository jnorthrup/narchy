package nars.term;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Util;
import nars.IO;
import nars.Op;
import nars.Param;
import nars.derive.PatternCompound;
import nars.index.term.TermContext;
import nars.term.container.TermContainer;
import nars.term.transform.CompoundTransform;
import nars.term.transform.Retemporalize;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.CONJ;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

public class GenericCompoundDT /*extends ProxyTerm<Compound>*/ implements Compound {

    /**
     * numeric (term or "dt" temporal relation)
     */
    public final int dt;
    private final int hashDT;
    Compound ref;

    public GenericCompoundDT(Compound base, int dt) {
        this.ref = base;

        if (!(dt == XTERNAL || Math.abs(dt) < Param.DT_ABS_LIMIT))
            throw new InvalidTermException(base.op(), dt, base.subterms(), "exceeded DT limit");

        if (Param.DEBUG_EXTRA) {

            assert (getClass() != GenericCompoundDT.class /* a subclass */ || dt != DTERNAL);

            Op op = base.op();

            @NotNull TermContainer subterms = base.subterms();
            int size = subterms.size();

            if (op.temporal && (op != CONJ && size != 2))
                throw new InvalidTermException(op, dt, "Invalid dt value for operator", subterms.toArray());

            if (dt != XTERNAL && op.commutative && size == 2) {
                if (sub(0).compareTo(sub(1)) > 0)
                    throw new RuntimeException("invalid ordering");
            }

        }


        this.dt = dt;

        assert dt != DTERNAL || this instanceof PatternCompound : "use GenericCompound if dt==DTERNAL";

        assert dt == DTERNAL || dt == XTERNAL || (Math.abs(dt) < Param.DT_ABS_LIMIT) : "abs(dt) limit reached: " + dt;

        int baseHash = base.hashCode();
        this.hashDT = dt != DTERNAL ? Util.hashCombine(baseHash, dt) : baseHash;
    }

    @Override
    public @Nullable Term temporalize(Retemporalize r) {
        return Compound.super.temporalize(r);
    }

    @Override
    public /*@NotNull*/ Op op() {
        return ref.op();
    }


    @Override
    public final Term dt(int dt) {
        return dt == this.dt ? this : Compound.super.dt(dt);
    }

    @Override
    public int structure() {
        return ref.structure();
    }

    @Override
    public void append(ByteArrayDataOutput out) {
        Term.append(this, out);
    }

    @NotNull
    @Override
    public String toString() {
        return IO.Printer.stringify(this).toString();
    }

    @Override
    public Term sub(int i, Term ifOutOfBounds) {
        return ref.sub(i, ifOutOfBounds);
    }

    @Override
    public final int hashCodeSubTerms() {
        return ref.hashCodeSubTerms();
    }

    @Override
    public boolean xternalEquals(Term x) {
        return Compound.super.xternalEquals(x);
    }

    @Override
    public Term evalSafe(TermContext context, int remain) {
        return Compound.super.evalSafe(context, remain);
    }

    @Override
    @Nullable
    public final Term xternal() {
        return Compound.super.xternal();
    }

    @Override
    public @Nullable Term transform(@NotNull CompoundTransform t, Compound parent) {
        return Compound.super.transform(t, parent);
    }

    @Override
    public @Nullable Term transform(int newDT, @NotNull CompoundTransform t) {
        return Compound.super.transform(newDT, t);
    }

    @Override
    public @Nullable Term transform(@NotNull CompoundTransform t) {
        return Compound.super.transform(t);
    }

    @Override
    public @Nullable Term transform(ByteList path, Term replacement) {
        return Compound.super.transform(path, replacement);
    }

    @Override
    public @Nullable Term transform(ByteList path, int depth, Term replacement) {
        return Compound.super.transform(path, depth, replacement);
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) return true;

        if (!(that instanceof Term) || hashDT != that.hashCode())
            return false;

        if (that instanceof GenericCompoundDT) {
            GenericCompoundDT cthat = (GenericCompoundDT) that;
            if (dt != cthat.dt) return false;
            if (ref == cthat.ref) return true;
            else if (ref.equals(cthat.ref)) {
                ref = cthat.ref; //share
                return true;
            }
            return false;
        }

        return Compound.equals(this, that);
    }

    //    @Override
//    public boolean equals(Object obj) {
//        if (this == obj) return true;
//
//        if (obj instanceof GenericCompoundDT) {
//
//            GenericCompoundDT d = (GenericCompoundDT) obj;
//
//            Compound ref = this.ref;
//            Compound dref = d.ref;
//
//            if (!Param.CompoundDT_TermSharing) {
//
//                //compares hash and dt first, but doesnt share
//                return (hashDT == d.hashDT && dt == d.dt && ref.equals(d.ref));
//
//            } else {
//
//                if (ref == dref) {
//                    //ok
//                } else if (ref.equals(dref)) {
//                    //share equivalent instance, prefer to maintain a normalized term as it is likely used elsewhere (ie. in task content)
//                    if (ref.isNormalized()) {
//                        d.ref.setNormalized(); //though we will overwrite this next, in case it's shared elsewhere it will now also be known normalized
//                        d.ref = ref;
//                    } else if (d.ref.isNormalized()) {
//                        ref.setNormalized();  //though we will overwrite this next, in case it's shared elsewhere it will now also be known normalized
//                        this.ref = d.ref;
//                    } else {
//                        d.ref = ref;
//                    }
//
//
//                } else {
//                    return false;
//                }
//
//                return (hashDT == d.hashDT && dt == d.dt);
//            }
//
//        } else if (obj instanceof ProxyCompound) {
//            return equals(((ProxyCompound) obj).ref);
//        }
//
//        return false;
//    }

    @Override
    public @NotNull TermContainer subterms() {
        return ref.subterms();
    }

    @Override
    public void setNormalized() {
        ref.setNormalized();
    }


    @Override
    public boolean isNormalized() {
        return ref.isNormalized();
    }


    @Override
    public final int hashCode() {
        return hashDT;
    }

    @Override
    public final int dt() {
        return dt;
    }


    @Override
    public final Term term() {
        return this;
    }


    //    @Override
//    public Compound dt(int nextDT) {
//        if (nextDT == this.dt)
//            return this;
//
//        return compoundOrNull($.the(op(), nextDT, toArray()));
//
////        if (o.commutative && !Op.concurrent(this.dt) && Op.concurrent(nextDT)) {
////            //HACK reconstruct with sorted subterms. construct directly, bypassing ordinary TermBuilder
////            TermContainer ms = subterms();
////            //@NotNull TermContainer st = ms;
//////            if (!st.isSorted()) {
//////                Term[] ts = Terms.sorted(ms.toArray());
//////                if (ts.length == 1) {
//////                    if (o == CONJ)
//////                        return compoundOrNull(ts[0]);
//////                    return null;
//////                }
//////
//////                TermContainer tv;
//////                if (ms.equalTerms(ts))
//////                    tv = ms; //share
//////                else
//////                    tv = TermVector.the(ts);
//////
////                /*GenericCompound g =*/ return compoundOrNull($.the(o, nextDT, ms.toArray())); //new GenericCompound(o, tv);
//////                if (nextDT != DTERNAL)
//////                    return new GenericCompoundDT(g, nextDT);
//////                else
//////                    return g;
//////            }
////
////        }
////        return ref.dt(nextDT);
//    }
}

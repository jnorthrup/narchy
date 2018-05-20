package nars.term.anon;

import com.google.common.io.ByteArrayDataOutput;
import nars.Op;
import nars.subterm.Subterms;
import nars.subterm.TermVector;
import nars.term.Term;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Predicate;

import static nars.Op.NEG;
import static nars.term.anon.AnonID.idToTerm;
import static nars.term.anon.AnonID.idToTermWithNegationTest;

/**
 * a vector which consists purely of AnonID terms
 */
public class AnonVector extends TermVector {

    /*@Stable*/
    final short[] subterms;

    /** assumes the array contains only AnonID instances */
    public AnonVector(Term... s) {
        super(s); //TODO optimize this for certain Anom invariants (ie. no variables etc)

        boolean hasNeg = anyNeg(); //HACK quick filterer

        short[] t = subterms = new short[s.length];
        for (int i = 0, sLength = s.length; i < sLength; i++) {
            Term ss = s[i];
            boolean neg;
            if (hasNeg && ss.op()==NEG) {
                ss = ss.unneg();
                neg = true;
            } else {
                neg = false;
            }
            short tt = ((AnonID) ss).anonID();
            if (neg)
                tt = (short) -tt;
            t[i] = tt;
        }

    }


    @Override
    public final Term sub(int i) {
        return idToTermWithNegationTest(subterms[i]);
    }

    @Override
    public void append(ByteArrayDataOutput out) {
        short[] ss = subterms;
        out.writeByte(ss.length);
        for (short s : ss) {
            if (s > 0) {
                idToTerm(s).append(out);
            } else {
                //wrap (prepend) with a virtual NEG
                out.writeByte(Op.NEG.id);
                idToTerm((short)-s).append(out);
            }
        }

    }

    @Override
    public int subs() {
        return subterms.length;
    }

    int indexOf(AnonID t, boolean neg) {
        short id = t.anonID();
        if (neg)
            id = (short)(-id);
        return ArrayUtils.indexOf(subterms, id);
    }

    @Override
    public int indexOf(Term t) {
        boolean neg = false;
        if (t.op()==NEG) {
            if (!anyNeg())
                return -1;
            t = t.unneg();
            neg = true;
        }
        if (t instanceof AnonID)
            return indexOf((AnonID) t, neg);
        else
            return -1; //super.indexOf(t);
    }

    @Override
    public boolean contains(Term t) {
        return indexOf(t)!=-1;
    }

    @Override
    public boolean containsRecursively(Term t, boolean root, Predicate<Term> inSubtermsOf) {
        if (t.op() == NEG) {
            if (anyNeg()) {
                Term tt = t.unneg();
                if (tt instanceof AnonID) {
                    return indexOf((AnonID)tt,true)!=-1;
                }
            }
        }else {
            if (t instanceof AnonID) {
                return (indexOf((AnonID)t, false)!=-1)  //the positive version
                        ||
                       (anyNeg() && indexOf((AnonID)t, true)!=-1 && inSubtermsOf.test(t.neg())); //the negative version, and tested as such
            }

        }
        return false;
    }

    @Override
    public boolean isTemporal() {
        return false; //this is limited to atomics so there is no temporal possibility
    }

    private boolean anyNeg() {
        return (structure&NEG.bit) != 0;
//        for (short s : subterms)
//            if (s < 0) return true;
//        return false;
    }


    @Override
    public Iterator<Term> iterator() {
        return new AnonArrayIterator(subterms);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (this.hash != obj.hashCode()) return false;

        if (obj instanceof AnonVector) {
            return Arrays.equals(subterms, ((AnonVector) obj).subterms);
        }

        if (obj instanceof Subterms) {


            Subterms ss = (Subterms) obj;
            int s = subterms.length;
            if (ss.subs() != s)
                return false;
            for (int i = 0; i < s; i++) {
                if (!sub(i).equals(ss.sub(i)))
                    return false;
            }
            return true;

        }
        return false;
    }

//    @Override
//    public boolean unifyLinear(Subterms Y, Unify u) {
//        int s = subs();
//        if (Y instanceof AnonVector) {
//            //accelerated AnonVector vs. AnonVector unification
//
//            MetalBitSet ok;
//            AnonVector yy = (AnonVector) Y;
//            //1. if both contain constant atoms, check for any conflicting constant terms before attempting any variable matching
//            if (((structure & ATOM.bit) > 0) && ((yy.structure & ATOM.bit) > 0)) {
//                ok = MetalBitSet.bits(s);
//                for (int i = 0; i < s; i++) {
//                    short xi = subterms[i];
//                    short yi = yy.subterms[i];
//                    if ((isAnomOrNegatedAnom(xi) && isAnomOrNegatedAnom(yi))) {
//                        if (xi != yi)
//                            return false; //both constants, so not equal
//                        else
//                            ok.set(i); //continue
//                    }
//                }
//                if (ok.getCardinality()==s)
//                    return true; //wtf it was equal?
//            } else {
//                ok = null;
//            }
//
//
//            //2. fully unify any variable-containing indices
//            for (int i = 0; i < s; i++) {
//                if (ok!=null && ok.get(i))
//                    continue; //already checked in first pass
//
//                short xi = subterms[i];
//                short yi = yy.subterms[i];
//
//                if (xi < 0 && yi < 0) {
//                    //both negations so unwrap these simultaneously
//                    xi = (short) -xi;
//                    yi = (short) -yi;
//                }
//
//                //one or both are variables, so decode to terms and unify normally
//                if (!idToTermWithNegationTest(xi).unify(idToTermWithNegationTest(yi), u))
//                    return false;
//            }
//        } else {
//            //TODO do the constant pass first like the above case
//
//            for (int i = 0; i < s; i++) {
//                short xi = subterms[i];
//                Term yi = Y.sub(i);
//                if (isAnomOrNegatedAnom(xi)) {
//                    //stupid test for constant negation mismatch
//                    Op yio = yi.op();
//                    if (!yio.var) {
//                        //both are constant, only a few possibilities exist...
//                        // plus opportunity to simultaneously unwrap if both are negated
//                        if (yio != NEG ^ xi > 0 /* polarity differ, and yi isnt a var */)
//                            return false;
//
//                        if (yio == NEG) {
//                            //then unwrap simultaneously both
//                            yi = yi.unneg();
//                            yio = yi.op();
//                            xi = (short) -xi;
//                        }
//                        if (yio!=ATOM)
//                            return false; //had to be negated atom
//                        return idToTerm(xi) == yi; //note the instance equality test, it is all that must be tested
//                    }
//                }
//
//                Term xxi = idToTermWithNegationTest(xi);
//                if (!xxi.unify(yi, u))
//                    return false;
//            }
//        }
//        return true;
//    }
}

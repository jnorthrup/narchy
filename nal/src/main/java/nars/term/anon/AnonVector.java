package nars.term.anon;

import com.google.common.io.ByteArrayDataOutput;
import nars.Op;
import nars.subterm.Subterms;
import nars.subterm.TermVector;
import nars.subterm.util.TermList;
import nars.term.Term;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Predicate;

import static nars.Op.NEG;
import static nars.term.anon.AnonID.*;

/**
 * a vector which consists purely of AnonID terms
 */
public class AnonVector extends TermVector {

    /*@Stable*/
    private final short[] subterms;

    /** assumes the array contains only AnonID instances */
    public AnonVector(Term... s) {
        super(s); 

        boolean hasNeg = anyNeg(); 

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

        /* checks for monotonically increasing variable numbers starting from 1,
         which will indicate that the subterms is normalized
         */
        if (vars()>0) {
            boolean normalized = true;
            int minID = 0;
            for (short x : subterms) {
                if (x < 0) x = (short) -x;
                int varID = AnonID.isVariable(x, -1);
                if (varID == -1) continue; //anom
                else if (varID == minID) {
                    //same order, ok
                } else if (varID == minID+1) {
                    //increase the order, ok
                    minID++;
                } else if (varID > minID + 1) {
                    normalized = false; //cant be sure
                    break;
                }
            }
            if (normalized)
                setNormalized();
        }
    }

//    @Override
//    public @Nullable Subterms transformSubs(TermTransform f) {
//        @Nullable Subterms s = super.transformSubs(f);
////        if (s!=this && s instanceof AnonVector && equals(s))
////            return this; //HACK
////        else
//            return s;
//    }

    @Override
    public Subterms replaceSubs(Term from, Term to) {
        int found;
        if ((found = indexOf(from))==-1)
            return this;

        short eq = subterms[found]; 
        int n = subs();
        TermList t = new TermList(n);
        for (int i = 0; i < n; i++) {
            short si = subterms[i];
            t.addWithoutResizeCheck(si == eq ? to : idToTermWithNegationTest(si)); 
        }
        return t;
    }

    @Override
    public final Term sub(int i) {
        return idToTermWithNegationTest(subterms[i]);
    }

    @Override
    public int subs(Op matchingOp) {
        short match;
        switch (matchingOp) {
            case NEG: return subsNeg();
            case ATOM: match = AnonID.ATOM_MASK; break;
            case VAR_PATTERN: match = AnonID.VARPATTERN_MASK; break;
            case VAR_QUERY: match = AnonID.VARQUERY_MASK; break;
            case VAR_DEP: match = AnonID.VARDEP_MASK; break;
            case VAR_INDEP: match = AnonID.VARINDEP_MASK; break;
            default: return 0;
        }
        int count = 0;
        for (short s : subterms) {
            if (s > 0 && idtoMask(s) == match)
                count++;
        }
        return count;
    }

    private int subsNeg() {
        int count = 0;
        for (short s : subterms) {
            if (s < 0)
                count++;
        }
        return count;
    }

    @Override
    public void append(ByteArrayDataOutput out) {
        short[] ss = subterms;
        out.writeByte(ss.length);
        for (short s : ss) {
            if (s < 0) {
                
                out.writeByte(Op.NEG.id);
                s = (short)-s;
            }
            idToTerm(s).appendTo(out);
        }
    }

    @Override
    public int subs() {
        return subterms.length;
    }

    private int indexOf(AnonID t, boolean neg) {
        return ArrayUtils.indexOf(subterms, t.anonID(neg));
    }
    private int indexOf(AnonID t) {
        return ArrayUtils.indexOf(subterms, t.anonID());
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
            return -1; 
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
                return (indexOf((AnonID)t)!=-1)  
                        ||
                       (anyNeg() && indexOf((AnonID)t, true)!=-1 && inSubtermsOf.test(t.neg())); 
            }

        }
        return false;
    }

    @Override
    public boolean isTemporal() {
        return false; 
    }

    private boolean anyNeg() {
        return (structure&NEG.bit) != 0;



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

















































































}

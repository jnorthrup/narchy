package nars.subterm;

import jcog.util.ArrayUtil;
import nars.Op;
import nars.subterm.util.SubtermMetadataCollector;
import nars.term.Neg;
import nars.term.Term;
import nars.term.anon.AnonArrayIterator;
import nars.term.anon.Intrin;
import nars.term.anon.IntrinAtomic;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static nars.Op.NEG;
import static nars.term.anon.Intrin.term;

/**
 * a vector which consists purely of AnonID terms
 */
public class IntrinSubterms extends TermVector /*implements Subterms.SubtermsBytesCached*/ {

    /*@Stable*/
    public final short[] subterms;

    public IntrinSubterms(short[] s) {
        super(subtermMetadata(s));
        this.subterms = s;
    }

    /**
     * assumes the array contains only Intrin-able terms
     */
    public IntrinSubterms(Term... s) {
        super(s);

        var hasNeg = hasNeg();

        var t = subterms = new short[s.length];
        for (int i = 0, sLength = s.length; i < sLength; i++) {
            var ss = s[i];
            var neg = hasNeg && ss instanceof Neg;
            if (neg)
                ss = ss.unneg();
            var tt = Intrin.id(ss); //assert(tt!=0);
            t[i] = neg ? ((short)-tt) : tt;
        }

        this.normalized = preNormalize();
    }

    public static SubtermMetadataCollector subtermMetadata(short[] s) {
        var c = new SubtermMetadataCollector();
        for (var x : s)
            c.collectMetadata(term(x));
        return c;
    }

    private boolean preNormalize() {
        return vars() == 0 || normalized(subterms);
    }


//        @Override
//    public @Nullable Subterms transformSubs(TermTransform f) {
//        @Nullable Subterms s = super.transformSubs(f);
//        if (s!=this && equals(s))
//            return this; //HACK
//        else
//            return s;
//    }


    /** since IntrinSubterms is flat, the structureSurface is equal to the structure if no NEG present */
    @Override public final int structureSurface() {
        if ((structure & NEG.bit) == 0)
            return structure;
        else {
            var x = 0;
            for (var t : subterms)
                x |= (t < 0) ? NEG.bit : Intrin.term(t).opBit();
            return x;
        }
    }

    public final boolean AND(/*@NotNull*/ Predicate<Term> p) {
        var s = subs();
        short prev = 0;
        for (var i = 0; i < s; i++) {
            var next = this.subterms[i];
            if (prev!=next && !p.test(term(next)))
                return false;
            prev = next;
        }
        return true;
    }

    public final boolean OR(/*@NotNull*/ Predicate<Term> p) {
        var s = subs();
        short prev = 0;
        for (var i = 0; i < s; i++) {
            var next = this.subterms[i];
            if (prev!=next && p.test(term(next)))
                return true;
            prev = next;
        }
        return false;
    }

    @Override
    public int height() {
        return hasNeg() ? 3 : 2;
    }

    public Subterms replaceSub(Term from, Term to, Op superOp) {


        var fid = Intrin.id(from);
        if (fid == 0)
            return this; //no change

        var found = false;
        if (fid > 0) {
            //find positive or negative subterm
            for (var x: subterms) {
                if (Math.abs(x) == fid) {
                    found = true;
                    break;
                }
            }
        } else {
            //find exact negative only
            for (var x: subterms) {
                if (x == fid) {
                    found = true;
                    break;
                }
            }
        }
        if (!found)
            return this;


        var tid = Intrin.id(to);
        if (tid != 0) {
            assert (from != to);
            var a = this.subterms.clone();
            if (fid > 0) {
                for (int i = 0, aLength = a.length; i < aLength; i++) { //replace positive or negative, with original polarity
                    var x = a[i];
                    if (x == fid) a[i] = tid;
                    else if (-x == fid) a[i] = (short) -tid;
                }
            } else {
                for (int i = 0, aLength = a.length; i < aLength; i++) //replace negative only
                    if (a[i] == fid) a[i] = tid;
            }

            var v = new IntrinSubterms(a);
            v.normalized = preNormalize();
            return v;

        } else {
            var n = subs();
            var tt = new Term[n];
            var a = this.subterms;
            if (fid > 0) {
                for (var i = 0; i < n; i++) { //replace positive or negative, with original polarity
                    var x = a[i];
                    Term y;
                    if (x == fid) y = (to);
                    else if (-x == fid) y = (to.neg());
                    else y = (term(x));
                    tt[i] = (y);
                }
            } else {
                //replace negative only
                tt = IntStream.range(0, n).mapToObj(i -> (a[i] == fid ? to : term(a[i]))).toArray(Term[]::new);

            }
            return new TermList(tt);
        }
    }


    @Override
    public final Term sub(int i) {
        return term(subRaw(i));
    }

    final short subRaw(int i) {
        return subterms[i];
    }

    /** all of the subterms here are atoms so containing eventRange is impossible */
    @Override public final int subEventRange(int i) {
        return 0;
    }

    @Override
    public int count(Op matchingOp) {
        switch (matchingOp) {
            case NEG:
                return subsNeg();

            case ATOM:
                return countByGroup(Intrin.ANOMs) + countByGroup(Intrin.CHARs);

            case VAR_PATTERN:
                return countByGroup(Intrin.VARPATTERNs);

            case VAR_QUERY:
                return countByGroup(Intrin.VARQUERYs);

            case VAR_DEP:
                return countByGroup(Intrin.VARDEPs);

            case VAR_INDEP:
                return countByGroup(Intrin.VARINDEPs);

            case IMG:
                return countByGroup(Intrin.IMGs);

            case INT:
                return countByGroup(Intrin.INT_POSs) + countByGroup(Intrin.INT_NEGs);

            default:
                return 0;
        }

    }

    public int countByGroup(short group) {
        var count = 0;
        for (var s: subterms) {
            if (s > 0 && Intrin.group(s) == group)
                count++;
        }
        return count;
    }

    private int subsNeg() {
        var count = 0;
        for (var s: subterms)
            if (s < 0)
                count++;
        return count;
    }


    @Override
    public final int subs() {
        return subterms.length;
    }

    private int indexOf(short id) {
        return indexOf(id, -1);
    }
    private int indexOf(short id, int after) {
        return ArrayUtil.indexOf(subterms, id, after+1);
    }
// TODO TEST
//    private int indexOf(short id, int after) {
//        return ArrayUtils.indexOf(subterms, id, after+1);
//    }

    public int indexOf(IntrinAtomic t) {
        return indexOf(t.i);
    }


    @Override
    public int indexOf(Term t, int after) {
        var tid = Intrin.id(t);
        return tid != 0 ? indexOf(tid,after) : -1;
    }

    // TODO TEST
//    @Override
//    public int indexOf(Term t, int after) {
//        throw new TODO();
//    }

    private int indexOfNeg(Term x) {
        var tid = Intrin.id(x);
        return tid != 0 ? indexOf((short) -tid) : -1;
    }

    @Override
    public @Nullable Term[] removing(Term x) {
        var tid = Intrin.id(x);
        if (tid == 0) return null;
        var count = 0;

        for (var subterm : subterms) {
            if (subterm == tid)
                count++;
        }
        if (count==0)
            return null; //none found
        var n = subterms.length;
        if (count == n){
            return Op.EmptyTermArray;
        } else {
            var y = new Term[n - count];
            var j = 0;
            for (var s : subterms) {
                if (s != tid)
                    y[j++] = term(s);
            }
            return y;
        }
    }



    @Override
    public boolean containsNeg(Term x) {
        return indexOfNeg(x) != -1;
    }

    @Override
    public boolean containsRecursively(Term x, boolean root, Predicate<Term> inSubtermsOf) {
        if (x instanceof Neg) {
            if (hasNeg()) {
                var tt = x.unneg();
                var ttu = Intrin.id(tt);
                if (ttu!=0)
                    return indexOf((short) -ttu) != -1;
            }
        } else {
            var aid = Intrin.id(x);
            if (aid!=0) {
                var hasNegX = false;
                for (var xx : this.subterms) {
                    if (xx == aid)
                        return true; //found positive
                    else if (xx == -aid)
                        hasNegX = true; //found negative, but keep searching for a positive first
                }
                if (hasNegX)
                    return (inSubtermsOf==null || inSubtermsOf.test(x.neg()));
            }

        }
        return false;
    }

    private boolean hasNeg() {
        return (structure & NEG.bit) != 0;
    }

    @Override
    public Iterator<Term> iterator() {
        return new AnonArrayIterator(subterms);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (obj instanceof IntrinSubterms)
            return Arrays.equals(subterms, ((IntrinSubterms) obj).subterms);

        if (obj instanceof Subterms) {

            var ss = (Subterms) obj;

            //if (this.hash != ss.hashCodeSubterms()) return false;

            var s = subterms.length;
            return ss.subs() == s && IntStream.range(0, s).allMatch(i -> subEquals(i, ss.sub(i)));

        }
        return false;
    }

    @Override
    public boolean subEquals(int i, Term x) {
        var xx = Intrin.id(x);
        return xx!=0 ? subterms[i]==xx : sub(i).equals(x);
    }

    //private transient byte[] bytes = null;



//    @Override
//    public void acceptBytes(DynBytes constructedWith) {
//        if (bytes == null)
//            bytes = constructedWith.arrayCopy(1 /* skip op byte */);
//    }

    @Override
    public @Nullable Term subSub(int start, int end, byte[] path) {
        var depth = end-start;
        if (depth <= 2) {
            var z = path[start];
            if (subterms.length > z) {
                switch (depth) {
                    case 1:
                        return sub(z);
                    case 2:
                        if (path[start+1] == 0) {
                            var a = this.subterms[z];
                            if (a < 0)
                                return Intrin.term((short) -a); //if the subterm is negative its the only way to realize path of length 2
                        }
                        break;
                }
            }
        }
        return null;
    }
}

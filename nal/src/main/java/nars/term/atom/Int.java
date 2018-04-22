package nars.term.atom;

import com.google.common.collect.*;
import com.google.common.io.ByteArrayDataOutput;
import jcog.TODO;
import jcog.Util;
import jcog.data.SimpleIntSet;
import jcog.list.FasterList;
import jcog.math.Longerval;
import jdk.internal.vm.annotation.Stable;
import nars.$;
import nars.Op;
import nars.Param;
import nars.The;
import nars.subterm.util.TermMetadata;
import nars.term.Compound;
import nars.term.Evaluation;
import nars.term.Term;
import nars.term.Termed;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.list.primitive.ImmutableByteList;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

import java.util.*;
import java.util.function.IntConsumer;

import static com.google.common.collect.BoundType.OPEN;
import static nars.Op.INT;
import static nars.Op.Null;
import static nars.term.Terms.sorted;

/**
 * 32-bit signed integer
 */
public class Int implements Intlike, The {


    @Stable
    private final byte[] bytesCached;

    public static Int the(int i) {
        if (i >= 0 && i < Param.MAX_INTERNED_INTS) {
            return pos[i];
        } else {
            if (i < 0 && i > -Param.MAX_INTERNED_INTS) {
                return neg[-i];
            } else {
                return new Int(i);
            }
        }
    }

    public static Intlike range(int from, int to) {
        return ((from == to) ? the(from) :
                new IntRange(from, to));
    }

    final static int INT_ATOM = Term.opX(INT, 0);
    final static int INT_RANGE = Term.opX(INT, 1);

    public final int id;

    protected static final Int[] pos = new Int[Param.MAX_INTERNED_INTS];
    protected static final Int[] neg = new Int[Param.MAX_INTERNED_INTS];

    static {
        for (int i = 0; i < Param.MAX_INTERNED_INTS; i++) {
            pos[i] = new Int(i);
            neg[i] = new Int(-i);
        }
    }


    protected Int(int id, byte[] bytes) {
        this.id = id;
        this.bytesCached = bytes;
    }

    protected Int(int i) {
        this.id = i;

        byte[] b = new byte[6];
        b[0] = op().id;
        b[1] = 0; //subtype
        Util.int2Bytes(id, b, 2);
        this.bytesCached = b;
    }

    @Override
    public final void collectMetadata(TermMetadata.SubtermMetadataCollector s) {
        s.collectNonVar(op(), hashCode());
    }

    @Override
    public final byte[] bytes() {
        return bytesCached;
    }

    @Override
    public final void append(ByteArrayDataOutput out) {
        out.write(bytesCached);
    }

    @Override
    public final Term eval(Evaluation.TermContext context) {
        return this;
    }

    @Override
    public final Term evalSafe(Evaluation.TermContext context, Op supertermOp, int subterm, int remain) {
        return this;
    }

    @Override
    public Range range() {
        return Range.singleton(id).canonical(DiscreteDomain.integers());
    }


    @Override
    public int opX() {
        return INT_ATOM;
    }

    @Override
    public /**/ Op op() {
        return INT;
    }

    @Override
    public int hashCode() {
        return (id + 1) * 31;
        //return id ^ 89478485; //010101010...
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (id >= Param.MAX_INTERNED_INTS && obj instanceof Int) { //dont need to check if this is an interned Int
            Int o = (Int) obj;
            //HACK check for op because Anom extends Int
            return (id == o.id) && (o.op() == INT);
        }
        return false;
    }

    @Override
    public String toString() {
        return Integer.toString(id);
    }


    @Override
    public int complexity() {
        return 1;
    }


//    @Override
//    public boolean unify(Term y,  Unify subst) {
//        if (Intlike.super.unify(y, subst))
//            return true;
//        //if (equals(y)) return true;
//        if (y instanceof IntRange) {
//            IntRange ir = (IntRange) y;
//            if (ir.min <= id && ir.max >= id) {
//                //return subst.putXY(y, this); //specialize from the range to this int
//                return true;
//            }
//        }
//        return false;
//    }

    public static Intlike range(Range<Integer> span) {
        return range(span.lowerEndpoint(), span.upperEndpoint() - ((span.upperBoundType() == OPEN ? 1 : 0)));
    }

    @Override
    public void forEachInt(IntConsumer c) {
        c.accept(id);
    }

    /**
     * a contiguous range of 1 or more integers
     */
    public static class IntRange implements Intlike, The {


        public final int min, max;
        private final int hash;
        private final byte[] bytesCached;

        /**
         * from, to - inclusive interval
         */
        IntRange(int min, int max) {
            assert (min < max);
            this.min = min;
            this.max = max;
            this.hash = Util.hashCombine(INT_RANGE, min, max);

            byte[] b = new byte[10];
            b[0] = INT.id;
            b[1] = 0; //subtype
            Util.int2Bytes(min, b, 2);
            Util.int2Bytes(min, b, 6);
            this.bytesCached = b;
        }

        @Override
        public void forEachInt(IntConsumer c) {
            for (int i = min; i <= max; i++) {
                c.accept(i);
            }
        }

        @Override
        public byte[] bytes() {
            return bytesCached;
        }
//        @Override
//        public boolean unify(Term y, Unify subst) {
//            if (Intlike.super.unify(y, subst)) return true;
//
//            //one way:
//            if (y instanceof IntRange) {
//                IntRange z = (IntRange) y;
//                return z.contains(this);
//            }
//
////            if (y instanceof Int) {
////                return intersects((Int)y);
////            } else if (y instanceof IntRange) {
////                IntRange z = (IntRange) y;
////                return contains(z) || z.contains(this);
////            }
//            return false;
//        }

        public boolean intersects(Int y) {
            int i = y.id;
            return (min <= i && max >= i);
        }

        public boolean connects(IntRange y) {
            return Longerval.intersectLength(min, max, y.min, y.max) >= 0;
        }

        public boolean contains(IntRange y) {
            return (y.min >= min) && (y.max <= max);
        }

        public boolean contains(Int y) {
            int yy = y.id;
            return (yy >= min) && (yy <= max);
        }

        @Override
        public String toString() {
            return min + ".." + max;
        }

        @Override
        public /**/ Op op() {
            return INT;
        }

        @Override
        public int complexity() {
            return 1;
        }


        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (o instanceof IntRange) {
                IntRange ir = (IntRange) o;
                return ir.min == min && ir.max == max;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        public long hash64() {
            return (((long) max) << 32) | min;
        }

        @Override
        public int opX() {
            return INT_RANGE;
        }

        @Override
        public Range range() {
            return Range.closed(min, max).canonical(DiscreteDomain.integers());
        }

        public Term subtract(Term b) {
            if (equals(b))
                return Null;
            if (b instanceof IntRange) {
                IntRange bb = (IntRange) b;
                if (contains(bb)) {
                    if (Param.DEBUG)
                        throw new TODO();
                }
            } else if (b.op() == INT) {
                Int bb = (Int) b;
                int bbi = bb.id;
                if (min == bbi) {
                    return Int.range(min + 1, max);
                } else if (max == bbi) {
                    return Int.range(min, max - 1);
                } else if (min < bbi && bbi < max) {
                    return Op.SECTe.the(Int.range(min, bbi - 1), Int.range(bbi + 1, max));
                }
            }
            return Null;
        }

        public Term intersect(Term b) {
            if (equals(b))
                return this;

            if (b instanceof IntRange) {
                IntRange bb = (IntRange) b;
                if (connects(bb))
                    return Int.range(range().intersection(bb.range()));
            } else if (b.op() == INT) {
                if (intersects((Int) b))
                    return b;
            }

            return Null;
        }
    }

//    public static Term[] intersect(Term[] u) {
//
//        TreeSet<Intlike> integers = new TreeSet();
//        for (Term x : u) {
//            if (x.op() == INT) {
//                integers.add((Intlike) x);
//            }
//        }
//
//        int ii = integers.size();
//        if (ii < 2)
//            return u; //one or less integers, do nothing about it
//
//
//        TreeSet<Term> v = new TreeSet<>();
//        for (Term uu : u)
//            v.add(uu);
//
//        Intlike a = integers.pollFirst();
//        Intlike b = integers.pollFirst();
//        Range ar = a.range();
//        Range br = b.range();
//        if (ar.isConnected(br)) {
//            Intlike combined = Int.the(ar.span(br));
//            v.remove(a);
//            v.remove(b);
//            v.add(combined);
//        }
//
//
//        return v.toArray(new Term[v.size()]);
//
//    }

    /**
     * TODO permute other arrangements
     */
    public static Term[] intersect(final Term... _subs) {


        RoaringBitmap factoring = new RoaringBitmap();

        int equalVolume = -1, equalStructure = -1;
        int pureInts = 0;
        for (int i = 0, subsLength = _subs.length; i < subsLength; i++) {
            Term x = _subs[i];
            if (!x.hasAny(Op.INT)) {
                continue;
            }
            if (x.op() == INT)
                pureInts++;

            if (equalVolume != -1) {
                if (x.volume() != equalVolume) {
                    continue; //a term with non-matching volume
                }
            }

            if (equalStructure != -1) {
                if (x.structure() != equalStructure) {
                    continue;
                }
            }

            equalVolume = x.volume();
            equalStructure = x.structure();
            factoring.add(i);
        }

        int ff = factoring.getCardinality();
        if (ff < 2)
            return sorted(_subs);



        Term[] subs;
        if (ff < _subs.length) {
            subs = new Term[ff];
            int j = 0;
            for (int i = 0; i < _subs.length; i++) {
                if (factoring.contains(i))
                    subs[j++] = _subs[i];
            }
            assert (j == ff);
        } else {
            subs = _subs;
        }

       if (subs.length == 3) {
            Term[] rr;
            //HACK try permutations to combine some but not all
            Term[] ab = intersect(subs[0], subs[1]);
            if (ab.length==1) {
                rr = intersect(ab[0], subs[2]);
            } else {
                Term[] bc = intersect(subs[1], subs[2]);
                if (bc.length==1) {
                    rr = intersect(bc[0], subs[0]);
                } else {
                    Term[] ac = intersect(subs[0], subs[2]);
                    if (ac.length == 1) {
                        rr = intersect(ac[0], subs[1]);
                    } else {
                        rr = null;
                    }
                }
            }
            if (rr!=null) {
                return intersectResult(factoring, ff, new FasterList(rr), _subs);
            }
        }        

        FasterList<Term> yAux = new FasterList(0);

        if (pureInts == ff) {
            //avoid the path stuff, just merge the int terms
            SimpleIntSet s = new SimpleIntSet(ff);
            for (Term x : subs) {
                ((Intlike)x).forEachInt(s::add);
            }
            int ns = s.size();
            assert(ns > 1);
            if (ns ==2) {
                //simple case
                Iterator<Integer> si = s.iterator();
                int a = si.next();
                int b = si.next();
                if (Math.abs(a-b)>1) {
                    yAux.add(Int.the(a));
                    yAux.add(Int.the(b));
                } else {
                    if (a > b) {
                        int c = b;
                        b = a;
                        a = c;
                    }
                    yAux.add(Int.range(a,b));
                }
            } else {
                features(s.iterator(), -1).forEachRemaining(yAux::add);
            }
        } else {

            //paths * extracted sequence of numbers at given path for each subterm
            Map<ByteList, Object /*SimpleIntSet*/> data = new LinkedHashMap<>(subs.length);

            //if a subterm is not an integer, check for equality of atoms (structure already compared abovec)
            final boolean[] valid = {true};
            subs[0].pathsTo(x -> x, d -> true, (p, x) -> {
                //            if (p.isEmpty())
                //                return true; //continue past root, of course the root term will be unique

                ImmutableByteList path = null;
                SimpleIntSet c = null;
                int xVol = x.volume();
                int xStruct = x.structure();
                for (int others = 1; others < subs.length; others++) {
                    Term y = subs[others].subPath(p);

                    if (x.equals(y)) continue;

                    if (!x.hasAny(INT)) {
                        //and we expect differences but only in the INT
                        if (!y.equals(x)) {
                            valid[0] = false;
                            return false;
                        }
                    } else if (x.op() == INT) {
                        if (y.op() != INT) {
                            valid[0] = false;
                            return false;
                        }
                        if (path == null) path = p.toImmutable();

                        //store the path to the integer(s)
                        if (c == null)
                            c = (SimpleIntSet) data.computeIfAbsent(path, (pp) -> new SimpleIntSet(2));
                        ((Intlike) y).forEachInt(c::add);
                    } else {
                        //this is a term containing an INT but is not an INT; the structure must still match
                        if (xVol != y.volume() || xStruct != y.structure()) {
                            valid[0] = false;
                            return false;
                        }
                    }
                }
                if (x.op() == INT) {
                    if (c == null) {
                        if (path == null) path = p.toImmutable();
                        data.put(path, c = new SimpleIntSet(1));
                    }
                    ((Intlike) x).forEachInt(c::add);
                }

                return true;
            });
            if (!valid[0])
                return _subs;


            Iterator<Map.Entry<ByteList, Object>> entries = data.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<ByteList, Object /*SimpleIntSet*/> e = entries.next();

                SimpleIntSet s = (SimpleIntSet) e.getValue();
                if (s.size() < 2) {
                    entries.remove();
                    continue; //same integer value in each
                }


                //for each path where the other numerics are uniformly equal (only one unique value)
                /*if (new HashSet(nn).size()==1)*/


                Iterator<Integer> si = s.iterator();

                if (e.getKey().isEmpty()) {
                    //root level, get as many as possible
                    features(si, -1).forEachRemaining(yAux::add);
                    entries.remove();
                } else {
                    Iterator<Intlike> iii = features(si, 1);
                    if (iii == null || !iii.hasNext())
                        return _subs; //discontiguous or otherwise ununifiable

                    e.setValue(iii.next());
                }
            }

            Term y;
            if (!data.isEmpty()) {
                y = subs[0];
                for (Map.Entry<ByteList, Object /*Intlike*/> e : data.entrySet()) {
                    Object v = e.getValue();
                    y = y.transform(e.getKey(), (Term) v);
                }
            } else {
                y = null;
            }
            if (subs.length == _subs.length && yAux.isEmpty()) {
                if (y == null)
                    return _subs; //??
                return new Term[]{y};
            } else {
                yAux.add(y);
            }
        }


        return intersectResult(factoring, ff, yAux, _subs);


//            int ffs = ff.size();
//            if (ffs == 0 || ffs >= numInvolved) {
//                //nothing would be gained; dont bother
//                continue;
//            }

//
//            for (Intlike f : ff) {
//                byte j = 0;
//                for (Term x : subs) {
//
//                    if (!involved.contains(j)) {
//                        result.add(x);
//                        //System.out.println("1: " + result);
//                    } else {
//
//
//                        //x is contained within range expression p
//                        Term xpp = x.subs() > 0 ? x.subPath(pp) : x;
//
//                        boolean connected;
//                        if (xpp.op() == INT) {
//                            connected = (f.range().isConnected(((Intlike) xpp).range()));
//                        } else {
//                            connected = false;
//                        }
//
//                        if (connected) {
//                            Term y = x instanceof Compound ?
//                                    x.transform(pp, f)
//                                    : f;
//                            //if (!y.equals(x)) {
//
//                            if (!x.equals(y)) {
//                                subsumed.add(x);
//                            }
//                            result.add(y);
//                            //System.out.println(x + " 3: " + result + "\t + " + y);
//                            //}
//                        } else {
//                            result.add(x);
//                        }
//                    }
//                    j++;
//                }
//
//
//            }
//
//            int results = result.size();
//            if ((results == 1) /*|| (results > resultLimit * subCount)*/) {
//                break; //reduced to one or exploded, go no further
//            }
//        }

//        result.removeAll(subsumed);
//
//        if (result.isEmpty()) {
//            return subs;
//        } else {
//
//            Term[] rr = result.toArray(new Term[result.size()]);
//            if (Arrays.equals(rr, subs))
//                return rr;
//            else
//                return intersect(rr); //changed, recompress
//
//        }
    }

    private static Term[] intersectResult(RoaringBitmap factoring, int ff, FasterList<Term> yAux, Term[] _subs) {
        int yAuxSize = yAux.size();
        int nonff = _subs.length - ff;
        Term[] r;
        if (nonff == 0) {
            r = yAux.toArrayRecycled(Term[]::new);
        } else {
            r = new Term[nonff + yAuxSize];
            int j = 0;
            if (ff != _subs.length) {
                for (int k = 0; k < _subs.length; k++) {
                    if (!factoring.contains(k)) {
                        r[j++] = _subs[k];
                    }
                }
            }
            for (int k = 0; k < yAuxSize; k++) {
                r[j++] = yAux.get(k);
            }
        }
        return sorted(r);
    }


    private static Iterator<Intlike> features(Iterator<Integer> nnnt, int limitCount) {

        RangeSet<Integer> intIntervals = ranges(nnnt);

        //if (!intIntervals.isEmpty()) {
        //Range<Integer> rNew = intIntervals.span();
        //if (rNew.upperEndpoint() - rNew.lowerEndpoint() > 1) {

        //boolean connected = true;
        //Range q = null;


        Set<Range<Integer>> srr = intIntervals.asRanges();
        int n = srr.size();
        if (limitCount > 0 && n > limitCount)
            return Collections.emptyIterator();
        else {
            return Iterators.transform(srr.iterator(), (rr) -> {
                int l = rr.lowerEndpoint();
                int u = rr.upperEndpoint();
                if (rr.lowerBoundType() == BoundType.OPEN)
                    l++;
                if (rr.upperBoundType() == BoundType.OPEN)
                    u--;
                return Int.range(l, u);
            });
        }

    }


    public static RangeSet<Integer> ranges(Iterator<Integer> ints) {
        TreeRangeSet<Integer> r = TreeRangeSet.create();
        while (ints.hasNext()) {
            int ii = ints.next();
            r.add(Range.singleton(ii).canonical(DiscreteDomain.integers()));
        }
        return r;
    }

    /**
     * unroll IntInterval's
     */
    public static Iterator<Term> unroll(Term cc) {
        //assert(!c.hasAny(Op.INT));
        //return Iterators.singletonIterator(c); //no IntInterval's early exit


        Map<ByteList, IntRange> intervals = new HashMap();

        cc.pathsTo(x -> x instanceof IntRange ? ((IntRange) x) : null, d -> d.hasAny(Op.INT), (ByteList p, IntRange x) -> {
            intervals.put(p.toImmutable(), x);
            return true;
        });

        int dim = intervals.size();
        switch (dim) {

            case 0:
                throw new RuntimeException();

            case 1: //1D
            {
                Map.Entry<ByteList, IntRange> e = intervals.entrySet().iterator().next();
                IntRange i1 = e.getValue();
                int max = i1.max;
                int min = i1.min;
                List<Term> t = $.newArrayList(1 + max - min);
                for (int i = min; i <= max; i++) {
                    @Nullable Term c1 = cc.transform(e.getKey(), $.the(i));
                    if (c1 != null)
                        t.add(c1);
                }
                return t.iterator();
            }

            case 2: //2D
                Iterator<Map.Entry<ByteList, IntRange>> ee = intervals.entrySet().iterator();
                Map.Entry<ByteList, IntRange> e1 = ee.next();
                Map.Entry<ByteList, IntRange> e2 = ee.next();
                IntRange i1 = e1.getValue();
                IntRange i2 = e2.getValue();
                int max1 = i1.max, min1 = i1.min, max2 = i2.max, min2 = i2.min;
                List<Term> t = $.newArrayList((1 + max2 - min2) * (1 + max1 - min1));

                for (int i = min1; i <= max1; i++) {
                    for (int j = min2; j <= max2; j++) {
                        Term c1 = cc.transform(e1.getKey(), $.the(i));
                        Term c2 = c1.transform(e2.getKey(), $.the(j));
                        if (!(c2 instanceof Compound))
                            //throw new RuntimeException("how not transformed to compound");
                            continue;
                        t.add(c2);
                    }
                }
                return t.iterator();

            default:
                //either there is none, or too many -- just use the term directly
                if (Param.DEBUG)
                    throw new UnsupportedOperationException("too many embedded dimensions: " + dim);
                else
                    return null;

        }

    }


    public static class RotatedInt implements Termed {

        private final int min, max;
        private Int i;

        public RotatedInt(int min /* inclusive */, int max /* exclusive */) {
            this.min = min;
            this.max = max;
            this.i = Int.the((min + max) / 2);
        }

        @Override
        public Term term() {
            Term cur = i;
            int next = this.i.id + 1;
            if (next >= max)
                next = min; //round robin
            this.i = Int.the(next);
            return cur;
        }
    }

}
//public class ArithmeticInduction {
//
//
//    public static Logger logger = LoggerFactory.getLogger(ArithmeticInduction.class);
//
//    private static final MultimapBuilder.SetMultimapBuilder setSetMapBuilder = MultimapBuilder.hashKeys().hashSetValues();
//    private static final int resultLimit = 1;
//    private static final boolean recurseAllSubstructures = false;
//
//    
//    public static TermContainer compress(Op op, int dt,  TermContainer args) {
//        if (op!=CONJ || !(((dt == DTERNAL) || (dt == 0))) || args.size() < 2 || !args.hasAny(Op.INT))
//            return args; //early exit condition
//
//        Set<Term> xx = args.toSet();
//        Set<Term> yy = compress(xx, 2);
//
//        if (!yy.equals(xx)) {
//            return TermVector.the(Terms.sorted(yy));
//        }
//
//        return args; //unchanged
//    }
//
//
//    
//    public static Set<Term> compress( Set<Term> subs, int depthRemain) {
//
//        int subCount = subs.size();
//        if (subCount < 2/* || !subs.hasAny(Op.INT)*/)
//            return subs; //early exit condition
//
//        SetMultimap<ByteList, Term> subTermStructures = setSetMapBuilder.builder();
//        int intContainingSubCount = 0;
//        for (Term x : subs) {
//            if (x.hasAny(Op.INT)) {
//                intContainingSubCount++;
//                subTermStructures.put(x.structureKey(), x);
//            }
//        }
//
//        int numUniqueSubstructures = subTermStructures.keySet().size();
//        if (numUniqueSubstructures == intContainingSubCount) {
//            return subs; //each subterm has a unique structure so nothing will be combined
//        } else if (numUniqueSubstructures > 1) {
//            //recurse with each sub-structure group and re-combine
//            if (recurseAllSubstructures) {
//
//                Set<Term> ss = new HashSet();
//                for (Collection<Term> stg : subTermStructures.asMap().values()) {
//                    ss.addAll(compress((Set<Term>) stg, depthRemain - 1));
//                }
//
//                return recompressIfChanged(subs, ss, depthRemain - 1);
//
//
//            } else {
//                return subs;
//            }
//        }
//
//        //group again according to appearance of unique atoms
//        SetMultimap<List<Term>, Term> subAtomSeqs = setSetMapBuilder.builder();
//        for (Term x : subs) {
//            if (x.hasAny(Op.INT))
//                subAtomSeqs.put(atomSeq(x), x);
//        }
//
//        int ssa = subAtomSeqs.keySet().size();
//        if (ssa == subCount) {
//            return subs;
//        } else if (ssa > 1) {
//            //process each unique atom seq group:
//            Set<Term> ss =
//                    //new TreeSet();
//                    new HashSet();
//            for (Collection<Term> ssg : subAtomSeqs.asMap().values()) {
//                ss.addAll(compress((Set<Term>)ssg, depthRemain-1));
//            }
//            return recompressIfChanged(subs, ss, -1);
//        }
//
//
//
////        if (!subs.equivalentStructures())
////            return subs;
////
////        if (!equalNonIntegerAtoms(subs))
////            return subs;
//
//
//        //paths * extracted sequence of numbers at given path for each subterm
//        Map<ByteList, Pair<ByteHashSet, List<Term>>> data = new HashMap();
//
//
//        //analyze subtermss
//        final int[] i = {0};
//        subs.forEach(f -> {
////        for (int i = 0; i < subCount; i++) {
////            //if a subterm is not an integer, check for equality of atoms (structure already compared abovec)
////             Term f = subs.term(i);
//
//            //first subterm: infer location of all inductables
//            int ii = i[0]++;
//            BiPredicate<ByteList, Term> collect = (p, t) -> {
//                if ((!p.isEmpty() || (t instanceof IntTerm) || (t instanceof IntInterval))) {
//                    Pair<ByteHashSet, List<Term>> c = data.computeIfAbsent(p.toImmutable(), (pp) ->
//                            Tuples.pair(new ByteHashSet(), $.newArrayList(1)));
//                    c.getOne().add((byte) ii);
//                    c.getTwo().add(t);
//                }
//
//                return true;
//            };
//
//            //if (f instanceof Compound) {
//            f.pathsTo(x -> x, collect);
//            /*} else {
//                if (f instanceof IntTerm) //raw atomic int term
//                    data.put(new ByteArrayList(new byte[] {0}), $.newArrayList(f));
//            }*/
//        });
//
//        Set<Term> result = new HashSet();//new TreeSet();
//        Set<Term> subsumed = new HashSet();
//
//        for (Map.Entry<ByteList, Pair<ByteHashSet, List<Term>>> e : data.entrySet()) {
//            //data.forEach((pp, nn) -> {
//            ByteList pp = e.getKey();
//            Pair<ByteHashSet, List<Term>> nn = e.getValue();
//
//            ByteHashSet involved = nn.getOne();
//            int numInvolved = involved.size(); //# of subterms involved
//
//            //at least 2 subterms must contribute a value for each path
//            if (numInvolved < 2)
//                continue;
//
//            //for each path where the other numerics are uniformly equal (only one unique value)
//            /*if (new HashSet(nn).size()==1)*/
//
//
//
//            List<IntInterval> ff = features(nn.getTwo());
//            int ffs = ff.size();
//            if (ffs == 0 || ffs >= numInvolved) {
//                //nothing would be gained; dont bother
//                continue;
//            }
//
//
//            for (IntInterval f : ff) {
//                byte j = 0;
//                for (Term x : subs) {
//
//                    if (!involved.contains(j)) {
//                        result.add(x);
//                        //System.out.println("1: " + result);
//                    } else {
//
//
//                        //x is contained within range expression p
//                        Term xpp = x instanceof Compound ? ((Compound) x).sub(pp) : x;
//
//                        boolean contained;
//                        if (xpp instanceof IntTerm) {
//                            contained = (f.val.contains(((IntTerm) xpp).val));
//                        } else if (xpp instanceof IntInterval) {
//                            contained = (f.val.encloses(((IntInterval) xpp).val));
//                        } else {
//                            contained = false;
//                        }
//
//                        if (contained) {
//                            Term y = x instanceof Compound ?
//                                    $.terms.transform((Compound) x, pp, f)
//                                    : f;
//                            //if (!y.equals(x)) {
//
//                            if (!x.equals(y)) {
//                                result.remove(x);
//                                subsumed.add(x);
//                            }
//                            result.add(y);
//                            //System.out.println(x + " 3: " + result + "\t + " + y);
//                            //}
//                        } else {
//                            result.add(x);
//                        }
//                    }
//                    j++;
//                }
//
//
//            }
//
//            int results = result.size();
//            if ((results == 1) || (results > resultLimit * subCount)) {
//                break; //reduced to one or exploded, go no further
//            }
//        }
//
//        result.removeAll(subsumed);
//
//        if (result.isEmpty()) {
//            return subs;
//        } else {
//            return recompressIfChanged(subs, result, depthRemain-1);
//        }
//
//    }
//
//    public
//    
//    static Set<Term> recompressIfChanged( Set<Term> orig,  Set<Term> newSubs, int depthRemain) {
//        //try {
//
//        if (newSubs.equals(orig)) {
//            return orig; //nothing changed
//        } else {
//            if (depthRemain <= 0)
//                return newSubs;
//            else
//                return compress(newSubs, depthRemain);
//            //return newSubs;
//        }
//    }

//
//
//    @Nullable
//    public static IntArrayList ints( List<Term> term) {
//        int termSize = term.size();
//        IntArrayList l = new IntArrayList(termSize);
//        for (int i = 0; i < termSize; i++)
//            intOrNull(term.get(i), l);
//        return l;
//    }
//
//

//
//    @Nullable
//    public static void intOrNull( Term term,  IntArrayList target) {
//        if (term.op() == INT) {
//            target.add( ((IntTerm) term).val() );
//        }
//
//    }
//
//
////    public static Set<Task> compress(Term in) {
////
////        if (!in.isBeliefOrGoal()) {
////
////            return Collections.emptySet();
////        } else {
////
////            Set<Task> generated = new HashSet();
////
////            int bdt = in.dt();
////            Op o = in.op();
////
////            //attempt to compress all subterms to a single rule
////            if ((/*(o == EQUI || o == IMPL || */(o == CONJ) && ((bdt == DTERNAL) || (bdt == 0)))
////                    ||
////                    (o.isSet())
////                    ||
////                    (o.isIntersect())) {
////
////                compress(in, (pattern) -> {
////
////                    task(in, pattern, generated);
////
////                });
////            }
////
////
////            //attempt to replace all subterms of an embedded conjunction subterm
////            Compound tn = in.term();
////            if ((o != CONJ && o!=EQUI && o!=IMPL) && tn.subterms().hasAny(CONJ)) {
////
////
////                //attempt to transform inner conjunctions
////                Map<ByteList, Compound> inners = $.newHashMap();
////
////                //Map<Compound, List<ByteList>> revs = $.newHashMap(); //reverse mapping to detect duplicates
////
////                tn.pathsTo(x -> x.op()==CONJ &&
////                        ((((Compound)x).dt() == 0) || ((Compound)x).dt() == DTERNAL)  ? x : null, (p, v) -> {
////                    if (!p.isEmpty())
////                        inners.put(p.toImmutable(), (Compound)v);
////                    return true;
////                });
////
////                //TODO see if duplicates exist and can be merged into one substitution
////
////                inners.forEach((pp,vv) -> {
////                    compress(vv, (fp) -> {
////
////                        Term c;
////                        try {
////                            if ((c = $.terms.transform(tn, pp, fp)) != null) {
////                                task(in, c, generated);
////                            }
////                        } catch (InvalidTermException e) {
////                            logger.warn("{}",e.toString());
////                        }
////
////                    });
////                });
////
////            }
////
////            if (!generated.isEmpty()) {
////                if (trace) {
////                    logger.info("{}\n\t{}", in, Joiner.on("\n\t").join(generated));
////                }
////
////            }
////            return generated;
////        }
////
////    }
//
//
//    static boolean compareNonInteger( Term x,  Term y) {
//        if ((x.op() == INT)) {
//            return (y.op() == INT);
//        } else if (x instanceof Compound) {
//            return x.op() == y.op() && x.size() == y.size() && ((Compound) x).dt() == ((Compound) y).dt();
//        } else {
//            return x.equals(y);
//        }
//    }
//
//    final static Function<Term, Term> xx = x -> x;
//
//    private static boolean equalNonIntegerAtoms( TermContainer subs) {
//        Term first = subs.sub(0);
//        int ss = subs.size();
//        return first.pathsTo(xx, (ByteList p, Term x) -> {
//            for (int i = 1; i < ss; i++) {
//                Term y = subs.sub(i);
//                if (!p.isEmpty()) {
//                    if (!compareNonInteger(x, ((Compound) y).sub(p)))
//                        return false;
//                }/* else {
//                    if (!compareNonInteger(x, y))
//                        return false;
//                }*/
//            }
//            return true;
//        });
//    }
//
//
//    //protected final GenericVariable var(int i, boolean varDep) {
//    //return new GenericVariable(varDep ? Op.VAR_DEP : Op.VAR_INDEP, Integer.toString(i));
//    //}
//
//    
//    private static List<IntInterval> features( List<Term> nnnt) {
//
//        RangeSet<Integer> intIntervals = ranges(nnnt);
//
//        //if (!intIntervals.isEmpty()) {
//        //Range<Integer> rNew = intIntervals.span();
//        //if (rNew.upperEndpoint() - rNew.lowerEndpoint() > 1) {
//
//        //boolean connected = true;
//        //Range q = null;
//
//
//
//        Set<Range<Integer>> srr = intIntervals.asRanges();
//
//        List<IntInterval> ll = $.newArrayList(srr.size());
//
//        for (Range<Integer> rr : srr) {
//            int l = rr.lowerEndpoint();
//            int u = rr.upperEndpoint();
//            if (rr.lowerBoundType() == BoundType.OPEN)
//                l++;
//            if (rr.upperBoundType() == BoundType.OPEN)
//                u--;
//            if (u - l == 0) {
//                //ll.add($.the(l)); //just the individual number
//            } else {
//                ll.add(new IntInterval(l, u));
//            }
//        }
//        //}
//        //}
//
//        //...
//
//        return ll;
//    }
//
//
////    @Nullable Task task(Task b, Term c, Collection<Task> target) {
////
////        if (b.isDeleted())
////            return null;
////
////        Task g = new GeneratedTask(
////                c,
////                b.punc(), b.truth())
////                .time(nar.time(), b.occurrence())
////                .budget(b)
////                .evidence(b.evidence())
////                .log(tag)
////                ;
////        if (g!=null)
////            target.add(g);
////        return g;
////    }
//
//
////    public static <X> Set<X> pluck(List<Term> term, Function<Term,X> p) {
////        Set<X> s = new HashSet();
////        for (Term x : term) {
////            X y = p.apply(x);
////            if (y!=null)
////                s.add(y);
////        }
////        return s;
////    }
//}

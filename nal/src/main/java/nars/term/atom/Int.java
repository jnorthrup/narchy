package nars.term.atom;

import com.google.common.collect.*;
import com.google.common.io.ByteArrayDataOutput;
import jcog.TODO;
import jcog.Util;
import jcog.data.SimpleIntSet;
import jcog.list.FasterList;
import jcog.math.Longerval;
import nars.*;
import nars.subterm.util.SubtermMetadataCollector;
import nars.term.Compound;
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


    /*@Stable*/
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

    public final int id;

    protected static final Int[] pos = new Int[Param.MAX_INTERNED_INTS];
    protected static final Int[] neg = new Int[Param.MAX_INTERNED_INTS];

    static {
        for (int i = 0; i < Param.MAX_INTERNED_INTS; i++) {
            pos[i] = new Int(i);
            neg[i] = new Int(-i);
        }
    }

    public static final Term ZERO = Int.the(0);
    public static final Term ONE = Int.the(1);
    public static final Term TWO = Int.the(2);
    public static final Term NEG_ONE = Int.the(-1);

    public static Intlike range(int from, int to) {
        return ((from == to) ? the(from) :
                new IntRange(from, to));
    }

    final static int INT_ATOM = Term.opX(INT, 0);
    final static int INT_RANGE = Term.opX(INT, 1);


    protected Int(int id, byte[] bytes) {
        this.id = id;
        this.bytesCached = bytes;
    }

    protected Int(int i) {
        this.id = i;
        this.bytesCached = Util.bytePlusIntToBytes(
                IO.opAndSubType(op(), (byte) (((opX()&0xffff)&0b111)>>5)),
                id);
    }

    @Override
    public final void collectMetadata(SubtermMetadataCollector s) {
        s.collectNonVar(op(), hashCode());
    }

    @Override
    public final byte[] bytes() {
        return bytesCached;
    }

    @Override
    public final void appendTo(ByteArrayDataOutput out) {
        out.write(bytesCached);
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
        
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (id >= Param.MAX_INTERNED_INTS && obj instanceof Int) { 
            Int o = (Int) obj;
            
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
            b[1] = 0; 
            Util.intToBytes(min, b, 2);
            Util.intToBytes(min, b, 6);
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
                    continue; 
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
            
            SimpleIntSet s = new SimpleIntSet(ff);
            for (Term x : subs) {
                ((Intlike)x).forEachInt(s::add);
            }
            int ns = s.size();
            assert(ns > 1);
            if (ns ==2) {
                
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

            
            Map<ByteList, Object /*SimpleIntSet*/> data = new LinkedHashMap<>(subs.length);

            
            final boolean[] valid = {true};
            subs[0].pathsTo(x -> x, d -> true, (ByteList p, Term x) -> {
                
                

                ImmutableByteList path = null;
                SimpleIntSet c = null;
                int xVol = x.volume();
                int xStruct = x.structure();
                for (int others = 1; others < subs.length; others++) {
                    Term y = subs[others].subPath(p);

                    if (x.equals(y)) continue;

                    if (!x.hasAny(INT)) {
                        
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

                        
                        if (c == null)
                            c = (SimpleIntSet) data.computeIfAbsent(path, (pp) -> new SimpleIntSet(2));
                        ((Intlike) y).forEachInt(c::add);
                    } else {
                        
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
                    continue; 
                }


                
                /*if (new HashSet(nn).size()==1)*/


                Iterator<Integer> si = s.iterator();

                if (e.getKey().isEmpty()) {
                    
                    features(si, -1).forEachRemaining(yAux::add);
                    entries.remove();
                } else {
                    Iterator<Intlike> iii = features(si, 1);
                    if (iii == null || !iii.hasNext())
                        return _subs; 

                    e.setValue(iii.next());
                }
            }

            Term y;
            if (!data.isEmpty()) {
                y = subs[0];
                for (Map.Entry<ByteList, Object /*Intlike*/> e : data.entrySet()) {
                    Object v = e.getValue();
                    y = y.replaceAt(e.getKey(), (Term) v);
                }
            } else {
                y = null;
            }
            if (subs.length == _subs.length && yAux.isEmpty()) {
                if (y == null)
                    return _subs; 
                return new Term[]{y};
            } else {
                yAux.add(y);
            }
        }


        return intersectResult(factoring, ff, yAux, _subs);






































































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
            for (Term aYAux : yAux) {
                r[j++] = aYAux;
            }
        }
        return sorted(r);
    }


    private static Iterator<Intlike> features(Iterator<Integer> nnnt, int limitCount) {

        RangeSet<Integer> intIntervals = ranges(nnnt);

        
        
        

        
        


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
    static Iterator<Term> unroll(Term cc) {
        
        


        Map<ByteList, IntRange> intervals = new HashMap();

        cc.pathsTo(x -> x instanceof IntRange ? ((IntRange) x) : null, d -> d.hasAny(Op.INT), (ByteList p, IntRange x) -> {
            intervals.put(p.toImmutable(), x);
            return true;
        });

        int dim = intervals.size();
        switch (dim) {

            case 0:
                throw new RuntimeException();

            case 1: 
            {
                Map.Entry<ByteList, IntRange> e = intervals.entrySet().iterator().next();
                IntRange i1 = e.getValue();
                int max = i1.max;
                int min = i1.min;
                List<Term> t = $.newArrayList(1 + max - min);
                for (int i = min; i <= max; i++) {
                    @Nullable Term c1 = cc.replaceAt(e.getKey(), $.the(i));
                    if (c1 != null)
                        t.add(c1);
                }
                return t.iterator();
            }

            case 2: 
                Iterator<Map.Entry<ByteList, IntRange>> ee = intervals.entrySet().iterator();
                Map.Entry<ByteList, IntRange> e1 = ee.next();
                Map.Entry<ByteList, IntRange> e2 = ee.next();
                IntRange i1 = e1.getValue();
                IntRange i2 = e2.getValue();
                int max1 = i1.max, min1 = i1.min, max2 = i2.max, min2 = i2.min;
                List<Term> t = $.newArrayList((1 + max2 - min2) * (1 + max1 - min1));

                for (int i = min1; i <= max1; i++) {
                    for (int j = min2; j <= max2; j++) {
                        Term c1 = cc.replaceAt(e1.getKey(), $.the(i));
                        Term c2 = c1.replaceAt(e2.getKey(), $.the(j));
                        if (!(c2 instanceof Compound))
                            
                            continue;
                        t.add(c2);
                    }
                }
                return t.iterator();

            default:
                
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
                next = min; 
            this.i = Int.the(next);
            return cur;
        }
    }

}















































































































































































































































































































































































































































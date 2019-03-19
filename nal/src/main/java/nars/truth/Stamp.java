/*
 * Stamp.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Pbulic License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http:
 */
package nars.truth;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import jcog.Util;
import jcog.WTF;
import jcog.data.set.MetalLongSet;
import jcog.io.BinTxt;
import jcog.util.ArrayUtils;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.truth.func.TruthFunctions;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.IntFunction;

import static nars.time.Tense.ETERNAL;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

public interface Stamp {


    long[] UNSTAMPED = new long[0];
//    long[] UNSTAMPED_OVERLAPPING = new long[]{Long.MAX_VALUE};

    /*@NotNull*/
    static long[] merge(/*@NotNull*/ long[] a, /*@NotNull*/ long[] b, float aToB, Random rng) {
//        return zip(a, b, aToB,
//                Param.STAMP_CAPACITY,
//                true);

        final int capacity = Param.STAMP_CAPACITY;
        return merge(a, b, rng, aToB, capacity);
    }

    /** applies a fair, random-removal merge of input stamps */
    static long[] merge(long[] a, long[] b, Random rng, float aToB, int capacity) {
        if (a.length == 0) return b;
        if (b.length == 0) return a;
        if (Arrays.equals(a, b))
            return a;

        int abLength = a.length + b.length;
        if (abLength <= capacity) {
            //TODO simple 2-ary case

            if (!Stamp.overlapsAny(a,b)) {
                //simple case: direct sequential merge with no contention
                long[] ab = new long[abLength];
                int ia = 0, ib = 0;
                for (int i = 0; i < ab.length; i++) {
                    long an = ia >= a.length ? Long.MAX_VALUE : a[ia];
                    long bn = ib >= b.length ? Long.MAX_VALUE : b[ib];
                    long abn;
                    if (an < bn) {
                        abn = an; ia++;
                    } else {
                        abn = bn; ib++;
                    }
                    ab[i] = abn;
                }
                return ab;
            } else {
                //duplicates, with no contention
                MetalLongSet ab = new MetalLongSet(a.length + b.length);
                if (a.length > b.length) {
                    //swap so that 'a' is smaller
                    long[] _a = a;
                    a = b;
                    b = _a;
                }
                ab.addAll(a);
                if (!ab.addAll(b))
                    return a; //b is contained entirely within a

                return ab.toSortedArray();
            }
        } else {
            LongArrayList A = Stamp.toList(a);
            if (a.length > 1)
                ArrayUtils.shuffle(A.elements(), rng);
            LongArrayList B = Stamp.toList(b);
            if (b.length > 1)
                ArrayUtils.shuffle(B.elements(), rng);

            MetalLongSet AB = new MetalLongSet(abLength);
            do {
                LongArrayList x = (A!=B && rng.nextFloat() <= aToB) ? A : B;
                AB.add(x.removeLong(x.size()-1));
                if (x.isEmpty()) {
                    if (x == A && x == B)
                        break; //both empty
                    else if (x == A) A = B;
                    else B = A;
                }
            } while (AB.size() < capacity);
            return AB.toSortedArray();
        }
    }

    static LongArrayList toList(long[] a) {
        return new LongArrayList(a);
    }


    /***
     * zips two evidentialBase arrays into a new one
     * assumes a and b are already sorted in increasing order
     * the later-created task should be in 'b'
     */
    /*@NotNull*/
    @Deprecated static long[] zip(long[] a, long[] b, float aToB, int maxLen, boolean newToOld) {

//        if (a.length == 0 || a == Stamp.UNSTAMPED_OVERLAPPING) {
//            if (b.length == 0 || b == Stamp.UNSTAMPED_OVERLAPPING)
//                return Stamp.UNSTAMPED_OVERLAPPING;
//            else
//                return b;
//        } else if (b.length == 0 || b == Stamp.UNSTAMPED_OVERLAPPING) {
//            return a;
//        }

        int aLen = a.length, bLen = b.length;


        int baseLength = Math.min(aLen + bLen, maxLen);


        int aMin = 0, bMin = 0;
        if (aLen + bLen > maxLen) {
            if (!newToOld)
                throw new UnsupportedOperationException("reverse weighted not yet unimplemented");


            if (aToB <= 0.5f) {
                int usedA = Math.max(1, (int) Math.floor(aToB * (aLen + bLen)));
                if (usedA < aLen) {
                    if (bLen + usedA < maxLen)
                        usedA += maxLen - usedA - bLen;
                    aMin = Math.max(0, aLen - usedA);
                }
            } else /* aToB > 0.5f */ {
                int usedB = Math.max(1, (int) Math.floor((1f - aToB) * (aLen + bLen)));
                if (usedB < bLen) {
                    if (aLen + usedB < maxLen)
                        usedB += maxLen - usedB - aLen;
                    bMin = Math.max(0, bLen - usedB);
                }
            }

        }

        long[] c = new long[baseLength];
        if (newToOld) {

            int ib = bLen - 1, ia = aLen - 1;
            for (int i = baseLength - 1; i >= 0; ) {
                boolean ha = (ia >= aMin), hb = (ib >= bMin);


                long next;
                if (ha && hb) {
                    next = (i & 1) > 0 ? a[ia--] : b[ib--];
                } else if (ha) {
                    next = a[ia--];
                } else if (hb) {
                    next = b[ib--];
                } else {
                    throw new RuntimeException("stamp fault");
                }

                c[i--] = next;
            }
        } else {

            int ib = 0, ia = 0;
            for (int i = 0; i < baseLength; ) {

                boolean ha = ia < (aLen - aMin), hb = ib < (bLen - bMin);
                c[i++] = ((ha && hb) ?
                        ((i & 1) > 0) : ha) ?
                        a[ia++] : b[ib++];
            }
        }

        return toSetArray(c, maxLen);
    }

//    static MetalLongSet toSet(Stamp task) {
//        return new MetalLongSet(task.stamp());
//    }
    static MetalLongSet toMutableSet(Stamp task) {
        long[] ts = task.stamp();
        return toMutableSet(ts);
    }

    static MetalLongSet toMutableSet(long[] ts) {
        MetalLongSet s = new MetalLongSet(ts);
        s.trim();
        return s;
    }

//    static ImmutableLongSet toImmutableSet(Stamp task) {
//        return LongSets.immutable.of(task.stamp());
//    }

    /** unsampled, may exceed expected capacity */
    static MetalLongSet toMutableSet(int expectedCap, IntFunction<long[]> t, int n) {
        switch (n) {
            case 0: throw new NullPointerException();
            case 1: return toMutableSet(t.apply(0));
            default:
                MetalLongSet e = new MetalLongSet(expectedCap);
                for (int i = 0; i < n; i++)
                    e.addAll(t.apply(i));
                e.trim();
                return e;
        }
    }

    static MetalLongSet toSet(int expectedCap, long[]... t) {
        MetalLongSet e = new MetalLongSet(expectedCap);
        for (long[] tt : t)
            e.addAll(tt);
        e.trim();
        return e;
    }

    static boolean validStamp(long[] stamp) {
        if (stamp.length > 1) {
            if (stamp.length > Param.STAMP_CAPACITY)
                throw new WTF();
            for (int i = 1, stampLength = stamp.length; i < stampLength; i++) {
                long x = stamp[i];
                if (stamp[i - 1] >= x)
                    return false; //out of order or duplicate
            }
        }
        return true;
    }




    boolean isCyclic();

    void setCyclic(boolean b);

    /*@NotNull*/
    default StringBuilder appendOccurrenceTime(/*@NotNull*/ StringBuilder sb) {
        long oc = start();

        /*if (oc == Stamp.TIMELESS)
            throw new RuntimeException("invalid occurrence time");*/


        if (oc != ETERNAL) {
            int estTimeLength = 8; /* # digits */
            sb.ensureCapacity(estTimeLength);
            sb.append(oc);

            long end = end();
            if (end != oc) {
                sb.append((char) 0x22c8 /* bowtie, horizontal hourglass */).append(end);
            }


        }

        return sb;
    }


    /*@NotNull*/
    default CharSequence stampAsStringBuilder() {

        long[] ev = stamp();
        int len = ev.length;
        int estimatedInitialSize = 8 + (len * 3);

        StringBuilder buffer = new StringBuilder(estimatedInitialSize);
        buffer.append(Op.STAMP_OPENER);

        /*if (creation() == TIMELESS) {
            buffer.append('?');
        } else */
        /*if (!(start() == ETERNAL)) {
            appendTime(buffer);
        } else {*/
        buffer.append(creation());

        buffer.append(Op.STAMP_STARTER).append(' ');

        for (int i = 0; i < len; i++) {

            BinTxt.append(buffer, ev[i]);
            if (i < (len - 1)) {
                buffer.append(Op.STAMP_SEPARATOR);
            }
        }

        if (isCyclic())
            buffer.append('©');

        buffer.append(Op.STAMP_CLOSER);


        return buffer;


    }

    /*@NotNull*/
    static long[] toSetArray(/*@NotNull*/ long[] x) {
        return toSetArray(x, x.length);
    }

    /*@NotNull*/
    static long[] toSetArray(/*@NotNull*/ long[] x, final int outputLen) {
        int l = x.length;


        return (l < 2) ? x : _toSetArray(outputLen, Arrays.copyOf(x, l));
    }


    /*@NotNull*/
    static long[] _toSetArray(int outputLen, /*@NotNull*/ long[] sorted) {


        Arrays.sort(sorted);


        long lastValue = -1;
        int uniques = 0;

        for (long v : sorted) {
            if (lastValue != v)
                uniques++;
            lastValue = v;
        }

        if ((uniques == outputLen) && (sorted.length == outputLen)) {

            return sorted;
        }


        int outSize = Math.min(uniques, outputLen);
        long[] dedupAndTrimmed = new long[outSize];
        int uniques2 = 0;
        long lastValue2 = -1;
        for (long v : sorted) {
            if (lastValue2 != v)
                dedupAndTrimmed[uniques2++] = v;
            if (uniques2 == outSize)
                break;
            lastValue2 = v;
        }
        return dedupAndTrimmed;
    }

    static boolean overlapsAny(/*@NotNull*/ Stamp a, /*@NotNull*/ Stamp b) {
        return ((a == b) || overlapsAny(a.stamp(), b.stamp()));
    }

    /**
     * true if there are any common elements;
     * assumes the arrays are sorted and contain no duplicates
     *
     * @param a evidence stamp in sorted order
     * @param b evidence stamp in sorted order
     */
    static boolean overlapsAny(/*@NotNull*/ long[] a, /*@NotNull*/ long[] b) {

        if (a.length == 0 || b.length == 0)
            return false;

        if (a.length > b.length) {
            //swap to get the larger stamp in the inner loop
            long[] _a = a;
            a = b;
            b = _a;
        }

        /** TODO there may be additional ways to exit early from this loop */

        for (long x : a) {
            for (long y : b) {
                if (y > x)
                    break;
                else if (x == y)
                    return true;
            }
        }
        return false;
    }

    /**
     * the fraction of components in common divided by the total amount of unique components.
     * <p>
     * how much two stamps overlap can be used to estimate
     * the potential for information gain vs. redundancy.
     * <p>
     * == 0 if nothing in common, completely independent
     * >0 if there is at least one common component;
     * 1.0 if they are equal, or if one is entirely contained within the other
     * < 1.0 if they have some component in common
     * <p>
     * assumes the arrays are sorted and contain no duplicates
     */
    static float overlapFraction(long[] a, long[] b) {

        int al = a.length;
        int bl = b.length;

        if (al == 1 && bl == 1) {
            return (a[0] == b[0]) ? 1 : 0;
        }

        if (al > bl) {

            long[] ab = a;
            a = b;
            b = ab;
        }

        int common = overlaps(LongSets.immutable.of(a), b);
        if (common == 0)
            return 0f;

        int denom = Math.min(al, bl);
        assert (denom != 0);

        return Util.unitize(((float) common) / denom);
    }

    static boolean overlapsAny(/*@NotNull*/ MetalLongSet aa,  /*@NotNull*/ long[] b) {
        for (long x : b)
            if (aa.contains(x))
                return true;
        return false;
    }

    static boolean overlaps(Task x, Task y) {
        return (!Param.REVISION_ALLOW_OVERLAP_IF_DISJOINT_TIME || x.intersects(y.start(), y.end()))
                   &&
               Stamp.overlapsAny(x, y);
    }

    static int overlaps(/*@NotNull*/ LongSet aa,  /*@NotNull*/ long[] b) {
        int common = 0;
        for (long x : b) {
            if (aa.contains(x))
                common++;
        }
        return common;
    }



    static int overlapsAdding(/*@NotNull*/ LongHashSet aa,  /*@NotNull*/ long[] b) {
        int common = 0;
        for (long x : b) {
            if (!aa.add(x)) {
                common++;
            }
        }
        return common;
    }

    long creation();

    /** for updating creation times */
    void setCreation(long creation);

    long start();

    long end();


    /**
     * originality monotonically decreases with evidence length increase.
     * it must always be < 1 (never equal to one) due to its use in the or(conf, originality) ranking
     */
    default float originality() {
        return TruthFunctions.originality(stamp().length);
    }

    /**
     * deduplicated and sorted version of the evidentialBase.
     * this can always be calculated deterministically from the evidentialBAse
     * since it is the deduplicated and sorted form of it.
     */
    /*@NotNull*/
    long[] stamp();


    /**
     * returns pair: (stamp, % overlapping)
     */
    static ObjectFloatPair<long[]> zip(List<? extends Stamp> s, int maxLen) {

        int S = s.size();
        assert (S > 0);
        if (S == 1) {
            return pair(s.get(0).stamp(), 0f);
        } else if (S > maxLen) {


            S = maxLen;
        }

        LongHashSet l = new LongHashSet(maxLen);
        int done = 0;

        int repeats = 0;

        int totalEvidence = 0;

        byte[] ptr = new byte[S];
        for (int i = 0, sSize = S; i < sSize; i++) {
            Stamp si = s.get(i);
            long[] x = si.stamp();
//            if (x == UNSTAMPED_OVERLAPPING)
//                continue;
            totalEvidence += x.length;
            ptr[i] = (byte) x.length;
        }
        if (totalEvidence == 0) {
            throw new WTF();
            //return pair(Stamp.UNSTAMPED_OVERLAPPING, 1f);
        }

        List<long[]> stamps = Lists.transform(s, Stamp::stamp);

        int size = 0;
        boolean halted = false;
        main:
        while (done < S && size < maxLen) {
            done = 0;
            for (int i = 0; i < S; i++) {
                long[] x = stamps.get(i);

                int xi = --ptr[i];
                if (xi < 0) {
                    done++;
                    continue;
                }
                if (!l.add(x[xi])) {
                    repeats++;
                } else {
                    size++;
                }

                if (size >= maxLen) {
                    halted = true;
                    break main;
                }
            }
        }

        if (halted) {

            for (int i = 0, ptrLength = ptr.length; i < ptrLength; i++) {
                int rr = ptr[i];
                if (rr >= 0) {
                    long[] ss = stamps.get(i);
                    for (int j = 0; j < rr; j++) {
                        if (l.contains(ss[j]))
                            repeats++;
                    }
                }
            }
        }


        assert (size <= maxLen);


        long[] e = l.toSortedArray();

        float overlap = ((float) repeats) / totalEvidence;


        return pair(e, Util.unitize(overlap));
    }

    static long[] sample(int capacity, MetalLongSet evi, Random rng) {


        int nab = evi.size();
        if (nab <= capacity)
            return evi.toSortedArray();
        else {
            MutableLongList x = evi.toList();
            int toRemove = nab - capacity;
            for (int i = 0; i < toRemove; i++)
                x.removeAtIndex(rng.nextInt(nab--));
            x.sortThis();
            return x.toArray();
        }


    }

//    static long[] sample(int max, long[] e, Random rng) {
//
//        if (e.length > max) {
//            ArrayUtils.shuffle(e, rng);
//            e = ArrayUtils.subarray(e, 0, max);
//        }
//
//        if (e.length > 1)
//            Arrays.sort(e);
//
//        return e;
//    }


}
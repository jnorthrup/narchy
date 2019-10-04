//package nars.control;
//
//import jcog.Util;
//import jcog.data.ShortBuffer;
//import jcog.util.ArrayUtil;
//import nars.Task;
//import org.eclipse.collections.api.set.primitive.ShortSet;
//import org.eclipse.collections.impl.factory.primitive.ShortSets;
//
//import java.util.Arrays;
//
///** cause merge strategies */
//@Deprecated public enum CauseMerge {
//
//    /** appends causes, producing a rolling window */
//    Append {
//        @Override
//        protected short[] apply(short[] existing, short[] incoming, int capacity) {
//            return Why.sample(capacity, false, existing, incoming);
//        }
//
//        @Override
//        protected short[] apply(int capacity, short[]... c) {
//            return Why.sample(capacity, false, c);
//        }
//
//    },
//
//    /** merges only the unique incoming causes */
//    AppendUnique {
//        @Override
//        protected short[] apply(short[] existing, short[] incoming, int capacity) {
//
//            //simple cases:
//            if (existing.length == 1) {
//                if (incoming.length == 1) {
//                    return (existing[0] == incoming[0]) ? existing : new short[] { existing[0], incoming[0] };
//                } else {
//                    short e = existing[0];
//                    for (short i : incoming) {
//                        if (i == e)
//                            return incoming; //incoming contains the only existing
//                    }
//                    return ArrayUtil.prepend(incoming, e);
//                }
//            }
//
//            //general case:
//            ShortBuffer append = null;
//            ShortSet ee = ShortSets.immutable.of(existing);
//            for (int k = 0, incomingLength = incoming.length; k < incomingLength; k++) {
//                short i = incoming[k];
//                if (!ee.contains(i)) {
//                    if (append == null)
//                        append = new ShortBuffer(incoming.length - k);
//                    append.add(i);
//                }
//            }
//            if (append == null)
//                return existing;
//
//            int aa = append.size;
//            if (aa + incoming.length < capacity) {
//                return ArrayUtil.addAll(incoming, append.toArray());
//            } else {
//                return Why.sample(capacity, false, existing, append.toArray());
//            }
//
////            int n = ee.size();
////            int toRemove = n - capacity;
////            if (toRemove <= 0) {
////                return ee.toArray();
////            } else {
////                //random sampling
////                //HACK use RNG parameter
////                MutableShortList x = ee.toList();
////                Random rng = ThreadLocalRandom.current();
////                while (toRemove-- > 0) {
////                    x.removeAtIndex(rng.nextInt(n--));
////                }
////                return x.toArray();
////            }
//        }
//
//        /** TODO customized implementation for merging duplicates */
//        @Override protected short[] apply(int capacity, short[]... c) {
//            return Why.sample(capacity, false, c);
//        }
//    };
//
//
//    @Deprecated public static short[] limit(short[] cause, int cap) {
//        return cause.length <= cap ? cause : ArrayUtil.subarray(cause, 0, cap);
//    }
//
//    abstract protected short[] apply(short[] existing, short[] incoming, int capacity);
//
//    abstract protected short[] apply(int capacity, short[]... c);
//
//    /** binary merge */
//    public final short[] merge(short[] existing, short[] incoming, int capacity) {
//        short[] y;
//        if (existing.length == 0)
//            y = incoming;
//        else if (incoming.length == 0)
//            y = existing;
//        else if (Arrays.equals(existing, incoming))
//            y = existing;
//        else
//            y = apply(existing, incoming, capacity);
//        return limit(y, capacity);
//    }
//
//    /** n-ary merge */
//    public short[] merge(int capacity, short[]... s) {
//
//
//        short[] y;
//
//        int ss = s.length;
//        assert(ss>0);
//        if (ss == 1)
//            y = s[0];
//        else if (ss == 2) {
//            return merge(s[0], s[1], capacity);
//        } else {
//
//            //quick test for all being equal
//            boolean allEqual = true;
//            for (int i = 1; i < ss; i++) {
//                if (!(allEqual = Arrays.equals(s[i - 1], s[i])))
//                    break;
//            }
//            y = allEqual ? s[0] : apply(capacity, s);
//        }
//
//        return limit(y, capacity);
//    }
//
//    @Deprecated public final short[] merge(int causeCapacity, Task... x) {
//        short[] a = x[0].why();
//        short[] y;
//        switch (x.length) {
////            case 0:
////                throw new NullPointerException();
//            case 1:
//                y = a;
//                break;
//            case 2:
//                y = merge(a, x[1].why(), causeCapacity);
//                break;
//            default:
//                y = merge(causeCapacity,
//                        Util.map(Task::why, short[][]::new,
//                                ArrayUtil.removeNulls(x)));
//                break;
//        }
//        return limit(y, causeCapacity);
//    }
//
//
//}

package nars.derive.value;

import jcog.Util;
import jcog.decide.Roulette;
import nars.$;
import nars.Param;
import nars.control.Cause;
import nars.derive.Derivation;
import nars.derive.ForkDerivation;
import nars.derive.Taskify;
import nars.derive.op.UnifyTerm;
import nars.term.pred.AbstractPred;
import nars.term.pred.AndCondition;
import nars.term.pred.PrediTerm;
import org.roaringbitmap.RoaringBitmap;

import java.util.function.Function;

/**
 * AIKR value-determined fork (aka choice-point)
 */
public class ValueFork extends ForkDerivation<Derivation> {

    final Taskify[] conc;


//    private final RoaringBitmap downstream;

    /**
     * the causes that this is responsible for, ie. those that may be caused by this
     */
    public final Cause[] causes;


    public ValueFork(PrediTerm[] branches/*, RoaringBitmap downstream*/) {
        super(branches);

        assert(branches.length > 0);

        conc = Util.map(b->(Taskify) (AndCondition.last(((UnifyTerm.UnifySubtermThenConclude)
                    AndCondition.last(b)
            ).eachMatch)), Taskify[]::new, branches);


        causes = Util.map(c -> c.channel, Cause[]::new, conc);
    }

    @Override
    public boolean test(Derivation d) {

        int before = d.now();

        int N = this.branches.length;
        if (N == 1) {
            this.branches[0].test(d);
            return d.revertLive(before);
        } else {

            float[] w =
                    //Util.marginMax(N, i -> causes[i].value(), 1f / N, 0);
                    Util.softmax(N, i -> causes[i].value(),
                            Param.TRIE_DERIVER_TEMPERATURE);

            Roulette.RouletteUnique.run(w, (b) -> {

                this.branches[b].test(d);

                return d.revertLive(before);

            }, d.random);

            return d.live();
        }
    }



    @Override
    @Deprecated public PrediTerm<Derivation> transform(Function<PrediTerm<Derivation>, PrediTerm<Derivation>> f) {
        //return new ValueFork(PrediTerm.transform(f, branches), valueBranch, downstream);
        throw new UnsupportedOperationException();
    }

//    /**
//     * The number of distinct byte values.
//     */
//    private static final int NUM_BYTE_VALUES = 1 << 8;
//
//    /**
//     * modified from jdk9 source:
//     * Sorts the specified range of the array.
//     *
//     * @param a     the array to be sorted
//     * @param left  the index of the first element, inclusive, to be sorted
//     * @param right the index of the last element, inclusive, to be sorted
//     */
//    static void sort(byte[] a, int left, int right, ByteToFloatFunction v) {
////        // Use counting sort on large arrays
////        if (right - left > COUNTING_SORT_THRESHOLD_FOR_BYTE) {
////            int[] count = new int[NUM_BYTE_VALUES];
////
////            for (int i = left - 1; ++i <= right;
////                 count[a[i] - Byte.MIN_VALUE]++
////                    )
////                ;
////            for (int i = NUM_BYTE_VALUES, k = right + 1; k > left; ) {
////                while (count[--i] == 0) ;
////                byte value = (byte) (i + Byte.MIN_VALUE);
////                int s = count[i];
////
////                do {
////                    a[--k] = value;
////                } while (--s > 0);
////            }
////        } else { // Use insertion sort on small arrays
//        for (int i = left, j = i; i < right; j = ++i) {
//            byte ai = a[i + 1];
//            while (v.valueOf(ai) < v.valueOf(a[j])) {
//                a[j + 1] = a[j];
//                if (j-- == left) {
//                    break;
//                }
//            }
//            a[j + 1] = ai;
//        }
////        }
//    }



    /**
     * remembers the possiblity of a choice which "can" be pursued
     * (ie. according to value rank)
     */
    public static class ValueBranch extends AbstractPred<Derivation> {

        public final int id;

//        /**
//         * global cause channel ID's that this leads to
//         */
//        private final RoaringBitmap downstream;

        public ValueBranch(int id, RoaringBitmap downstream) {
            super($.func("can", /*$.the(id),*/ $.sFast(downstream)));

            this.id = id;
//            this.downstream = downstream;
        }

        @Override
        public float cost() {
            return Float.POSITIVE_INFINITY; //post-condition: must be the last element in any sequence
        }

        @Override
        public boolean test(Derivation derivation) {
            derivation.can.add(id);
            return true;
        }
    }


//        @Override
//        public String toString() {
//            return id + "(to=" + cache.length + ")";
//        }
}

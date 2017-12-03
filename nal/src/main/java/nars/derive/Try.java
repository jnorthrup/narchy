package nars.derive;

import jcog.Util;
import jcog.decide.DecideRoulette;
import nars.$;
import nars.control.Cause;
import nars.control.Derivation;
import org.roaringbitmap.RoaringBitmap;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * set of branches, subsets of which a premise "can" "try"
 */
public class Try extends AbstractPred<Derivation> {

//    public final ValueCache cache;
    public final PrediTerm<Derivation>[] branches;
    public final Cause[] causes;

    Try(PrediTerm<Derivation>[] branches, Cause[] causes) {
        super($.func("try", branches));
        this.branches = branches;
        this.causes = causes;
    }

    public Try(ValueFork[] branches) {
        this(branches, Stream.of(branches).flatMap(b -> Stream.of(b.causes)).toArray(Cause[]::new));
    }

    @Override
    public PrediTerm<Derivation> transform(Function<PrediTerm<Derivation>, PrediTerm<Derivation>> f) {
        return new Try(
                PrediTerm.transform(f, branches), causes
        );
    }



    @Override
    public boolean test(Derivation d) {

        RoaringBitmap choices = d.preToPost;
        int N = choices.getCardinality();
        switch (N) {
            case 0:
                return false;
            case 1:

                branches[choices.first()].test(d);

                break;
            default:

                int[] c = choices.toArray();
                float[] val = Util.map(N, (x) ->
                    (float)Math.exp(   //softmax
                            Util.sum(Cause::value, ((ValueFork)(branches[c[x]])).causes) //sum of downstream cause values
                ) );

                int before = d.now();
                DecideRoulette.selectRouletteUnique(N, i -> val[i], (i) -> {
                    branches[c[i]].test(d);
                    return d.revertLive(before);
                }, d.random);

                break;
        }

        choices.clear();

        return false;
    }

//    public int tryBranch(Derivation d, short[] routing, int sample) {
////        float branchScore =
////                minVal!=maxVal ? ((float) (g2(routing, sample, VAL)) - minVal) / (maxVal - minVal) : 0.5f;
//        int loopBudget = g2(routing, sample, VAL); //Util.lerp(branchScore, minPerBranch, maxPerBranch);
//        if (loopBudget < Param.TTL_PREMISE_MIN)
//            return -1;
//
//
//        int ttlSaved = d.getAndSetTTL(loopBudget) - loopBudget - 1;
//
//        int n = g2(routing, sample, KEY);
//
////        System.out.println(d.time + " " + d.ttl + " " + d.task + " " + d.belief + " "+ d.beliefTerm + " " + n);
////        //TrieDeriver.print(branches[n]);
////        System.out.println(branches[n]);
////        System.out.println();
//
//        branches[n].test(d);
//
//        return ttlSaved;
//    }


    /**
     * get
     */
    private static short g2(short[] s, int i, boolean firstOrSecond) {
        return s[i * 2 + (firstOrSecond ? 0 : 1)];
    }

    private static void a2(short[] s, int i, boolean firstOrSecond, short amt) {
        s[i * 2 + (firstOrSecond ? 0 : 1)] += amt;
    }

    /**
     * put
     */
    private static short p2(short[] s, int i, boolean firstOrSecond, short newValue) {
        int ii = i * 2 + (firstOrSecond ? 0 : 1);
        short prev = s[ii];
        s[ii] = newValue;
        return prev;
    }

    private static final boolean KEY = true;
    private static final boolean VAL = false;
//
//    /**
//     * set
//     */
//    private static void s2(short[] s, int i, boolean firstOrSecond, short newValue) {
//        s[i * 2 + (firstOrSecond ? 0 : 1)] = newValue;
//    }
//
//    private static void bingoSortPairwise(short[] A, int[] range) {
//        /*
//        https://en.wikipedia.org/wiki/Selection_sort
//        In the bingo sort variant, items are ordered by repeatedly
//        looking through the remaining items to find the greatest
//        value and moving all items with that value to their final location.
//        [2] Like counting sort, this is an efficient variant if
//        there are many duplicate values.
//        Indeed, selection sort does one pass through
//        the remaining items for each item moved.
//        Bingo sort does one pass for each value (not item):
//        after an initial pass to find the biggest value,
//        the next passes can move every item with that value to
//        its final location while finding the next value
//*/
////{ This procedure sorts in ascending order. }
//        int max = (A.length - 1) / 2;
//
//    /* The first iteration is written to look very similar to the subsequent ones, but
//      without swaps. */
//        short vm = g2(A, max, VAL); //   nextValue := A[max];
//        for (int i = max - 1; i >= 0; i--) { //    for i := max - 1 downto 0 do
//            short vi;
//            if ((vi = g2(A, i, VAL)) > vm) //        if A[i] > nextValue then
//                vm = vi;
//        }
//        range[1] = vm;
//        while (max >= 0 && g2(A, max, VAL) == vm) max--;
//        while (max >= 0) { //    while max > 0 do begin
//            float value = vm;
//            vm = g2(A, max, VAL);
//            for (int i = max - 1; i >= 0; i--) {  //for i:=max - 1 downto 0 do
//                short vi = g2(A, i, VAL);
//                if (vi == value) {
//                    //swap(A[i], A[max]);
//                    short ki = g2(A, i, KEY);
//                    short km = g2(A, max, KEY);
//                    s2(A, i, KEY, km);
//                    s2(A, i, VAL, vm);
//                    s2(A, max, KEY, ki);
//                    s2(A, max, VAL, vi);
//                    max--;
//                } else if (vi > vm)
//                    vm = vi;
//            }
//            while (max >= 0 && g2(A, max, VAL) == vm)
//                max--;
//        }
//        range[0] = g2(A, 0, VAL);
//
//    }
}


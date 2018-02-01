package nars.derive;

import com.google.common.base.Joiner;
import jcog.Util;
import jcog.decide.Roulette;
import nars.Param;
import nars.control.Cause;
import nars.control.Derivation;
import nars.term.Term;
import nars.term.pred.PrediTerm;
import org.apache.commons.lang3.ArrayUtils;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * set of branches, subsets of which a premise "can" "try"
 */
public class Try implements Consumer<Derivation> {

//    public final ValueCache cache;
    public final PrediTerm<Derivation>[] branches;
    public final Cause[] causes;

    Try(PrediTerm<Derivation>[] branches, Cause[] causes) {
//        super($.func("try",
//                branches.length < 127 ? branches :
//                new Term[] {  $.the(Arrays.toString(branches)) } //HACK
//        ));
        this.branches = branches;
        this.causes = causes;
    }

    public Try(ValueFork[] branches) {
        this(branches,
             Stream.of(branches).flatMap(b -> Stream.of(b.causes)).toArray(Cause[]::new));
    }

    public Try transform(Function<PrediTerm<Derivation>, PrediTerm<Derivation>> f) {
        return new Try(
                PrediTerm.transform(f, branches), causes
        );
    }


    @Override
    public String toString() {
        return Joiner.on(",").join(branches);
    }

    @Override
    public void accept(Derivation d) {

        int[] choices = d.will;
        int N = choices.length;
        switch (N) {
            case 0:
                throw new RuntimeException("zero causes should not even reach here");
            case 1:

                branches[choices[0]].test(d);

                break;
            default:

                forkRoulette(d, choices,
                        1f/N
                        //1f/((float)Math.sqrt(N))
                );
                //forkTTLBudget(d, choices);

                break;
        }
    }

    /** TTL and the value of each decides budget to allocate to each branch. then these are tried in shuffled order */
    public void forkTTLBudget(Derivation d, int[] choices) {

        float ttlTotal = d.ttl;
        int n = choices.length;
        float[] w = Util.marginMax(n, x ->
                        valueSum(choices[x]), //sum of downstream cause values
                1f / n, 0
        );
        float valueSum = Util.sum(w);

        int[] order = new int[n];
        for (int i = 0; i < n; i++) order[i] = i;
        ArrayUtils.shuffle(order, d.random);

        int k = 0;
        int start = d.now();
        while (d.ttl > 0) {
            int ttlSave = d.ttl;

            int c = order[(k++)%n];
            int ttlFrac = Math.max(Param.TTL_MIN(), Math.round(ttlTotal * w[c]/valueSum));
            d.ttl = ttlFrac;

            branches[choices[c]].test(d);
            d.revert(start);

            int ttlUsed = Math.max(1, ttlSave - d.ttl);
            d.ttl = ttlSave - ttlUsed;
        }

    }

    public void forkRoulette(Derivation d, int[] choices, float reserve) {
        int n = choices.length;
        float[] w = Util.marginMax(n, x ->
                        valueSum(choices[x]), //sum of downstream cause values
                1f / n, 0
        );

        int before = d.now();
        Roulette.selectRouletteUnique(n, i -> w[i], (i) -> {
            int ttlSave = d.ttl;

            int ci = choices[i];

            //int fanout = ((ValueFork)(branches[ci])).causes.length;
            int ttlFrac =
                    Math.round(ttlSave*reserve);
                    //Math.min(ttlSave, Math.max(fanout * Param.TTL_MIN, Math.round(ttlSave*reserve)));
            d.ttl = ttlFrac;

            branches[ci].test(d);
            d.revert(before);

            int ttlUsed = Math.max(1, ttlFrac - d.ttl);

            return (d.ttl = ttlSave - ttlUsed) > 0;

        }, d.random);
    }

    public float valueSum(int choice) {
        return Util.sum(Cause::value, ((ValueFork)(branches[choice])).causes);
    }

    public void recurseTerms(Consumer<Term> each) {
        for (PrediTerm p : branches)
            p.recurseTerms(each);
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

//
//    /**
//     * get
//     */
//    private static short g2(short[] s, int i, boolean firstOrSecond) {
//        return s[i * 2 + (firstOrSecond ? 0 : 1)];
//    }
//
//    private static void a2(short[] s, int i, boolean firstOrSecond, short amt) {
//        s[i * 2 + (firstOrSecond ? 0 : 1)] += amt;
//    }
//
//    /**
//     * put
//     */
//    private static short p2(short[] s, int i, boolean firstOrSecond, short newValue) {
//        int ii = i * 2 + (firstOrSecond ? 0 : 1);
//        short prev = s[ii];
//        s[ii] = newValue;
//        return prev;
//    }
//
//    private static final boolean KEY = true;
//    private static final boolean VAL = false;
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


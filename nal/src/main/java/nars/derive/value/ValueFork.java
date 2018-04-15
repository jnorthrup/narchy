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

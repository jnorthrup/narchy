package nars.derive.control;

import jcog.decide.MutableRoulette;
import nars.Param;
import nars.control.Cause;
import nars.derive.Derivation;
import nars.term.control.PrediTerm;
import org.eclipse.collections.api.block.function.primitive.ObjectIntToObjectFunction;

import java.util.function.Function;

/**
 * AIKR value-determined fork (aka choice-point)
 */
public class ValueFork extends ForkDerivation<Derivation> {



    /**
     * the causes that this is responsible for, ie. those that may be caused by this
     */
//    final PrediTerm<Derivation>[] conc;
    public final Cause[] causes;

    /**
     * weight vector generation function
     */
    private final Function<Derivation, float[]> value;

    /**
     * choice id to branch mapping function
     */
    private final ObjectIntToObjectFunction<Derivation, PrediTerm<Derivation>> branchChoice;


    public ValueFork(PrediTerm[] branches, Cause[] causes, Function<Derivation, float[]> value, ObjectIntToObjectFunction<Derivation, PrediTerm<Derivation>> choiceToBranch) {
        super(branches);

        assert (branches.length > 0);

        this.causes = causes;
        this.value = value;
        this.branchChoice = choiceToBranch;
    }

    @Override
    public boolean test(Derivation d) {

        int before = d.now();

        float[] weights = value.apply(d);
        int N = weights.length;

        assert (N > 0);
        MutableRoulette.run(weights, d.random,

            wi -> wi / 2 /* harmonic decay */,

            b -> {

                branchChoice.valueOf(d, b).test(d); //fork's return value ignored

                return d.revertLive(before, Param.TTL_BRANCH);

            }

        );
        return true;
    }


//    void forkRoulette(Derivation d, short[] choices) {
//        int N = choices.length;
//        float[] w =
//                //Util.marginMax(N, x -> valueSum(choices[x]), 1f / N, 0);
//                Util.softmax(N, i -> branchValue(branches[choices[i]]), Param.TRIE_DERIVER_TEMPERATURE);
//
//        //System.out.println(Arrays.toString(choices) + " " + Arrays.toString(w));
//
//        int before = d.now();
//        MutableRoulette.run(w, wi -> wi/N /* harmonic decay */, (i) -> {
//            int ttlStart = d.ttl;
//
//
//            int fanout = ((ValueFork)(branches[choices[i]])).causes.length;
//            int ttlSpend = Math.max(
//                    Math.round(ttlStart * spendRatePerBranch * fanout * spendRatePerFanOut),
//                    Param.TTL_MIN);
//            //Math.min(ttlSave, Math.max(fanout * Param.TTL_MIN, Math.round(ttlSave*reserve)));
//
//            d.ttl = ttlSpend;
//
//            branches[choices[i]].test(d);
//
//            int ttlUsed = Math.max(1, ttlSpend - d.ttl);
//
//            return ((d.ttl = ttlStart - ttlUsed - Param.TTL_BRANCH) > 0 && d.revertLive(before));
//        }, d.random);
//    }

    @Override
    @Deprecated
    public PrediTerm<Derivation> transform(Function<PrediTerm<Derivation>, PrediTerm<Derivation>> f) {
        //return new ValueFork(PrediTerm.transform(f, branches), valueBranch, downstream);
        throw new UnsupportedOperationException();
    }

//    @Override
//    public void accept(Derivation d) {
//
//        short[] choices = d.will;
//        int N = choices.length;
//        switch (N) {
//            case 0:
//                throw new RuntimeException("zero causes should not even reach here");
//            case 1:
//
//                branches[choices[0]].test(d);
//
//                break;
//            default:
//
//
//                forkRoulette(d, choices
//                        //1f
//                        //1f/N
//                        //1f/((float)Math.sqrt(N))
//                );
//                //forkTTLBudget(d, choices);
//
//                break;
//        }
//    }
//

}

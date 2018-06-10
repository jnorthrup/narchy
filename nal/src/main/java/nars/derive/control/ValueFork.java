package nars.derive.control;

import jcog.decide.MutableRoulette;
import nars.Param;
import nars.control.Cause;
import nars.derive.Derivation;
import nars.term.control.Fork;
import nars.term.control.PrediTerm;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;

import java.util.function.Function;

/**
 * AIKR value-determined fork (aka choice-point)
 */
public class ValueFork extends Fork<Derivation> {



    /**
     * the causes that this is responsible for, ie. those that may be caused by this
     */

    /*@Stable*/
    public final Cause[] causes;

    /**
     * weight vector generation function
     */
    private final Function<Derivation, float[]> value;

    /**
     * choice id to branch mapping function
     */
    private final ObjectIntProcedure<Derivation> branchChoice;


    public ValueFork(PrediTerm[] branches, Cause[] causes, Function<Derivation, float[]> value, ObjectIntProcedure<Derivation> choiceToBranch) {
        super(branches);

        assert (branches.length > 0);

        this.causes = causes;
        this.value = value;
        this.branchChoice = choiceToBranch;
    }

    @Override
    public boolean test(Derivation d) {

        if (d.ttl < Param.TTL_MIN)
            return false;


        float[] paths = value.apply(d);
        int fanOut = paths.length;
        assert(fanOut > 0);

        int before = d.now();

        if (fanOut == 1) {


            branchChoice.value(d, 0);

            return d.revertLive(before, Param.TTL_BRANCH);

        } else {

            MutableRoulette.run(paths, d.random,

                    wi -> 0      

                    , b -> {

                        int beforeTTL = d.ttl;
                        int subBudget = beforeTTL / fanOut;
                        if (subBudget < Param.TTL_MIN)
                            return false;

                        int dTtl = subBudget;

                        branchChoice.value(d, b); 

                        int spent = subBudget - dTtl;

                        return d.revertLive(before, Param.TTL_BRANCH + Math.max(spent, 0));
                    }
            );
            return true;
        }
    }































    @Override
    @Deprecated
    public PrediTerm<Derivation> transform(Function<PrediTerm<Derivation>, PrediTerm<Derivation>> f) {
        
        throw new UnsupportedOperationException();
    }





























}

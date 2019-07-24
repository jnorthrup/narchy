package nars.derive.model;

import jcog.Util;
import jcog.data.ShortBuffer;
import jcog.decide.Roulette;
import nars.Op;
import nars.derive.rule.DeriveAction;
import nars.derive.rule.PostDerivable;
import nars.term.Term;
import nars.truth.Truth;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/** contains only information which depends on the premise itself (Task, Belief, BeliefTerm).
 * used for first stage winnowing to determine the (memoizable) set of possible forkable outcomes */
public abstract class PreDerivation extends Unify {


    public Term taskTerm;
    public Term beliefTerm;
    public byte taskPunc;

    public Truth taskTruth;
    public Truth beliefTruth_at_Belief, beliefTruth_at_Task;

    /**
     * choices mapping the available post targets
     */
    public final ShortBuffer canCollector = new ShortBuffer(256);


    static final int MAX_FANOUT = 64;
	/**
	 * post-derivation lookahead buffer
	 */
	final PostDerivable[] post;


    protected PreDerivation(@Nullable Op type, Random random, int stackMax) {
        super(type, random, stackMax);
        post = Util.map(MAX_FANOUT, PostDerivable[]::new, i->new PostDerivable(this));
    }

    public boolean hasBeliefTruth() {
        return beliefTruth_at_Belief !=null || beliefTruth_at_Task !=null;
    }

    public abstract ShortBuffer preDerive();

    public static boolean run(Derivation d, final int deriveTTL) {

        short[] can = d.deriver.what(d);

        int n = can.length;
        if (n == 0)
            return false;

        d.preReady();

        d.runPostDerivable(d, can, deriveTTL);

        return true;
    }

    void runPostDerivable(Derivation d, short[] can, int deriveTTL) {

        d.ready(deriveTTL); //first come first serve, maybe not ideal

        DeriveAction[] branch = d.deriver.rules.branch;
        int valid = 0, lastValid = -1;
        for (int i = 0; i < can.length; i++) {
            if ((post[i].priSet(branch[can[i]], d)) > 0) {
                lastValid = i;
                valid++;
            }
        }


        switch (valid) {
            case 0: break;
            case 1:
                //optimized 1-option case
                //while (branch[can[lastValid]].run(post[lastValid])) { }
                while (post[lastValid].run()) { }
                break;
            default:
                Arrays.sort(post, sortByPri);
                int n = valid;
                int j;
                do {
                    j = Roulette.selectRoulette(n, i -> post[i].pri, d.random);
                } while (post[j].run());

                //MutableRoulette.run(pri, d.random, wi -> 0, i -> branch[can[i]].run(post[i]));
                break;
        }
    }

    private static final Comparator<? super PostDerivable> sortByPri = (a, b)->{
        if (a==b) return 0;
        int i = Float.compare(a.pri, b.pri);
        if (i != 0) return -i;
        else return Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
    };

}

package nars.derive.model;

import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.decide.MutableRoulette;
import nars.Op;
import nars.derive.rule.DeriveAction;
import nars.derive.rule.PostDerivable;
import nars.term.Term;
import nars.truth.Truth;
import nars.unify.Unify;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;
import org.jetbrains.annotations.Nullable;

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
    public final ShortArrayList canCollector = new ShortArrayList();


    static final int MAX_FANOUT = 64;
    /** post-derivation lookahead buffer */
    final PostDerivable post[];


    protected PreDerivation(@Nullable Op type, Random random, int stackMax) {
        super(type, random, stackMax);
        post = Util.map(MAX_FANOUT, PostDerivable[]::new, i->new PostDerivable(this));
    }

    public boolean hasBeliefTruth() {
        return beliefTruth_at_Belief !=null || beliefTruth_at_Task !=null;
    }

    public abstract short[] preDerive();

    public static boolean run(Derivation d, final int deriveTTL) {

        short[] maybe = d.deriver.what(d);

        if (maybe.length == 0)
            return false;

        d.preReady();

        DeriveAction[] branch = d.deriver.rules.branch;

        float[] pri;
        short[] can;

        if (maybe.length == 1) {
            if (branch[maybe[0]].value(d) <= 0)
                return false;

            can = maybe;
            pri = null; //not used

        } else /* could.length > 1 */ {

            int n = maybe.length;
            float[] f = new float[n];
            MetalBitSet toRemove = null;
            for (int choice = 0; choice < n; choice++) {
                float fc = branch[maybe[choice]].value(d);
                if (fc <= 0) {
                    if (toRemove == null) toRemove = MetalBitSet.bits(n);
                    toRemove.set(choice);
                }
                f[choice] = fc;
            }

            if (toRemove == null) {
                can = maybe; //all
                pri = maybe.length > 1 ? f : null;
            } else {
                int r = toRemove.cardinality();
                if (r == n)
                    return false; //all removed; nothing remains
                /*else if (r == n-1) {*/
                //TODO all but one

                int nn = n - r;

                pri = new float[nn];
                can = new short[nn];
                int nc = 0;
                for (int i = 0; i < n; i++) {
                    if (toRemove.getNot(i)) {
                        pri[nc] = f[i];
                        can[nc++] = maybe[i];
                    }
                }
            }
        }

        d.ready(maybe, deriveTTL);

        if (can.length == 1) {
            branch[can[0]].test(d);
        } else {

            //runRoulette(d, branch, pri, can);
            d.runPostDerivable(d, branch, pri, can);

        }

        return true;
    }

    static void runRoulette(Derivation d, DeriveAction[] branch, float[] pri, short[] can) {
        MutableRoulette.run(pri, d.random, wi -> 0, i -> branch[can[i]].test(d));
    }
    void runPostDerivable(Derivation d, DeriveAction[] branch, float[] pri, short[] can) {
        int n = can.length;
        int valid = 0, lastValid = -1;
        for (int i = 0; i < can.length; i++) {
            PostDerivable p = post[i];
            if (!p.set(branch[can[i]], d))
                pri[i] = 0;
            else {
                if ((pri[i] = p.value(pri[i])) > 0) {
                    lastValid = i;
                    valid++;
                }
            }
        }
        switch (valid) {
            case 0: break;
            case 1:
                branch[can[lastValid]].test(post[lastValid]);
                break;
            //TODO 1-ary
            default:
                MutableRoulette.run(pri, d.random, wi -> 0, i -> branch[can[i]].test(post[i]));
                break;
        }
    }
}

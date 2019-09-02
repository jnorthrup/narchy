package nars.derive;

import jcog.Util;
import jcog.data.ShortBuffer;
import jcog.decide.MutableRoulette;
import nars.Op;
import nars.term.Term;
import nars.truth.MutableTruth;
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

    public final MutableTruth taskTruth = new MutableTruth();
    public final MutableTruth beliefTruth_at_Belief = new MutableTruth();
    public final MutableTruth beliefTruth_at_Task = new MutableTruth();

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
        return beliefTruth_at_Belief.set() || beliefTruth_at_Task.set();
    }

    public abstract ShortBuffer preDerive();


    public boolean run(int deriveTTL) {

        Derivation d = (Derivation) this; //HACK

        short[] can = d.deriver.what(d);
        if (can.length == 0)
            return false;

        d.preReady();


        DeriveAction[] branch = d.deriver.rules.branch;
        int valid = 0, lastValid = -1;
        PostDerivable[] post = this.post;
        for (int i = 0; i < can.length; i++) {
            if ((post[i].priSet(branch[can[i]], d)) > Float.MIN_NORMAL) {
                lastValid = i;
                valid++;
            }
        }
        if (valid == 0)
            return false;

        d.ready(deriveTTL); //first come first serve, maybe not ideal


        switch (valid) {
            case 1:
                //optimized 1-option case
                //while (post[lastValid].run()) { }
                post[lastValid].run();
                break;
            default:
                if (valid < can.length) {
                    //sort the valid to the first slots for fastest roulette iteration on the contiguous valid subset
                    Arrays.sort(post, 0, can.length, sortByPri);
                } //otherwise any order here is valid

//                int j;
//                do {
//                    j = Roulette.selectRoulette(valid, i -> post[i].pri, d.random);
//                } while (post[j].run());

                float[] pri = new float[valid];
                for (int i = 0; i < valid; i++)
                    pri[i] = post[i].pri;
                MutableRoulette.run(pri, d.random, wi -> 0, i -> post[i].run());
                break;
        }
        return true;
    }

    private static final Comparator<? super PostDerivable> sortByPri = (a, b)->{
        if (a==b) return 0;
        int i = Float.compare(a.pri, b.pri);
        return i != 0 ? -i : Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
    };

}

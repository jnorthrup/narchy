package nars.derive.premise;

import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.PLinkArrayBag;
import jcog.pri.bag.impl.hijack.PriLinkHijackBag;
import jcog.pri.op.PriMerge;
import nars.Task;
import nars.derive.model.Derivation;
import nars.link.TaskLinks;
import nars.time.When;

/** premise buffer with novelty filter
 *  thread-safe; for cooperative use by multiple threads
 *  UNTESTED
 */
public class PremiseBuffer extends PremiseSource {

    static final int premiseBufferCapacity = 32;

    /** rate that priority from the novelty bag subtracts from potential premises.
     * may need to be divided by concurrency so that threads dont step on each other */
    float notNovelCost = 0.5f;

    /** search rate */
    public float fillRate = 1.5f;

    /** active premises for sampling */
    public final Bag<Premise,PriReference<Premise>> premise;

    /** tracks usage, the anti-premise novelty filter */
    public final Bag<Premise,PriReference<Premise>> premiseTried;

    //TODO premise blacklist to detect pointless derivations that shouldnt even be considered, per-Deriver

    public PremiseBuffer() {

        this.premise =
                new PLinkArrayBag<>(PriMerge.max, premiseBufferCapacity);
                //new PriLinkHijackBag<>(PriMerge.max, premiseBufferCapacity, 3);
        this.premiseTried =
                new PriLinkHijackBag<>(PriMerge.max, premiseBufferCapacity, 2);
    }

    @Override public void derive(When when, int premisesPerIteration, int termlinksPerTaskLink, int matchTTL, int deriveTTL, TaskLinks links, Derivation d) {

        d.what.sample(d.random, (int) Math.max(1, Math.ceil((fillRate * premisesPerIteration) / termlinksPerTaskLink)), tasklink -> {

            Task task = tasklink.get(when);
            if (task != null && !task.isDeleted()) {
                float linkPri = tasklink.priPunc(task.punc());
                hypothesize(tasklink, task, termlinksPerTaskLink, links, d, (premise) -> {
                    this.premise.put(new PLink<>(premise, linkPri));
                });
            }

            return true;

        });

        premise.sample(d.random, premisesPerIteration, pp->{
            fire(pp, matchTTL, deriveTTL, d);
        });

    }

    public void fire(PriReference<Premise> pp, int matchTTL, int deriveTTL, Derivation d) {
        Premise P = pp.get();
        PriReference<Premise> existing = premiseTried.put(pp);
        if (existing!=null && existing!=pp)
            pp.priSub(existing.pri() * notNovelCost);

        P.derive(d, matchTTL, deriveTTL);
    }


    public void commit() {
        premise.commit();
        premiseTried.commit();
    }
}

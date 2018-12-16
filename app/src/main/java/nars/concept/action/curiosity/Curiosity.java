package nars.concept.action.curiosity;

import jcog.data.list.FasterList;
import jcog.decide.MutableRoulette;
import jcog.math.FloatRange;
import jcog.math.MutableEnum;
import nars.Param;
import nars.agent.NAgent;
import nars.concept.action.AbstractGoalActionConcept;
import nars.task.Revision;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/** a curiosity configuration which can be shared by multiple AbstractGoalActionConcept's */
public class Curiosity {

    public final AtomicBoolean enable = new AtomicBoolean(true);

    /** advised default or maximum confidence setting of generated goal */
    public final FloatRange conf = new FloatRange(0, Param.TRUTH_EPSILON, Param.TRUTH_MAX_CONF);

    public final FasterList<CuriosityMode> curiosity = new FasterList<>(8); //new FastCoWList(8, CuriosityMode[]::new);

    private final IntToFloatFunction pri = i -> curiosity.get(i).priElseZero();
    public final NAgent agent;

    public final AtomicBoolean goal = new AtomicBoolean(true);

    /** activation probabilitiy */
    public final FloatRange rate = new FloatRange(0, 0, 1f);


    public enum CuriosityInjection {
        Passive {
            @java.lang.Override
            public Truth inject(Truth actionTruth, Truth curi) {
                return actionTruth;
            }
        },
        Revise {
            @java.lang.Override
            public Truth inject(Truth actionTruth, Truth curi) {
                return actionTruth == null ? curi : (curi == null ?  actionTruth : Revision.revise(curi, actionTruth));
            }
        },
        Override {
            @java.lang.Override
            public Truth inject(Truth actionTruth, Truth curi) {
                return curi!=null ? curi : actionTruth;
            }
        };


        abstract public Truth inject(Truth dex, Truth curi);
    }


    /** injection mode */
    public final MutableEnum<CuriosityInjection> injection = new MutableEnum<>(
        CuriosityInjection.Revise
    );



    private MutableRoulette select = null;

    public Curiosity(NAgent a, float initialRate) {

        this.agent = a;

        this.rate.set(initialRate);

        update();
        a.onFrame(this::update);

    }

    private void update() {
        if (!enable.getOpaque())
            return;

        int cc = curiosity.size();

        if (select == null || select.size()!= cc) {
            if (cc == 0)
                select =null;
            else
                select = new MutableRoulette(cc, pri, agent.nar().random());
        } else {
            select.reweigh(pri);
        }
    }

    /** returns curiosity goal to override goal output, or null if not curious.  applied to each
     * acction concept per frame
     */
    @Nullable public final Truth curiosity(AbstractGoalActionConcept concept) {
        return (select != null && enable.getOpaque() && agent.random().nextFloat() < rate.floatValue()) ?
                curiosity.get(select.next()).get(concept, this) : null;
    }

    public Curiosity add(CuriosityMode m) {
        curiosity.add(m);
        return this;
    }

}

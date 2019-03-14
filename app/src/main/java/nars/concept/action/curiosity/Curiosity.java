package nars.concept.action.curiosity;

import jcog.Skill;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.decide.MutableRoulette;
import jcog.math.FloatRange;
import jcog.math.MutableEnum;
import nars.NAR;
import nars.Param;
import nars.agent.NAgent;
import nars.concept.action.AbstractGoalActionConcept;
import nars.task.Revision;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** a curiosity configuration which can be shared by multiple AbstractGoalActionConcept's */
@Skill({"Curiosity", "Central_pattern_generator","Phantom_limb"})
public class Curiosity {

    public final AtomicBoolean enable = new AtomicBoolean(true);

    /** advised default or maximum confidence setting of generated goal */
    public final FloatRange conf = new FloatRange(0, Param.TRUTH_EPSILON, Param.TRUTH_CONF_MAX);

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

        a.onFrame((Consumer<NAR>)this::update);

    }

    private void update(NAR nar) {
        if (!enable.getOpaque())
            return;

        float curiConf =
                //nar.confMin.floatValue();
                //nar.confMin.floatValue() * 2;
                nar.confMin.floatValue() * 4;
                //Util.lerp(1/8f, nar.confMin.floatValue(), Param.TRUTH_MAX_CONF);
                //nar.confDefault(GOAL)/4;
                //nar.confDefault(GOAL)/3;
                //nar.confDefault(GOAL)/2;
                //nar.confDefault(GOAL)/3;
                //w2c(c2w(nar.confDefault(GOAL))/3);
                //w2c(c2w(nar.confDefault(GOAL))/2);
                //nar.confDefault(GOAL);

        conf.set(Util.clamp(curiConf, nar.confMin.floatValue(), Param.TRUTH_CONF_MAX));

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

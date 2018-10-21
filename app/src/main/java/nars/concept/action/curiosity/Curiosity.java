package nars.concept.action.curiosity;

import jcog.data.list.FasterList;
import jcog.decide.MutableRoulette;
import jcog.math.FloatRange;
import nars.Param;
import nars.agent.NAgent;
import nars.concept.action.AbstractGoalActionConcept;
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

    private final IntToFloatFunction weigher = i -> curiosity.get(i).weight.floatValue();
    public final NAgent agent;

    private MutableRoulette select = null;

    public Curiosity(NAgent a) {

        this.agent = a;

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
                select = new MutableRoulette(cc, weigher, agent.nar().random());
        } else {
            select.reweigh(weigher);
        }
    }

    /** returns curiosity goal to override goal output, or null if not curious.  applied to each
     * acction concept per frame
     */
    @Nullable public final Truth curiosity(AbstractGoalActionConcept concept) {
        return (enable.getOpaque() && select != null) ?
                curiosity.get(select.next()).get(concept, this) : null;
    }

    public Curiosity add(CuriosityMode m) {
        curiosity.add(m);
        return this;
    }

}

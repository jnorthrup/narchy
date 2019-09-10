package nars.agent;

import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import nars.attention.AttnBranch;
import nars.concept.sensor.ScalarSignal;
import nars.concept.sensor.Signal;
import nars.term.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleReward extends ScalarReward {

    private static final Logger logger = LoggerFactory.getLogger(SimpleReward.class);
    private final FloatSupplier rewardFunc;

    public SimpleReward(Term id, float freq, FloatSupplier r, Game a) {
        super(id, freq, a);

        this.rewardFunc = r;
//        TermLinker linker = new TemplateTermLinker((TemplateTermLinker) TemplateTermLinker.of(id)) {
//            @Override
//            public void sample(Random rng, Function<? super Term, SampleReaction> each) {
//                if (rng.nextFloat() < 0.9f) {
//                    if ((each.apply(a.actions.get(rng.nextInt(a.actions.size())).target())).stop)
//                        return;
//                }
//                super.sample(rng, each);
//            }
//        };

//        @Nullable EternalTable eteTable = ((BeliefTables) concept.goals()).tableFirst(EternalTable.class);

//        ((BeliefTables)concept.goals()).add(0, new EmptyBeliefTable() {
//            @Override
//            public void remember(Remember r) {
//                Task i = r.input;
//
//                if (Math.abs(i.freq() - freq) > 0.5f) {
//                    logger.info("goal contradicts reward:\n{}", i.proof());
//                    r.nar.proofPrint(i);
//                    Util.nop();
//                    r.forget(i);
//                }
//
//            }
//        });

        //else {
//                    if (!eteTable.isEmpty()) {
//                        Task eteGoal = eteTable.get(0);
//                        if (i.conf() < eteGoal.conf() && ArrayUtils.indexOf(i.stamp(), eteGoal.stamp()[0]) != -1) {
//                            logger.debug("goal subsumed by eternal goal:\n{}", i.proof());
//                            r.forget(i);
//                        }
//                    }
//                }

//        agent.//alwaysWant
//                alwaysWantEternally
//                    (concept.target, nar.confDefault(GOAL));
//        agent.alwaysQuestionDynamic(()->{
//            int dt =
//                    //0;
//                    nar.dur();
//            Random rng = nar.random();
//            return IMPL.the(agent.actions.get(rng).target().negIf(rng.nextBoolean()), dt, concept.target());
//        }, true);
    }


    @Override protected Signal newConcept() {
        ScalarSignal concept = new ScalarSignal(id, cause, () -> reward, /*linker, */nar()) {
            @Override
            protected AttnBranch newAttn(Term term) {
                return new AttnBranch(term, this.components());
            }
        };
        if (!concept.pri.equals(attn))
            nar().control.input(concept.pri, attn);
        return concept;
    }

    @Override
    public FloatRange resolution() {
        return concept.resolution();
    }

    @Override
    public Term term() {
        return concept.term();
    }

    @Override
    protected final float reward(Game a) {
        return rewardFunc.asFloat();
    }

}

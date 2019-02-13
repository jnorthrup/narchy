package nars.agent;

import jcog.math.FloatSupplier;
import nars.attention.AttBranch;
import nars.concept.sensor.Signal;
import nars.term.Term;

public class SimpleReward extends BeliefReward {

//    private static final Logger logger = LoggerFactory.getLogger(SimpleReward.class);
    private final FloatSupplier rewardFunc;


    public SimpleReward(Term id, FloatSupplier r, NAgent a) {
        super(id, a);

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
//        ((BeliefTables)concept.goals()).tables.addAt(0, new EmptyBeliefTable() {
//            @Override public void addAt(Remember r, NAR nar) {
//                Task i = r.input;
//
//                if (i.isNegative()) {
//                    logger.info("goal contradicts reward:\n{}", i.proof());
//                    r.forget(i);
//                } else {
//                    if (!eteTable.isEmpty()) {
//                        Task eteGoal = eteTable.get(0);
//                        if (i.conf() < eteGoal.conf() && ArrayUtils.indexOf(i.stamp(), eteGoal.stamp()[0]) != -1) {
//                            logger.debug("goal subsumed by eternal goal:\n{}", i.proof());
//                            r.forget(i);
//                        }
//                    }
//                }
//            }
//        });

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

    @Override
    protected float reward() {
        return rewardFunc.asFloat();
    }

    @Override protected Signal newConcept() {
        Signal concept = new Signal(id, in.id, () -> reward, /*linker, */nar()) {
            @Override
            protected AttBranch newAttn(Term term) {
                return new AttBranch(term, this.components());
            }
        };
        concept.attn.parent(attn);
        return concept;
    }



    @Override
    public Term term() {
        return concept.term();
    }


    @Override
    protected void updateReward(long prev, long now) {
        ((Signal)concept).sense(prev, now, nar());
    }
}

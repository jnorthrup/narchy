package nars.game;

import jcog.math.FloatSupplier;
import nars.Task;
import nars.control.op.Remember;
import nars.table.BeliefTables;
import nars.table.EmptyBeliefTable;
import nars.term.Term;

public class SimpleReward extends ScalarReward {

//    private static final Logger logger = LoggerFactory.getLogger(SimpleReward.class);
    private final FloatSupplier rewardFunc;

    public SimpleReward(Term id, float freq, FloatSupplier r, Game a) {
        this(id, freq, r, true, a);
    }

    public SimpleReward(Term id, float freq, FloatSupplier r, boolean stamped, Game a) {
        super(id, freq, stamped, a);

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

    public void addGuard(boolean log, boolean forget) {
        ((BeliefTables)concept.goals()).add(0, new EmptyBeliefTable() {
            @Override
            public void remember(Remember r) {
                Task i = r.input;

                if (Math.abs(i.freq() - goal.freq()) > 0.5f) {
                    if (log) {
                        //logger.info("goal contradicts reward:\n{}", i.proof());
                        System.out.print("goal contradicts reward\t"); r.nar.proofPrint(i);
                    }
                    if (forget) {
                        r.forget(i);
                    }
                }

            }
        });
    }

    @Override
    protected final float reward(Game a) {
        return rewardFunc.asFloat();
    }

}

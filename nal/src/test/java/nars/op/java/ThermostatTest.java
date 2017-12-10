package nars.op.java;

import jcog.Util;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Param;
import nars.term.Term;
import nars.time.Tense;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class ThermostatTest {

    public static class Thermostat {
        private int now, target;
        private final static int cold = 0, hot = 1;

        public int is() {
            return now;
        }

        public int should() {
            return target;
        }

        private void add(int delta) {
            now = Util.clamp(now + delta, cold, hot);
        }

        public void up() {
            add(+1);
        }

        public void down() {
            add(-1);
        }

        public void should(int x) {
            this.target = x;
        }

        public void is(int x) {
            this.now = x;
        }
    }

    /** labelled training episode */
    public static class Trick<X> {

        final static org.slf4j.Logger logger = LoggerFactory.getLogger(Trick.class);

        final String id;

        /** setup preconditions */
        final Consumer<X> pre;

        /** activity */
        final Consumer<X> action;

        /** validation */
        final Predicate<X> post;

        public Trick(String name, Consumer<X> pre, Consumer<X> action, Predicate<X> post) {
            this.id = name;
            this.pre = pre;
            this.action = action;
            this.post = post;
        }

        public boolean valid(X x) {
             return post.test(x);
        }

        public void train(X x, NAR n) {

            logger.info("training: {}", id);

            n.clear();
            Term LEARN = $.func("learn", $.the(id));
            n.believe(LEARN, Tense.Present); //label the learning episode which begins now
            pre.accept(x);

            n.run(1000); //perceive the preconditions

            Term DO = $.func("do", $.the(id));
            n.believe(DO, Tense.Present); //label the activity that will happen next

            action.accept(x); //execute the task

            n.run(1000); //consider the execution

            n.believe(DO.neg(), Tense.Present); //done doing
            n.believe(LEARN.neg(), Tense.Present); //done learning

        }
    }

    public static class ThermostatTester {

        final static org.slf4j.Logger logger = LoggerFactory.getLogger(ThermostatTester.class);
        protected final Thermostat x;
        protected final NAR n;

        public ThermostatTester() {

            Param.DEBUG = true;

            n = NARS.tmp();

            OObjects objs = new OObjects(n);

            this.x =
                    objs.a("x", Thermostat.class);
            //objs.the("x", new MyMutableInteger());
        }

        public Trick teach(String taskName,
                          Consumer<Thermostat> pre,
                          Consumer<Thermostat> task,
                          Predicate<Thermostat> post /* validation*/) {

            Trick<Thermostat> t = new Trick<>(taskName, pre, task, post);
            t.train(x, n);

            boolean valid = t.valid(x);
            if (!valid)
                throw new RuntimeException("invalid after training. please dont confuse NARS");

            n.run(1000); //debriefing

            return t;
        }
    }

    @Test
    public void test1() {
        ThermostatTester env = new ThermostatTester();
        env.n.log();

        Consumer<Thermostat>
            hotToCold =  x -> { x.is(x.hot);  x.should(x.cold); },
            coldToCold = x -> { x.is(x.cold); x.should(x.cold); };

        for (Consumer<Thermostat> condition : new Consumer[] { hotToCold, coldToCold })
            env.teach("cold", condition, x -> {
                while (x.is() > x.cold) x.down();
            }, x -> x.is() == x.cold);


        Consumer<Thermostat>
            coldToHot =  x -> { x.is(x.cold);  x.should(x.hot); },
            hotToHot = x -> { x.is(x.hot); x.should(x.hot); };
        for (Consumer<Thermostat> condition : new Consumer[] { coldToHot, hotToHot })
            env.teach("hot", condition, x -> {
                while (x.is() < x.hot) x.up();
            }, x -> x.is() == x.hot);

    }

//    @Test
//    public void test1() {
//        new ThermostatTester() {
//
//            {
//                int period = 500;
//                int subPeriods = 6;
//                int subPeriod = period / subPeriods;
//
//                n.log();
//                n.priDefault(BELIEF, 0.2f);
//                n.priDefault(QUESTION, 0.1f);
//                n.priDefault(QUEST, 0.1f);
//                n.freqResolution.set(0.02f);
//                n.termVolumeMax.set(28);
//                n.time.dur(subPeriod / 2);
//                //MetaGoal.Desire.want(n.want, 1.5f);
//
//                for (int i = 0; i < 2; i++) {
//                    x.set(3);
//                    n.run(subPeriod);
//
//                    x.intValue();
//                    n.run(subPeriod);
//
//                    x.set(4);
//                    n.run(subPeriod);
//
//                    x.intValue();
//                    n.run(subPeriod);
//                }
//
//                assertEquals(4, x.intValue());
//
//                n.run(1);
//
//                n.onTask(x -> {
//                    if (x.isGoal() && !x.isInput())
//                        System.out.println(x.proof());
//                });
//
//
//                while (x.intValue() != 3 && n.time() < period) {
//                    if (n.time() % (period / subPeriods) == 0) {
//                        try {
//                            n.input("$1.0 x(intValue, (), 3)! :|: %1.00;0.90%");
//                        } catch (Narsese.NarseseException e) {
//                            e.printStackTrace();
//                        }
//                        //n.input("$1.0 x(intValue, (), 4)! :|: %0.00;0.90%");
//                        //n.input("$1.0 (set:?1 <-> intValue:?2)?");
//                        //n.input("$1.0 x(set, 1)@ :|:");
//                    }
//                    n.run(1);
//                }
//
//                assertEquals(3, x.intValue());
//
//                while (x.intValue() != 5 && n.time() < period * 2) {
//                    if (n.time() % (period / subPeriods) == 0) {
//                        try {
//                            n.input("$1.0 x(intValue, (), 5)! :|: %1.00;0.90%");
////                            n.input("$0.5 x(intValue, (), 3)! :|: %0.00;0.90%");
////                            n.input("$0.5 x(intValue, (), 4)! :|: %0.00;0.90%");
//                            //n.input("$1.0 (set:?1 <-> intValue:?2)?");
//                            //n.input("$1.0 x(set, 1)@ :|:");
//                        } catch (Narsese.NarseseException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    n.run(1);
//                }
//                assertEquals(5, x.intValue());
//
//                new MetaGoal.Report().add(n.causes).print(System.out);
//
//            }
//        };
//    }

}

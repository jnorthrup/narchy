package nars.op.java;

import jcog.Util;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Task;
import nars.control.DurService;
import nars.control.MetaGoal;
import nars.op.stm.ConjClustering;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.Op.BELIEF;

public class ThermostatTest {

    public static class Thermostat {
        private int current, target;

        /**
         * limits
         */
        private final static int cold = 0, hot = 3;

        public int is() {
            return current;
        }

        public int should() {
            return target;
        }

        private void add(int delta) {
            current = Util.clamp(current + delta, cold, hot);
        }

        public void up() {
            System.err.println("t++");
            add(+1);
        }

        public void down() {
            System.err.println("t--");
            add(-1);
        }

        public String report() {
            String msg;
            if (is() < should())
                msg = "too cold";
            else if (is() > should())
                msg = "too hot";
            else
                msg = "temperature ok";
            System.err.println(msg);
            return msg;
        }

        private void should(int x) {
            System.err.println("temperature should " + x);
            this.target = x;
        }

        private void is(int x) {
            System.err.println("temperature is " + x);
            this.current = x;
        }

        static Consumer<Thermostat> change(boolean isHot, boolean shouldHot) {
            return x -> {
                x.is(isHot ? hot : cold);
                x.should(shouldHot ? hot : cold);
            };
        }
    }


    @Test
    @Disabled
    public void test1() {
        //Param.DEBUG = true;
        final int DUR = 2;

        final int subTrainings = 1;
        final int thinkDurs = 2;

        NAR n = NARS.tmp();

        n.time.dur(DUR);
        n.termVolumeMax.set(32);
        n.freqResolution.set(0.02f);
        n.confResolution.set(0.02f);

        n.want(MetaGoal.Desire, 0.2f);
        n.want(MetaGoal.Believe, 0.1f);
        n.want(MetaGoal.Perceive, -0.01f);

        float exeThresh = 0.51f;

        new ConjClustering(n, BELIEF, (t) -> true, 4, 16);

        //n.priDefault(BELIEF, 0.3f);

        //n.logPriMin(System.out, 0.5f);
        //n.logWhen(System.out, false, true, true);
        //n.log();

        boolean[] training = new boolean[]{true};

        Opjects op = new Opjects(n) {

//            {
//                pretend = true;
//            }

            @Override
            @Nullable
            protected synchronized Object invoked(Object obj, Method wrapped, Object[] args, Object result) {

                n.time.synch(n);
                //n.runLater(nn -> nn.run(DUR)); //queue some thinking cycles

                Object y = super.invoked(obj, wrapped, args, result);


                if (training[0])
                    n.run(DUR * thinkDurs);

                return y;
            }

            //            @Override
//            protected synchronized Object invoked(Instance in, Object obj, Method wrapped, Object[] args, Object result) {
//
//                //n.time.synch(n);
//
//
//                //long now = System.nanoTime();
//
//                Object r = super.invoked(in, obj, wrapped, args, result);
//
//                //n.runLater(() -> {
//                    //n.run(DUR * 2);
//                //});
//
//                return r;
//
//            }

        };

        Teacher<Thermostat> env = new Teacher<Thermostat>(op, Thermostat.class);



        Consumer<Thermostat>
                hotToCold = Thermostat.change(true, false),
                coldToCold = Thermostat.change(false, false),
                coldToHot = Thermostat.change(false, true),
                hotToHot = Thermostat.change(true, true);
        Predicate<Thermostat> isCold = x -> x.is() == Thermostat.cold;
        Predicate<Thermostat> isHot = x -> x.is() == Thermostat.hot;
        n.logWhen(System.out, false, true, true);

        boolean stupid = true;
        training:
        do {

            training[0] = true;

            op.executionThreshold.set(1f);
            for (int i = 0; i < subTrainings; i++) {
                for (Consumer<Thermostat> condition : new Consumer[]{hotToCold, coldToCold}) {
                    env.teach("cold", condition, x -> {
                        x.up(); //demonstrate no change
                        x.report();
                        while (x.is() > Thermostat.cold) x.down();
                        x.report();
                        x.down(); //demonstrate no change
                        x.report();
                    }, isCold);
                }

                for (Consumer<Thermostat> condition : new Consumer[]{coldToHot, hotToHot}) {
                    env.teach("hot", condition, x -> {
                        x.down(); //demonstrate no change
                        x.report();
                        while (!isHot.test(x)) x.up();
                        x.report();
                        x.up(); //demonstrate no change
                        x.report();
                    }, isHot);
                }

                n.run(thinkDurs * n.dur());
            }


            System.out.println("VALIDATING");
            System.out.println();
            training[0] = false;
            op.executionThreshold.set(exeThresh);



//        n.log();
            //n.run(100);

//        new Implier(n, new float[] { 1f },
//                $.$("a_Thermostat(down,())"),
//                $.$("a_Thermostat(up,())")
//                //$.$("a_Thermostat(is,(),#x)")
//        );

//        try {

            //make cold
//            n.input(new NALTask($.$("a_Thermostat(should,(),0)"),
//                    BELIEF, $.t(1f, 0.99f),
//                    n.time(), n.time(), n.time()+1000,
//                    n.time.nextInputStamp()).pri(1f));

            Thermostat t = env.x;



            {

                //n.clear();

                t.is(3);
                t.should(0);
                n.run(thinkDurs*n.dur());

                Term cold = $.$safe("a_Thermostat(is,(),0)");
                //Term cold = $.$safe("(a_Thermostat(is,(),0) &| --a_Thermostat(is,(),3))");
                Term hot = $.$safe("a_Thermostat(is,(),3)");
                Truth goalTruth = $.t(1f, 0.9f);

                final int[] maxTries = {8};
                DurService xPos = n.wantWhile(cold, goalTruth, new TaskConceptLogger(n, (w) ->
                        (--maxTries[0] >= 0) && (t.is() != t.should())
                ));
                DurService xNeg = n.wantWhile(hot, goalTruth.neg(), new TaskConceptLogger(n, (w) ->
                        t.is() != t.should()
                ));

                n.run(1);

                while (xPos.isOn()) {
                    int period = 1000;
                    //t.report();
                    n.run(period);
                }

                xPos.off();
                xNeg.off();

                t.report();

                if (t.is() == t.should()) {
                    System.out.println("good job nars!");
                    n.believe($.the("good"), Tense.Present);
                    stupid = false;
                } else {
                    System.out.println("bad job nars! try again");
                    n.believe($.the("bad"), Tense.Present);
                }
                n.run(thinkDurs * n.dur());


//            n.input(new NALTask($.$safe("a_Thermostat(is,(),0)"),
//                    GOAL, $.t(1f, 0.95f),
//                    n.time(), n.time(), n.time() + periods,
//                    n.time.nextInputStamp()).pri(1f));
//            n.input(new NALTask($.$safe("a_Thermostat(is,(),3)"),
//                    GOAL, $.t(0f, 0.95f),
//                    n.time(), n.time(), n.time() + periods,
//                    n.time.nextInputStamp()).pri(1f));

            }
        } while (stupid);
        {
//            n.input(new NALTask($.$safe("a_Thermostat(is,(),3)"),
//                    GOAL, $.t(0f, 0.99f),
//                    n.time(), n.time(), n.time()+1000,
//                    n.time.nextInputStamp()).pri(1f));
        }

//        while (t.is() != t.should()) {
//            int period = 1000;
//            t.report();
//            n.run(period);
//        }

        n.tasks().forEach(t -> {
            if (!t.isInput())
                System.out.println(t);
        });

    }

    private class TaskConceptLogger implements Predicate<Task> {
        private final Predicate<Task> pred;
        private final NAR nar;

        public TaskConceptLogger(NAR n, Predicate<Task> o) {
            this.pred = o;
            this.nar = n;
        }

        @Override
        public boolean test(Task task) {
            boolean result = pred.test(task);
            System.out.print(nar.time() + "\t");
            task.concept(nar, false).printSummary(System.out, nar);
            return result;
        }
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

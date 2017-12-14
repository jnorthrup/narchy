package nars.op.java;

import jcog.Util;
import nars.*;
import nars.op.Implier;
import nars.op.stm.ConjClustering;
import nars.task.NALTask;
import nars.task.NativeTask;
import nars.time.Tense;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

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
            this.target = x;
        }

        private void is(int x) {
            this.current = x;
        }

        static Consumer<Thermostat> change(boolean isHot, boolean shouldHot) {
            return x -> {
                x.is(isHot ? x.hot : x.cold);
                x.should(shouldHot? x.hot : x.cold);
            };
        }
    }


    @Test
    public void test1() throws Narsese.NarseseException {
        Param.DEBUG = true;
        final int DUR = 2;

        NAR n = NARS.tmp();
        n.time.dur(DUR);
        n.termVolumeMax.set(22);
        n.freqResolution.set(0.1f);
        n.confResolution.set(0.01f);

        new ConjClustering(n, BELIEF, (t)->true, 8, 64);

        n.priDefault(BELIEF, 0.2f);

        //n.logPriMin(System.out, 0.5f);
        n.logWhen(System.out, false, true, true);

        Teacher<Thermostat> env = new Teacher<Thermostat>(new Opjects(n) {


            {
                //goalMimic = true;
            }

            @Override
            protected synchronized Object invoked(Instance in, Object obj, Method wrapped, Object[] args, Object result) {

                //n.time.synch(n);


                //long now = System.nanoTime();

                Object r = super.invoked(in, obj, wrapped, args, result);

                //n.runLater(() -> {
                    n.run(DUR * 2);
                //});

                return r;

            }

            @Override
            protected boolean evoked(Task task, Object[] args, Object inst) {
                System.err.println("evoke: " + task.proof());
                return super.evoked(task, args, inst);
            }

        }, Thermostat.class);


        Consumer<Thermostat>
                hotToCold = Thermostat.change(true, false),
                coldToCold = Thermostat.change(false, false),
                coldToHot = Thermostat.change(false, true),
                hotToHot = Thermostat.change(true, true);
        Predicate<Thermostat> isCold = x -> x.is() == x.cold;
        Predicate<Thermostat> isHot = x -> x.is() == x.hot;

        for (Consumer<Thermostat> condition : new Consumer[]{hotToCold, coldToCold}) {
            env.teach("cold", condition, x -> {
                x.up(); //demonstrate no change
                x.report();
                while (x.is() > x.cold) x.down();
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



        n.clear();

        System.out.println("VALIDATING");
        System.out.println();


//        n.log();
        //n.run(100);
//        n.logWhen(System.out, false, true, true);

//        new Implier(n, new float[] { 1f },
//                $.$("a_Thermostat(down,())"),
//                $.$("a_Thermostat(up,())")
//                //$.$("a_Thermostat(is,(),#x)")
//        );

        try {

             //make cold
//            n.input(new NALTask($.$("a_Thermostat(should,(),0)"),
//                    BELIEF, $.t(1f, 0.99f),
//                    n.time(), n.time(), n.time()+1000,
//                    n.time.nextInputStamp()).pri(1f));

            env.x.should(0);
            env.x.is();

            n.input(new NALTask($.$("a_Thermostat(is,(),0)"),
                    GOAL, $.t(1f, 0.99f),
                    n.time(), n.time(), n.time()+1000,
                    n.time.nextInputStamp()).pri(1f));
            n.input(new NALTask($.$("a_Thermostat(is,(),3)"),
                    GOAL, $.t(0f, 0.99f),
                    n.time(), n.time(), n.time()+1000,
                    n.time.nextInputStamp()).pri(1f));

        } catch (Narsese.NarseseException e) {
            e.printStackTrace();
        }

        while (env.x.is()!=0)
            n.run(100);

        env.x.report();
        System.out.println("good job nars!");
        n.believe($.the("good"), Tense.Present);

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

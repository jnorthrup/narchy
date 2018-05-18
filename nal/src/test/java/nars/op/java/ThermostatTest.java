package nars.op.java;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Task;
import nars.control.DurService;
import nars.op.stm.ConjClustering;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.Op.BELIEF;

public class ThermostatTest {


//    final Runnable pause = () -> {
//        //Util.sleep(500);
//    };
//    @Test
//    @Disabled
    public static void main (String[] args) {// void test1() {
        //
        final int DUR = 3;

        final int subTrainings = 2;
        final int thinkDurs = 4; //pause between episodes

        NAR n = NARS.tmp();

        n.time.dur(DUR);
        n.dtDither.set(0f);
        n.timeFocus.set(2);
        n.termVolumeMax.set(18);
        n.freqResolution.set(0.1f);
        n.confResolution.set(0.01f);
        n.activateConceptRate.set(0.1f);

        n.goalPriDefault.set(0.5f);
//        n.forgetRate.set(2f);
        //n.deep.set(0.8);


     //   n.emotion.want(MetaGoal.Desire, 0.2f);
//        n.want(MetaGoal.Believe, 0.1f);
//        n.want(MetaGoal.Perceive, -0.01f);

        float exeThresh = 0.51f;

        //new ArithmeticIntroduction(8, n);
        new ConjClustering(n, BELIEF, (t) -> true, 8, 32);

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

                if (training[0]) {
                    n.synch();
                    //n.runLater(nn -> nn.run(DUR)); //queue some thinking cycles
                }

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

        Teacher<Thermostat> env = new Teacher<>(op,
                new Thermostat());
                //Thermostat.class);



        Consumer<Thermostat>
                hotToCold = Thermostat.change(true, false),
                coldToCold = Thermostat.change(false, false),
                coldToHot = Thermostat.change(false, true),
                hotToHot = Thermostat.change(true, true);
        Predicate<Thermostat> isCold = x -> x.is() == Thermostat.cold;
        Predicate<Thermostat> isHot = x -> x.is() == Thermostat.hot;
        n.logWhen(System.out, true, true, true);

        boolean stupid = true;
        training:
        do {

            training[0] = true;

            op.exeThresh.set(1f);
            for (int i = 0; i < subTrainings; i++) {
                for (Consumer<Thermostat> condition : new Consumer[]{hotToCold, coldToCold}) {

                    System.out.println("EPISODE START");
                    n.clear();

                    env.teach("down", condition, (Thermostat x) -> {
//                        x.up(); //demonstrate no change
//                        x.report();

                        n.run(1);
                        while (x.is() > Thermostat.cold) {
                            x.down();
                            n.run(1);
                        }
                        x.report();
                        n.run(1);

//                        x.down(); //demonstrate no change
//                        x.report();
                    }, isCold);
                    System.out.println("EPISODE END");
                    n.run(thinkDurs * n.dur());

//                    n.concept("do(down)").print();
                }

                for (Consumer<Thermostat> condition : new Consumer[]{coldToHot, hotToHot}) {

                    System.out.println("EPISODE START");
                    n.clear();

                    env.teach("up", condition, x -> {
//                        x.down(); //demonstrate no change
//                        x.report();
                        n.run(1);
                        while (!isHot.test(x)) {
                            x.up();
                            n.run(1);
                        }
                        x.report();
                        n.run(1);
//                        x.up(); //demonstrate no change
//                        x.report();
                    }, isHot);

                    System.out.println("EPISODE END");
                    n.run(thinkDurs * n.dur());
                }

            }


            System.out.println("VALIDATING");
            System.out.println();
            training[0] = false;
            op.exeThresh.set(exeThresh);



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
                n.run(thinkDurs * n.dur());

                Term cold = $.$$("is(a_Thermostat,0)");
                //Term cold = $.$safe("(a_Thermostat(is,(),0) &| --a_Thermostat(is,(),3))");
                Term hot = $.$$("is(a_Thermostat,3)");
                Truth goalTruth = $.t(1f, 0.9f);

                DurService xPos = n.wantWhile(cold, goalTruth, new TaskConceptLogger(n, (w) ->
                        /*(--maxTries[0] >= 0) && */(t.current != t.target)
                ));
                DurService xNeg = n.wantWhile(hot, goalTruth.neg(), new TaskConceptLogger(n, (w) ->
                        t.current != t.target
                ));

                n.run(1);

                for (int i = 0; i < 16 && xPos.isOn(); i++) {
                    int period = 100;
                    //t.report();
                    //n.run(period, pause);
                    n.run(period);
                }

                xPos.off();
                xNeg.off();

                t.report();

                if (t.is() == t.should()) {
                    System.out.println("good job nars!");
                    n.believe($.$$("(learn(up) && learn(down))"), Tense.Present);
                    stupid = false;
                } else {
                    System.out.println("bad job nars! try again");
                    n.believe($.$$("(--learn(up) && --learn(down))"), Tense.Present);
                }


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


        //n.run(thinkDurs * n.dur());

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

    public static class TaskConceptLogger implements Predicate<Task> {
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

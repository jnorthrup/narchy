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


    public static void main(String[] args) {

        final int DUR = 3;

        final int subTrainings = 1;
        final int thinkDurs = 1;

        NAR n = NARS.tmp();

        //new ArithmeticIntroduction(4, n);

        n.time.dur(DUR);
        n.dtDither.set(1);
        n.timeFocus.set(4);

        n.termVolumeMax.set(22);
        n.freqResolution.set(0.05f);
        n.confResolution.set(0.02f);
        //n.activateConceptRate.set(0.1f);

        n.goalPriDefault.set(0.9f);
        //n.emotion.want(MetaGoal.Believe, -0.1f);

        float exeThresh = 0.51f;


        new ConjClustering(n, BELIEF, (t) -> true, 2, 8);


        boolean[] training = new boolean[]{true};

        Opjects op = new Opjects(n) {


            @Override
            @Nullable
            protected synchronized Object invoked(Object obj, Method wrapped, Object[] args, Object result) {

                if (training[0]) {
                    n.synch();

                }

                Object y = super.invoked(obj, wrapped, args, result);


                if (training[0])
                    n.run(DUR * thinkDurs);

                return y;
            }


        };

        Teacher<Thermostat> env = new Teacher<>(op,
                new Thermostat());


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

            //op.exeThresh.set(1f);
            for (int i = 0; i < subTrainings; i++) {
                for (Consumer<Thermostat> condition : new Consumer[]{hotToCold, coldToCold}) {

                    System.out.println("EPISODE START");
                    n.clear();

                    env.teach("down", condition, (Thermostat x) -> {


                        n.run(1);
                        while (x.is() > Thermostat.cold) {
                            x.down();
                            n.run(1);
                        }
                        x.report();
                        n.run(1);


                    }, isCold);
                    System.out.println("EPISODE END");
                    n.run(thinkDurs * n.dur());


                }

                for (Consumer<Thermostat> condition : new Consumer[]{coldToHot, hotToHot}) {

                    System.out.println("EPISODE START");
                    n.clear();

                    env.teach("up", condition, x -> {


                        n.run(1);
                        while (!isHot.test(x)) {
                            x.up();
                            n.run(1);
                        }
                        x.report();
                        n.run(1);


                    }, isHot);

                    System.out.println("EPISODE END");
                    n.run(thinkDurs * n.dur());
                }

            }


            System.out.println("VALIDATING");
            System.out.println();
            training[0] = false;
            op.exeThresh.set(exeThresh);


            Thermostat t = env.x;


            {


                t.is(3);
                t.should(0);
                n.run(thinkDurs * n.dur());

                Term cold = $.$$("is(a_Thermostat,0)");

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


            }
        } while (stupid);


        {


        }


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


}

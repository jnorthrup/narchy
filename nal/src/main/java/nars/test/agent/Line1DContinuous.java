package nars.test.agent;

import nars.*;
import nars.concept.ActionConcept;
import nars.concept.SensorConcept;
import nars.term.atom.Atomic;

import java.util.Arrays;

import static java.lang.System.out;


/**
 * Created by me on 5/4/16.
 */
public class Line1DContinuous extends NAgent {

    static {
        Param.DEBUG = false;
    }

    public interface IntToFloatFunction {
        float valueOf(int i);
    }

    private final IntToFloatFunction targetFunc;
    int size;
    public boolean print;
    private float yHidden;
    private float yEst;
    float speed = 5f;
    final float[] ins;

    public Line1DContinuous(NAR n, int size, IntToFloatFunction target) {
        super("x", n);
        this.size = size;
        ins = new float[size*2];
        this.targetFunc = target;

        yEst = size/2f; //NAR estimate of Y
        yHidden = size/2f; //actual best Y used by loss function


        for (int i = 0; i < size; i++) {
            int ii = i;
            //hidden
            addSensor(new SensorConcept(
                    $.func("h", Atomic.the("x"), $.the( i)),
                    //$.p($.the("h"), $.the(i)),
                    n, ()-> ins[ii]));

            //estimated
            addSensor(new SensorConcept(
                    $.func("e", Atomic.the("x"), $.the( i)),
                    //$.func("e", $.the(i)),
                    //$.p($.the("e"), $.the(i)),
                    n, ()-> ins[size + ii]));
        }

        ActionConcept a;

        actionBipolar($.inh(Atomic.the("move"), Atomic.the("x")), (v) -> {

            yEst += (v)*speed;

            return yEst;
        });

        trace = false;

    }

    @Override
    protected float act() {


        yHidden = Math.round(targetFunc.valueOf((int) now) * (size-1));

        yHidden = Math.min(size-1, Math.max(0, yHidden));
        yEst    = Math.min(size-1, Math.max(0, yEst));



        //update perceived state:
        //1*size
        //        Arrays.fill(ins, 0.5f);
        //        ins[yHidden] += 0.5f;
        //        ins[yEst] -= 0.5f;
        //2*size
        Arrays.fill(ins, 0f);
        float smoothing = 1/2f;
        for (int i = 0; i < size; i++) {
            ins[i] = Math.abs(yHidden - i)/(size*smoothing);
            ins[i + this.size] = Math.abs(yEst - i)/(size*smoothing);
        }
//        ins[Math.round(yHidden)] = 1f;
//        ins[Math.round(this.size + yEst)] = 1f;


        float dist =  Math.abs(yHidden - yEst) / this.size;

        //float closeness = 1f - dist;
        //float reward = ((closeness*closeness*closeness) -0.5f)*2f;
        //float reward = dist < speed ? (0.5f/(1f+dist)) : -dist;
        float reward =
                -dist * 2f + 1f;
                //(1f-dist)*(1f-dist);
                //1f / (1+dist*dist);



//        float de;
//        switch (aa) {
//            case 1: //right
//                de = 1f*speed;
//                break;
//            case 0: //left
//                de = -1f*speed;
//                break;
////                case 3:
////                    de = 1f * speed/4f;
////                    break;
////                case 4:
////                    de = -1f * speed/4f;
////                    break;
//            case 2:
//            default:
//                de = 0f; //nothing
//                break;
//        }




        if (yEst > this.size -1) yEst = this.size -1;
        if (yEst < 0) yEst = 0;


        if (print) {


            int colActual = Math.round(yHidden);
            int colEst = Math.round(yEst);
            for (int i = 0; i < this.size; i++) {

                char c;
                if (i == colActual && i == colEst) {
                    c = '@';
                }else if (i == colActual)
                    c = 'X';
                else if (i == colEst)
                    c = '+';
                else
                    c = '.';

                out.print(c);
            }

            //out.print(Texts.n2(ins));

            //out.print(' ');
            //out.print(reward);
            out.print(' ');
            out.print(summary());
            out.println();
        }

        return reward;
    }


    public static IntToFloatFunction sine(float targetPeriod) {
        return (t) -> 0.5f + 0.5f * (float) Math.sin(t / (targetPeriod));
        //+ 0.05f * (a * (float)Math.cos(t / (targetPeriod/3f))-1)
        //return 0.5f + 0.5f * (float)Math.tan(t / (targetPeriod)) + (float)Math.random()*0.1f;
    }
    public static IntToFloatFunction random(float targetPeriod) {
        return (t) -> (((((int)(t/targetPeriod)) * 31) ^ 37) % 256)/256.0f;

        //+ 0.05f * (a * (float)Math.cos(t / (targetPeriod/3f))-1)
        //return 0.5f + 0.5f * (float)Math.tan(t / (targetPeriod)) + (float)Math.random()*0.1f;
    }

    public static void main(String[] args) {

        NAR nar = new NARS().get();
        nar.termVolumeMax.set(32);


        nar.beliefConfidence(0.9f);
        nar.goalConfidence(0.9f);


        Line1DContinuous l = new Line1DContinuous(nar, 6,
                random(16)
        );

        l.print = true;
        //l.runRT(25, 15000).join();
//        l.runCycles(2000);



    }


//    static class Evolve {
//
//        static double eval(NAR nar) {
//
//            Line1DContinuous l = new Line1DContinuous(nar, 3,
//                    //sine(50)
//                    random(8)
//            );
//
//            l.print = false;
//            l.run(200);
//
//            l.stop();
//
//
//            float score = l.rewardSum() / nar.time();
//            //System.out.println("AVG SCORE=" + score);
//            return score;
//        }
//
//        static double eval(Node f) {
//
//            XorShift128PlusRandom rng = new XorShift128PlusRandom((int)(Math.random()*1000));
//
//            final Executioner exe =
//                    new SynchronousExecutor();
//
//            Param.ANSWER_REPORTING = false;
//
//            Default nar = new Default(1024,
//                    8, 1, 3, rng,
//                    new CaffeineIndex(new DefaultConceptBuilder(), 1024*4, false, exe),
//                    new FrameTime(1f), exe
//            );
//            nar.termVolumeMax.set(22);
//
//            int cpf = 8;
//
//            nar.onCycle(()-> {
//                Assignments x = new Assignments(
//                        nar.emotion.busyPri.getSum(), (double)nar.emotion.learning(),
//                        (double)nar.emotion.happy(), (double)nar.emotion.sad()
//                );
//                Arguments y = f.eval(x);
//
//                double y0 = y.arg(0).eval(x);
//                double y1 = y.arg(1).eval(x);
//
//                int tpf = Math.round( Util.clamp((float)y0, 0f, 128));
//                int termLinks = Util.clampI((float)y1, 0, 4);
//                int taskLinks = 1;
//                //System.out.println(y + " " + tpf + " " + termLinks);
//
//                nar.core.conceptsFiredPerCycle.setValue(cpf);
//                nar.core.tasklinksFiredPerFiredConcept.set(taskLinks);
//                nar.core.termlinksFiredPerFiredConcept.set(1, termLinks);
//                nar.core.derivationsInputPerCycle.setValue(tpf);
//            });
//
//
//
//            nar.beliefConfidence(0.9f);
//            nar.goalConfidence(0.9f);
//
//            double score = eval(nar);
//            System.out.println(/*Thread.currentThread() + " " */ f + " = " + score);
//            return -score; //minimizes
//        }
//
//        public static void main(String[] args) {
//
//            new Evolution().returning(Type.arrayType(Type.doubleType()))// Type.doubleType())
//                    .setRandom(new XorShift128PlusRandom(1))
//                    .setConstants(Utils.createDoubleConstants(1, 3))
//                    .setVariables(Type.doubleType(), Type.doubleType(), Type.doubleType(), Type.doubleType())
//                    .setFunctions(
//                            DoubleUtils.the.add,
//                            DoubleUtils.the.subtract,
//                            DoubleUtils.the.multiply,
//                            DoubleUtils.the.divide,
//                            new PairDouble()
//                    )
//                    .setFitness(Evolve::eval, true) // the fitness function will compare candidates against a data set which maps inputs to their expected outputs
//                    .setInitialPopulationSize(16).setTreeDepth(6)
//                    .setTargetFitness(-1.0)
//                    .setMaxGenerations(64)
//                    .get();
//
//
//
//
//        }
//
//    }
}

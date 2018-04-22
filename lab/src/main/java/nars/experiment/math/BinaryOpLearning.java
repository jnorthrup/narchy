//package nars.experiment.math;
//
//import nars.*;
//import nars.concept.ActionConcept;
//import nars.concept.SensorConcept;
//import nars.gui.Vis;
//import nars.index.CaffeineIndex;
//import nars.nar.Default;
//import nars.nar.exe.Executioner;
//import nars.nar.exe.SingleThreadExecutioner;
//import nars.nar.util.DefaultConceptBuilder;
//import nars.op.time.MySTMClustered;
//import nars.term.Compound;
//import nars.term.Term;
//import nars.term.obj.IntTerm;
//import nars.time.FrameClock;
//import nars.truth.Truth;
//import nars.util.Util;
//import nars.util.data.random.XorShift128PlusRandom;
//import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.Random;
//import java.util.function.BiFunction;
//
//import static java.lang.Integer.toBinaryString;
//import static nars.NAR.printTasks;
//import static nars.experiment.tetris.Tetris.DEFAULT_INDEX_WEIGHT;
//import static nars.util.Texts.n2;
//import static nars.util.Util.argmax;
//
///**
// * Created by me on 8/21/16.
// */
//public class BinaryOpLearning extends NAgent {
//
//    private CharSensor a, b;
//    private CharMotor c;
//    private char[] expect;
//
//    public BinaryOpLearning(NAR nar) {
//        super(nar);
//
//        a = new CharSensor($.the("a"), this, W, vocab);
//        b = new CharSensor($.the("b"), this, W, vocab);
//
//        c = new CharMotor($.the("c"), this, W, vocab);
//
//    }
//
//    public static class CharSensor {
//
//        private final Term id;
//
//        char[] data = {};
//
//        public CharSensor(Term id, NAgent env, int size, char[] vocab) {
//            this.id = id;
//
//            data = new char[size];
//
//            for (int i = 0; i < vocab.length; i++) {
//                for (int j = 0; j < size; j++) {
//
//                    int jj = j, ii = i;
//
//                    char c = vocab[ii];
//                    Compound t = charTerm(id, jj, c);
//                    env.sensors.add(new SensorConcept(t, env.nar, ()->
//                        data[jj] == c ? 1f : 0f,
//                        (v) -> $.t(v, env.alpha)
//                    ));
//                }
//            }
//        }
//
////        public CharSensor input() {
////            Term t = $.inh(
////                $.p(data, c -> $.the(c)),
////                id
////            );
////            nar.believe(t,
////                    //Tense.Present,
////                    Tense.Present,
////                    1f);
////            return this;
////        }
//
//        @NotNull public char[] get() {
//            return data;
//        }
//
//        public CharSensor set(@NotNull char[] newData) {
//            //if (!Arrays.equals(data, newData)) {
//            System.arraycopy(newData, 0, data, 0, data.length);
//                //input();
//            //}
//            return this;
//        }
//
//        public CharSensor set(@NotNull String s) {
//            //TODO re-use buffer if the same sze
//            return set(s.toCharArray());
//        }
//
//        public String string() {
//            return new String(data);
//        }
//    }
//
//    public static class CharMotor {
//
//        final char[] buffer;
//
//        final ActionConcept[][] motor;
//        final float[][] desire;
//
//        private final char[] vocab;
//        private final NAR nar;
//
//        public CharMotor(Term id, NAgent env, int length, char[] vocab /* characters it can choose from */) {
//
//            this.nar = env.nar;
//
//            this.buffer = new char[length];
//            this.vocab = vocab;
//
//            this.motor = new ActionConcept[length][vocab.length];
//            this.desire = new float[length][vocab.length];
//
//
//
//            for (int i = 0; i < vocab.length; i++) {
//                for (int j = 0; j < length; j++) {
//
//                    int ll = j, cc = i;
//
//                    Compound t = charTerm(id, ll, vocab[cc]);
//                    ActionConcept m;
//                    env.actions.add(m = new ActionConcept(t, env.nar, (Truth b, Truth d)->{
//                        float e;
//                        if (d!=null) {
//                            e =
//                                //d.expectation();
//                                d.freq();
//                        } else {
//                            e = 0.5f;
//                        }
//
//                        //float s = desireTotal(jj); //this is approximate unless we make an async motor feedback mode that calculates after all other motors have updated
//                        //float p = s > 0 ? e / (s) : 1;
//
//                        desire[ll][cc] = e;
//
//                        //return d.confMultViaWeight(p); //share the feedback with the other motors for this position
//
//                        return d;
//                    }));
//                }
//            }
//        }
//
//        public float desireTotal(int j) {
//            return Util.sum(desire[j]);
//        }
//
//        public char[] get() {
//            update();
//            return buffer;
//        }
//
//        protected void update() {
//            for (int j = 0; j < buffer.length; j++) {
//                buffer[j] = decide(j);
//            }
//
//        }
//
//        //DecidingSoftmax decide = new DecidingSoftmax(0.6f, 0.4f, 0.999f);
//
//
//        private char decide(int j) {
//            return vocab[argmax(desire[j], nar.random)];
//            //return vocab[decide.decide(desire[j], -1, nar.random)];
//        }
//
//        public String string() {
//            return new String(get());
//        }
//
//    }
//
//    public
//    @Nullable
//    static Compound charTerm(Term id, int jj, char c) {
//        return $.inh($.p(new IntTerm(jj), $.the(c)), id);
//
////        return $.p($.p(id,
////                new Termject.IntTerm(jj)),
////                //$.the(jj)),
////                $.quote(String.valueOf(c)));
////
//        //return $.inh($.p($.the(jj + "_" + c)), id);
//
//    }
//
//    @Override
//    public String summary() {
//        return n2(rMean.getMean()) + " " + super.summary();
//    }
//
//    int period = 10;
//    int W = 4; //width of output buffer
//    int N = (int)Math.pow(2, W)-1;
//    int ax, bx;
//    char[] vocab = {'0', '1'};
//    char padChar = '0';
//    int timeSpacing;
//    //int rewardIntensity;
//    static IntIntToIntFunction f = (a,b) ->
//            //a + b;
//            a & b;
//
//
//    BiFunction<char[],char[],Float> distance = (x,y) -> BinaryOpLearning.difference(x,y); //ex: Levenshtein, etc
//
//    final DescriptiveStatistics rMean = new DescriptiveStatistics((period+1)*5); //reward mean
//
//
//
//
//    interface IntIntToIntFunction {
//        int valueOf(int x1, int x2);
//    }
//
//
//    @Override
//    protected float act() {
//
////
////        if (now > 1000 || desireConf() > 0.45f && rMean.getMean() > 0.75f) {
////            //lock into logical mode
////            epsilonProbability = 0;
////        }
//
//        if (expect == null || now % period == 0) {
//
//            //set for the remainder of the period
//            ax = nar.random.nextInt(N);
//            bx = nar.random.nextInt(N);
//            //rewardIntensity = 0;
//        } else {
//            //rewardIntensity++;
//        }
//
//        a.set(pad(toBinaryString(ax), W, padChar));
//        b.set(pad(toBinaryString(bx), W, padChar));
//        String s = pad(toBinaryString(f.valueOf(ax, bx)), W, padChar);
//        expect = s.toCharArray();
//
//
//        float d = distance.apply(expect, c.get());
//        float r0 = (1f - 2 * d);
//        float r =
//                //Util.lerp(
//                    r0;
//                    //(float)rMean.getMean(),
//                //0.5f);
//
//        if (now > 500 && rMean.getMean() < 0.5f) {
//            //it caused a mistake
////
////            System.out.println("\nWHY I DONT SEEM TO LEARN\n");
////
////            actions.forEach(a -> {
////               a.goals().forEach(ag -> {
////                   System.out.println(ag.proof());
////
////               });
////                System.out.println("\n---\n");
////            });
////
////            System.out.println("\n------\n");
//
//        }
//
//        rMean.addValue(r0);
//
//
//        /*
//        System.err.println("score=" + n2(rMean.getMean()) + " conf=" + n2(desireConf())
//                 + "\t" + a.string() + " " + b.string() + " --> " + new String(expect) + " ?? " +
//                c.string() );
//                */
//
//
//        return r;
//    }
//
//    /** also truncates if exceeds the size */
//    private String pad(String s, int size, char padChar) {
//        if (s.length() > size) {
//            s = s.substring(0, size);
//        }
//        while (s.length() < size) {
//            s = padChar + s;
//        }
//        return s;
//    }
//
//
//    /** x and y have to be same length */
//    static float difference(char[] x, char[] y) {
//        int sum = 0;
//        int n = x.length;
//        for (int i = 0; i < n; i++) {
//            if (x[i]!=y[i])
//                sum++;
//        }
//        return ((float)sum)/n;
//    }
//
//    public static void main(String[] args) {
//        Random rng = new XorShift128PlusRandom(1);
//
//        Param.DEBUG = false;
//
//        //Multi nar = new Multi(3,512,
//        Executioner exe = new SingleThreadExecutioner();
//        Default nar = new Default(1024,
//                16, 2, 3, rng,
//                new CaffeineIndex(new DefaultConceptBuilder(rng), DEFAULT_INDEX_WEIGHT, false, exe)
//
//                , new FrameClock(), exe
//
//        );
//
//
//        nar.beliefConfidence(0.9f);
//        nar.goalConfidence(0.6f);
//        nar.DEFAULT_BELIEF_PRIORITY = 0.5f;
//        nar.DEFAULT_GOAL_PRIORITY = 0.5f;
//        nar.DEFAULT_QUESTION_PRIORITY = 0.1f;
//        nar.DEFAULT_QUEST_PRIORITY = 0.1f;
//        nar.cyclesPerFrame.set(8);
//        nar.compoundVolumeMax.set(80);
//        //nar.confMin.setValue(0.05f);
//        //nar.truthResolution.setValue(0.02f);
//
//
//        MySTMClustered stm = new MySTMClustered(nar, 32, '.', 2);
//        MySTMClustered stmGoal = new MySTMClustered(nar, 32, '!', 2);
//
//        //new ArithmeticInduction(nar);
//        //new VariableCompressor(nar);
//
//        //new Abbreviation(nar,"aKa_");
//
//        BinaryOpLearning b = new BinaryOpLearning(nar);
//
//
//        //b.trace = false;
//
//        NARLoop bLoop = b.run(500, 0);
//
////        System.out.println(b.sensors);
////        System.out.println(b.actions);
//
//        Vis.newBeliefChartWindow(b, 100);
//        //BagChart.show((Default) nar, 1024);
//
//        bLoop.join();
//
//
//        printTasks(nar, true);
//        printTasks(nar, false);
//        nar.printConceptStatistics();
//    }
//
//
//}

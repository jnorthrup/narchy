package jcog.pri.mix.control;

import jcog.Util;
import jcog.exe.Loop;
import jcog.learn.classify.AbstractClassifier;
import jcog.learn.classify.BooleanClassifier;
import jcog.learn.classify.EnumClassifier;
import jcog.list.FasterList;
import jcog.math.AtomicSummaryStatistics;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import jcog.math.tensor.ArrayTensor;
import jcog.math.tensor.Tensor;
import jcog.math.tensor.TensorChain;
import jcog.pri.Prioritized;
import jcog.pri.Priority;
import jcog.pri.mix.PSink;
import jcog.pri.mix.PSinks;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static jcog.Texts.n4;
import static jcog.Util.sqr;

/**
 * intelligent stream filter
 * it can be trained with a reinforcement learning agent for any goal
 * it classifies each input in an array of configured classifications
 * each classification has a gain level
 * if it matches an input then then its gain will be applied in combination with all other matching gain values
 * in -1..+1 range
 * so they can override each other
 * then this value is shifted and squared to get the final multiplier amount
 * <p>
 * record a finite history of mix parameters values
 * in order to assign credit
 * to historic classifications or something
 * which resembles a differentiable
 * RNN backprop
 */
public class MixContRL<X extends Priority> extends Loop implements PSinks<X, CLink<X>> {

    public final MixChannel[] mix;
    public final FloatRange priMin = new FloatRange(Prioritized.EPSILON, 0f, 1f);

//    float dynamicRange = 4f;
//    public final FloatParam gainMin = new FloatParam(1/dynamicRange, 0f, 0f);
//    public final FloatParam gainMax = new FloatParam(dynamicRange, 0f, 16f);

    /** the tests to apply to input (doesnt include aux's which will already have applied their id)  */
    public  final AbstractClassifier<X>[] tests;
    private final ObjectIntPair<EnumClassifier<X>>[] dynTests;

    /** should probably be calibrated in relation to the executioner's processing rate */
    private final float activeTaskMomentum = 0.5f;

    public static class MixChannel {

        public final AtomicSummaryStatistics input = new AtomicSummaryStatistics();
        public final AtomicSummaryStatistics active = new AtomicSummaryStatistics();
        public String id;

        public MixChannel(String id) {
            this.id = id;
        }

        public void accept(float pri, boolean input, boolean active) {
            if (input)
                this.input.accept(pri);
            if (active)
                this.active.accept(pri);
        }
    }



    @Nullable
    public MixAgent agent;


    public final FloatSupplier score;

    /**
     * range 0..1.0, 0.5 is middle (1x)
     */
    public final ArrayTensor mixControl;

    public final ArrayTensor nextInput, nextActive, active, input;

    /**
     * unipolar vector, 0..1.0
     */
    public final Tensor agentIn;

    /**
     * dim >= size
     */
    public final int dim;

    final int maxAux;
    final List<PSink<X,CLink<X>>> aux = new FasterList();

    /**
     * values less than 1 eventually lowers channel volume levels to zero (flat, ie. x1)
     * unless otherwise boosted or cut
     */
    //float decay = 1f;

    //private final double[] delta; //delta accumulated
    //double controlSpeed = 0.05; //increments up/down per action
    public float lastScore;

    final int auxStart;


    public MixContRL(float fps, MixAgent agent, FloatSupplier score, int aux, AbstractClassifier<X>... tests) {
        super(fps);

        this.maxAux = aux;

        int dim = 0;
        for (AbstractClassifier t : tests)
            dim += t.dimension();
        this.auxStart = dim;

        AbstractClassifier[] aa = new AbstractClassifier[aux];
        for (int a = 0; a < aux; a++) {
            aa[a] = new BooleanClassifier("ax" + a, (x) -> false);
            dim++;
        }
        this.tests = tests;
        tests = ArrayUtils.addAll(tests, aa);


        List<ObjectIntPair<EnumClassifier>> dynTests = new FasterList();
        int i = 0;
        for (AbstractClassifier c : tests) {
            if (c instanceof EnumClassifier && ((EnumClassifier)c).isDynamic())
                dynTests.add(PrimitiveTuples.pair((EnumClassifier)c, i));
            i += c.dimension();
        }

        this.dynTests = dynTests.toArray(new ObjectIntPair[dynTests.size()]);

        assert(dim >= tests.length);
        this.dim = dim;



        int j = 0;
        this.mix = new MixChannel[dim];
        for (AbstractClassifier t : tests) {
            int n = t.dimension();
            for (int k = 0; k < n; k++) {
                mix[j++] = new MixChannel(t.name(k));
            }
        }
        for (; j < dim; ) {
            mix[j++] = new MixChannel("aux" + '_' +j);
        }


        /** level values. between 0 and 1: 0 = max cut, 1 = max boost, 0.5 = x1 */
        this.mixControl = new ArrayTensor(this.dim);

        this.nextInput = new ArrayTensor(this.dim);
        this.nextActive = new ArrayTensor(this.dim);
        this.mixControl.fill(0.5f);
        this.nextInput.fill(0f);
        this.nextActive.fill(0f);

        this.agentIn =
                //new AutoTensor(
                //RingBufferTensor.get(
                TensorChain.get(
                        this.input = nextInput,
                        this.active = nextActive,
                        //this.traffic = new TensorLERP(rawTraffic, 0.75f), //sum is normalized to 1
                        mixControl
                )
        //      , 2)
        //,12)
        ;


        this.agent = agent;

        //this.delta = new double[outs];
        this.score = score;
    }

    public CLink<X> test(X x) {
        return test(new CLink<>(x));
    }

    public CLink<X> test(CLink<X> x) {
        int t = 0;


        for (AbstractClassifier c : tests) {
            c.classify(x.ref, x, t);
            t += c.dimension();
        }

        //record input
        float p = x.priElseZero();
        if (p > 0)
            x.forEach((int i) -> mix[i].accept(p, true, false));

        return x;
    }


    public float gain(int dimension) {
        return 2f * (mixControl.get(dimension) - 0.5f); //bipolarize
    }

    /** computes the gain, and records the (pre-amplified) traffic */
    public float gain(CLink<X> x) {
        float p = x.priElseZero();
        if (p > priMin.floatValue()) {

            for (ObjectIntPair<EnumClassifier<X>> c : dynTests) {
                c.getOne().classify(x.ref, x, c.getTwo());
            }
            final float[] preGain = {0};

            x.forEach((int i) -> {
                mix[i].accept(p, false, true);
                preGain[0] += gain(i);
            });

            //preGain[0] += levels.get(size - 1); //bias

            return
//                    Util.lerp(Util.sigmoid(preGain[0]), gainMin.floatValue(), gainMax.floatValue())
//                sqr( //l^4
                sqr(1f + Util.clamp(preGain[0], -1f, +1f)) //l^2
//        )
                ;
        } else {
            return 0;
        }

        //TODO record the post traffic?

    }

    @Override
    public boolean next() {

        @Nullable MixAgent agent = this.agent;

        //HACK
        if (agent == null || mixControl == null || score == null || agentIn == null) return true;

        agent.act(agentIn, this.lastScore = score.asFloat(), mixControl);

        updateTraffic();

        return true;
    }

    public void setAgent(MixAgent agent) {
        this.agent = agent;
    }

    protected float perceivedTraffic(float t) {
        return t; //linear
        //return (float) Math.log(1+t); //perceptual logarithmic decibel-like
    }

    private void updateTraffic() {
        float totalInput = 0, totalActive = 0;
        float[] nextInput = this.nextInput.data;
        float[] nextActive = this.nextActive.data;
        float[] prevActive = this.nextActive.data.clone();
        for (int i = 0; i < dim; i++) {
            MixChannel mm = this.mix[i];
            float ii = perceivedTraffic(mm.input.sumThenClear());
            float aa = perceivedTraffic(mm.active.sumThenClear());
            nextInput[i] = ii;
            totalInput += ii;
            nextActive[i] = aa;
            totalActive += aa;
        }
        float total = totalInput + totalActive;
        //normalize
        for (int i = 0; i < dim; i++) {
            if (nextInput[i] < Prioritized.EPSILON) {
                nextInput[i] = 0;
            } else {
                nextInput[i] /= total;
            }
//            if (nextActive[i] < Pri.EPSILON) {
////                if (inputEmpty) {
////                    //set the gain for this knob to neutral, so it doesnt need to learn that whtever particular setting exists had any effect
////                    mixControl.set(0.5f, i);
////                    nextActive[i] = 0;
////                }
//            } else {
                //TODO lerp the activation in proportion to the executor's rate, to ammortize the actual loss rather than just reset each cycle
            float a = total >= Prioritized.EPSILON ? nextActive[i] / total : 0f;
            nextActive[i] = Util.lerp(activeTaskMomentum, prevActive[i], a);
            //}
        }

    }

    public String summary() {
        return IntStream.range(0, dim).mapToObj(i -> id(i) + ' ' + n4(trafficInput(i)) + "->" + n4(trafficActive(i))).collect(Collectors.joining(", "));
    }


    @Override
    public PSink<X,CLink<X>> newStream(Object x, Consumer<CLink<X>> each) {
        synchronized (aux) {

            int aux = this.aux.size();
            if (aux >= maxAux)
                throw new RuntimeException("no more sinks available");

            //TODO return a previously created sink with exact name, using Map

            int id = auxStart + aux;
            MixChannel mm = this.mix[id];
            mm.id = x.toString();

            PSink<X,CLink<X>> p = new PSink<X,CLink<X>>(x, each);


            this.aux.add(p);

            return p;
        }
    }

    public String id(int i) {
        return mix[i].id;
    }

    /**
     * value in 0..1.0, percentage of traffic this channel contributes
     */
    public double trafficInput(int i) {
        return input.get(i);
    }

    public double trafficActive(int i) {
        return active.get(i);
    }


}

package nars.game.sensor;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.table.eternal.EternalDefaultTable;
import nars.term.Term;
import nars.truth.Truth;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static nars.Op.BELIEF;
import static nars.Op.SETe;

/**
 * manages a set of N 'digit' concepts whose beliefs represent components of an
 * N-ary (N>=1) discretization of a varying scalar (ie: 32-bit floating point) signal.
 * <p>
 * 'digit' here does not necessarily represent radix arithmetic. instead their
 * value are determined by a ScalarEncoder impl
 * <p>
 * expects values which have been normalized to 0..1.0 range (ex: use NormalizedFloat)
 */
public class DigitizedScalar extends DemultiplexedScalarSensor {


    /**
     * decides the truth value of a 'digit'. returns frequency float
     *
     * @param conceptIndex the 'digit' concept
     * @param x            the value being input
     * @maxDigits the total size of the set of digits being calculated
     */
    @FunctionalInterface
    public interface ScalarEncoder {
        float truth(float x, int digit, int maxDigits);

        default float defaultTruth() {
            return Float.NaN;
        }
    }

    public final List<ComponentSignal> sensors;


    @Override
    public Iterator<ComponentSignal> iterator() {
        return sensors.iterator();
    }

    /**
     * "HARD" - analogous to a filled volume of liquid
     * <p>
     * [ ] [ ] [ ] [ ] 0.00
     * [x] [ ] [ ] [ ] 0.25
     * [x] [x] [ ] [ ] 0.50
     * [x] [x] [x] [ ] 0.75
     * [x] [x] [x] [x] 1.00
     * <p>
     * key:
     * [ ] = freq 0
     * [x] = freq 1,
     */
    public static final ScalarEncoder Fluid = new ScalarEncoder() {
        @Override
        public float truth(float v, int i, int indices) {


            float vv = v * (float) (indices);
            int which = (int) Math.ceil((double) vv);
            float f;
            if (i < which) {
                f = 1f;
            } else if (i > which) {
                f = 0f;
            } else {
                f = 1f - Math.max((float) 0, (vv - (float) which));
            }

            return f;

        }
    };
//    public final static ScalarEncoder Mirror = (v, i, indices) -> {
//        assert (indices == 2);
//        return i == 0 ? v : 1 - v;
//    };

    //        public final float defaultTruth() {
//            return 0;
//        }
    /**
     * hard
     */
    public static final ScalarEncoder Needle = new ScalarEncoder() {
        @Override
        public float truth(float v, int i, int indices) {
            float vv = v * (float) indices;
            int which = (int) Math.floor((double) vv);
            return (float) (i == which ? 1 : 0);
        }
    };

    /**
     * analogous to a needle on a guage, the needle being the triangle spanning several of the 'digits'
     * /          |       \
     * /         / \        \
     * /        /   \         \
     * + + +    + + +     + + +
     * TODO need to analyze the interaction of the produced frequency values being reported by all concepts.
     */
    public static final ScalarEncoder FuzzyNeedle = new ScalarEncoder() {
        @Override
        public float truth(float v, int i, int indices) {

            float dr = 1f / (float) (indices - 1);

            return Math.max((float) 0, (1f - Math.abs(((float) i * dr) - v) / dr));
        }
    };


    /**
     * TODO not quite working yet. it is supposed to recursively subdivide like a binary number, and each concept represents the balance corresponding to each radix's progressively increasing sensitivity
     */
    public static final ScalarEncoder FuzzyBinary = new ScalarEncoder() {
        @Override
        public float truth(float v, int i, int indices) {


            float b = v;
            float dv = 1f;
            for (int j = 0; j < i; j++) {
                dv /= 2f;
                b = Math.max((float) 0, b - dv);
            }


            return b / (dv);
        }
    };

    @Override
    public final int size() {
        return sensors.size();
    }

    /**
     * returns snapshot of the belief state of the concepts
     */
    public Truth[] belief(long when, NAR n) {
        int s = size();
        List<Truth> list = new ArrayList<>();
        for (int i = 0; i < s; i++) {
            Truth truth = n.beliefTruth(sensors.get(i), when);
            list.add(truth);
        }
        Truth[] f = list.toArray(new Truth[0]);
        return f;
    }

    public DigitizedScalar(FloatSupplier input, ScalarEncoder freqer, NAR nar, Term... states) {
        super(input, //$.func(DigitizedScalar.class.getSimpleName(),
                SETe.the(states) //TODO refine
                ///*,$.quote(Util.toString(input))*/, $.the(freqer.getClass().getSimpleName())
                 //   )
                ,
                nar, new FloatFloatToObjectFunction<Truth>() {
                    @Override
                    public Truth value(float prev, float next) {
                        if (next < (float) 0 || next > 1.0F)
                            throw new OutOfRangeException(next, 0, 1);
                        return next == next ? $.t(next, nar.confDefault(BELIEF)) : null;
                    }
                }
        );


        this.input = input;


        float defaultFreq = freqer.defaultTruth();

        assert (states.length > 1);
        this.sensors = new FasterList(states.length);
        int i = 0;
        for (Term s : states) {
            int ii = i++;
            ComponentSignal sc = newComponent(s, new FloatSupplier() {
                @Override
                public float asFloat() {
                    float x = freqer.truth(DigitizedScalar.this.asFloat(), ii, states.length);
                    return Util.equals(x, defaultFreq) ? Float.NaN : x;
                }
            });

            if (defaultFreq==defaultFreq)
                EternalDefaultTable.add(sc, defaultFreq, nar);

            sensors.add(sc);
        }


        //this.nar.start(this);

    }






    



























































}






























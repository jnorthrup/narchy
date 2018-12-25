package nars.concept.sensor;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

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

    public final FloatSupplier input;

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
    }

    public final List<Signal> sensors;



    public final Stream<Signal> stream() {
        return sensors.stream();
    }


    @Override
    public Iterator<Signal> iterator() {
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
    public final static ScalarEncoder Fluid = (v, i, indices) -> {


        float vv = v * (indices);
        int which = (int) Math.ceil(vv);
        float f;
        if (i < which) {
            f = 1f;
        } else if (i > which) {
            f = 0f;
        } else {
            f = 1f-Math.max(0,(vv - which));
        }

        return f;

    };
    public final static ScalarEncoder Mirror = (v, i, indices) -> {
        assert (indices == 2);
        return i == 0 ? v : 1 - v;
    };

    /**
     * hard
     */
    public final static ScalarEncoder Needle = (v, i, indices) -> {
        float vv = v * indices;
        int which = (int) Math.floor(vv);
        return i == which ? 1 : 0;
    };

    /**
     * analogous to a needle on a guage, the needle being the triangle spanning several of the 'digits'
     * /          |       \
     * /         / \        \
     * /        /   \         \
     * + + +    + + +     + + +
     * TODO need to analyze the interaction of the produced frequency values being reported by all concepts.
     */
    public final static ScalarEncoder FuzzyNeedle = (v, i, indices) -> {

        float dr = 1f / (indices - 1);

        return Math.max(0, (1f - Math.abs((i * dr) - v) / dr));
    };


    /**
     * TODO not quite working yet. it is supposed to recursively subdivide like a binary number, and each concept represents the balance corresponding to each radix's progressively increasing sensitivity
     */
    public final static ScalarEncoder FuzzyBinary = (v, i, indices) -> {

        

        float b = v;
        float dv = 1f;
        for (int j = 0; j < i; j++) {
            dv /= 2f;
            b = Math.max(0, b - dv);
        }

        

        return b / (dv);
    };

    /**
     * returns snapshot of the belief state of the concepts
     */
    public Truth[] belief(long when, NAR n) {
        int s = sensors.size();
        Truth[] f = new Truth[s];
        for (int i = 0; i < s; i++)
            f[i] = n.beliefTruth(sensors.get(i), when);
        return f;
    }



    @Override
    public String toString() {
        return id.toString();
    }


    public DigitizedScalar(FloatSupplier input, ScalarEncoder freqer, @NotNull NAR nar, @NotNull Term... states) {
        super(input, $.func(DigitizedScalar.class.getSimpleName(),
                SETe.the(states)
                /*,$.quote(Util.toString(input))*/, $.the(freqer.getClass().getSimpleName())
                    ),
            /** special truther that emphasizes the on concepts more than the off, since more will be off than on usually */
            (prev, next) -> next==next ? $.t(Util.unitize(next),
                //nar.confDefault(BELIEF) * ((1-1f/states.length) * next) + (1f/states.length)
                    nar.confDefault(BELIEF)
            ) : null,
            nar
        );


        this.input = input;


        assert (states.length > 1);
        this.sensors = new FasterList(states.length);
        int i = 0;
        for (Term s : states) {
            final int ii = i++;
            Signal sc = new Signal(s,
                () -> freqer.truth(asFloat(), ii, states.length),
                nar);
            sensors.add(sc);
        }


        nar.on(this);

    }






    



























































}






























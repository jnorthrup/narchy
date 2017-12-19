package nars.concept;

import com.google.common.util.concurrent.AtomicDouble;
import jcog.Util;
import jcog.math.AtomicFloat;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.control.CauseChannel;
import nars.control.NARService;
import nars.task.ITask;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static nars.Op.BELIEF;

/**
 * manages a set of N 'digit' concepts whose beliefs represent components of an
 * N-ary (N>=1) discretization of a varying scalar (ie: 32-bit floating point) signal.
 * <p>
 * 'digit' here does not necessarily represent radix arithmetic. instead their
 * value are determined by a ScalarEncoder impl
 * <p>
 * expects values which have been normalized to 0..1.0 range (ex: use NormalizedFloat)
 */
public class ScalarConcepts extends NARService implements Iterable<SensorConcept>, Consumer<NAR>, FloatSupplier {


    final AtomicFloat value = new AtomicFloat();
    public final CauseChannel<ITask> in;

    @Override
    public final float asFloat() {
        return value.floatValue();
    }

    public Stream<SensorConcept> stream() {
        return sensors.stream();
    }


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

    private final FloatSupplier input;

    @NotNull
    public final List<SensorConcept> sensors;


    float conf;


    @NotNull
    @Override
    public Iterator<SensorConcept> iterator() {
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

        float vv = v * indices;

        int which = (int) Math.floor(vv);
        float f;
        if (i < which) {
            f = 1f;
        } else if (i > which) {
            f = 0f;
        } else {
            f = vv - which;
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

        //float nearness[] = new float[n];

        float b = v;
        float dv = 1f;
        for (int j = 0; j < i; j++) {
            dv /= 2f;
            b = Math.max(0, b - dv);
        }

        //System.out.println(v + " " + b + "/" + dv + " = " + (b/dv));

        return b / (dv);
    };

    /**
     * returns snapshot of the belief state of the concepts
     */
    public Truth[] belief(long when, NAR n) {
        Truth[] f = new Truth[sensors.size()];
        for (int i = 0; i < sensors.size(); i++)
            f[i] = n.beliefTruth(sensors.get(i), when);
        return f;
    }

    public Term get(int i) {
        return sensors.get(i).term();
    }

    public ScalarConcepts resolution(float r) {
        for (SensorConcept s : sensors)
            s.resolution(r);
        return this;
    }


    @Override
    public String toString() {
        return id.toString();
    }

    public ScalarConcepts(FloatSupplier input, @NotNull NAR nar, ScalarEncoder truther, @NotNull Term... states) {
        super(null, $.func(ScalarConcepts.class.getSimpleName(),
                $.sete(states),
                $.quote(Util.toString(input)), $.the(truther.toString())
        ));

        int numStates = states.length;

        assert (numStates > 1);

        this.conf = nar.confDefault(BELIEF);
        this.input = input;
        this.in = nar.newCauseChannel(id);
        //output.amplitude(1f / numStates);

        this.sensors = $.newArrayList(numStates);

        int i = 0;
        for (Term s : states) {
            final int ii = i++;
            SensorConcept sc = new SensorConcept(s, nar, () -> truther.truth(asFloat(), ii, numStates),
                    (x) -> $.t(x, nar.confDefault(BELIEF))
                    //(x) -> $.t(1f, Math.max(nar.confMin.floatValue(), x * nar.confDefault(BELIEF)))
            );
            nar.on(sc);
            sensors.add(sc);
        }

        nar.on(this);

    }

    @Override
    public void accept(NAR n) {
        update(n.time(), n.dur(), n);
    }

    public void update(long now, int dur, NAR n) {

        value.set(input.asFloat());

        in.input(sensors.stream().map(x -> x.update(now, dur, n)));
    }




    //		private Truth biangular(float v) {
//			if (v < 0.5f) return t(0, conf);
//			else {
//				//return t(1f, conf * Math.min(1f,(v-0.5f)*2f));
//				return t(v, conf);
//			}
//		}
//
//		private Truth triangular(float v) {
//			float f, c;
//			if (v < 0.66f && v > 0.33f) {
//				f = 0.5f;
//				c = (0.33f-Math.abs(v-0.5f)) * 3f;
//			} else {
//				f = (v > 0.5f) ? 1 : 0;
//				if (v > 0.5f) {
//					c = Math.abs(v - 0.66f) * 3f;
//				} else {
//					c = Math.abs(v - 0.33f) * 3f;
//				}
//			}
//			c = Util.clamp(c);
//
//			return t(f, c * conf);
//		}

//    @NotNull
//    public FuzzyScalar pri(float p) {
//        for (int i = 0, sensorsSize = sensors.size(); i < sensorsSize; i++) {
//            sensors.get(i).pri(p);
//        }
//        return this;
//    }


//    @Override
//    public void forEach(Consumer<? super SensorConcept> action) {
//        sensors.forEach(action);
//    }

    @NotNull
    public ScalarConcepts resolution() {
        for (int i = 0, sensorsSize = sensors.size(); i < sensorsSize; i++) {
            sensors.get(i);
        }
        return this;
    }

    @NotNull
    public ScalarConcepts conf(float c) {
        this.conf = c;
        return this;
    }


//    @NotNull
//    public String summary() {
//        return Joiner.on("\t").join(Iterators.transform(
//                sensors.iterator(), s -> {
//                    if (s == null)
//                        return "?";
//                    else {
//                        long when = nar.time();
//                        return s.term() + " " + s.beliefs().truth(when, nar);
//                    }
//                }
//        ));
//    }

//    /** clear all sensor's belief state */
//    public void clear() {
//        sensors.forEach(s -> s.beliefs().clear());
//    }

}
///**
// * SensorConcept which wraps a MutableFloat value
// */
//public class FloatConcept extends SensorConcept {
//
//
//    @NotNull
//    private final MutableFloat value;
//
//    public FloatConcept(@NotNull String compoundTermString, @NotNull NAR n) throws Narsese.NarseseException {
//        this(compoundTermString, n, Float.NaN);
//    }
//
//    public FloatConcept(@NotNull String compoundTermString, @NotNull NAR n, float initialValue) throws Narsese.NarseseException {
//        this(compoundTermString, n, new MutableFloat(initialValue));
//    }
//
//    public FloatConcept(@NotNull String compoundTermString, @NotNull NAR n, @NotNull MutableFloat v) throws Narsese.NarseseException {
//        super(compoundTermString, n, v::floatValue,
//                (vv) -> new DefaultTruth(vv, n.confidenceDefault(Symbols.BELIEF) )
//        );
//        this.value = v;
//    }
//
//    public float set(float v) {
//        value.setValue(v);
//        return v;
//    }
//
//}
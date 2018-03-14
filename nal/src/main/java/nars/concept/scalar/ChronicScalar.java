package nars.concept.scalar;

import jcog.Paper;
import jcog.Skill;
import jcog.math.FloatSupplier;
import nars.NAR;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntFunction;

/**
 * an input scalar value de-multiplexed into a set of concepts
 * representing time-averaged samples which capture
 * a scalar signal's behavior as measured in variously sized
 * and variously prioritized time windows.
 * <p>
 * this can be used to model:
 * https://en.wikipedia.org/wiki/Delayed_gratification
 * https://en.wikipedia.org/wiki/Time_preference
 * https://en.wikipedia.org/wiki/Addiction
 * https://www.urbandictionary.com/define.php?term=chronic
 * <p>
 * for use in adaptive reward signal perceptionthat requires, for example:
 * <p>
 * allowing short-term loss as a tradeoff in order to maximize long-term gain
 * <p>
 * (or vice-versa)..
 * <p>
 * may be extended to generate derivatives and
 * integrals of the input signal, at the various
 * time windows, in addition to the 0th-order averages.
 * <p>
 * TODO
 */
@Paper
@Skill({"Psychology", "Addiction", "Motivation", "Delayed_gratification", "Time_preference"})
abstract public class ChronicScalar extends FilteredScalar {

    public ChronicScalar(@Nullable Term id, FloatSupplier input, int windows, IntFunction<Filter> windowBuilder, NAR nar) {
        super(id, input, windows, windowBuilder, nar);
    }

    //TODO

}

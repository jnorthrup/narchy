package nars.concept.scalar;

/**
 *  an input scalar value de-multiplexed into a set of concepts
 *  representing time-averaged samples which capture
 *  a scalar signal's behavior as measured in variously sized
 *  and variously prioritized time windows.
 *
 *  this can be used to model:
 *      https://en.wikipedia.org/wiki/Delayed_gratification
 *      https://en.wikipedia.org/wiki/Time_preference
 *      https://en.wikipedia.org/wiki/Addiction
 *      https://www.urbandictionary.com/define.php?term=chronic
 *
 *  for use in adaptive reward signal perceptionthat requires, for example:
 *
 *    allowing short-term loss as a tradeoff in order to maximize long-term gain
 *
 *    (or vice-versa)..
 *
 *  may be extended to generate derivatives and
 *  integrals of the input signal, at the various
 *  time windows, in addition to the 0th-order averages.
 *
 *  TODO
 */
public class ChronicScalar {

    static class Window {
        public final float dur; //in the system clock units (not global system perception duration)

        public float belief; //relative priority of generated beliefs
        public float goal; //relative priority of generated goals

        public float value; //current (0th order) value, updated when the input signal is reprocessed

        Window(float dur) {
            this.dur = dur;
        }
    }

}

package nars.derive;

..import jcog.memoize.Memoizers;
import jcog.memoize.byt.ByteHijackMemoize;
import nars.derive.util.PremiseKey;
import nars.link.TaskLink;
import nars.term.util.builder.InterningTermBuilder;

import java.util.function.Function;

/** winnows the subset of valid conclusion pathways applicable to a particular Pre-derivation.
 *  this is returned as a short[] of conclusions id's. */
@FunctionalInterface public interface PreDeriver extends Function<PreDerivation,short[]> {

    /** memory-less, evaluated exhaustively each (can re-use the array result) */
    PreDeriver DIRECT_DERIVATION_RUNNER = new PreDeriver() {
        @Override
        public short[] apply(PreDerivation p) {
            return p.preDerive().toArray();
        }
    };

    /** runs, and clones so no possibilty of sharing array result */
    static short[] run(PreDerivation d) {
        return d.preDerive().toArray(true);
    }

    static short[] run(PremiseKey d) {
        return run(d.x);
    }

    final class CentralMemoizer implements PreDeriver {

        final ByteHijackMemoize<PremiseKey, short[]> whats;

        public CentralMemoizer() {
            whats = Memoizers.the.memoizeByte(
                this + "_" + PreDeriver.class.getSimpleName(),
                   Memoizers.DEFAULT_MEMOIZE_CAPACITY,
                   PreDeriver::run);
        }

        @Override
        public short[] apply(PreDerivation d) {
            return intern(d) ? whats.apply(new PremiseKey(d)) : PreDeriver.run(d);
        }

        /** decides what premises can be interned */
        protected static boolean intern(PreDerivation d) {
            return
                !(((Derivation)d)._task instanceof TaskLink) &&
                (d.taskTerm.volume() + d.beliefTerm.volume() <= 3 * InterningTermBuilder.volMaxDefault);
        }

    }


}

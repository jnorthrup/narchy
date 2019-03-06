package nars.derive.premise;

import jcog.memoize.Memoizers;
import jcog.memoize.byt.ByteHijackMemoize;
import nars.derive.Derivation;

import java.util.function.Function;

/** determines the valid conclusions of a particular Pre-derivation.
 * this is returned as a short[] of conclusions id's. */
@FunctionalInterface public interface DeriverPlanner extends Function<PreDerivation,short[]> {

    /** memory-less, evaluated exhaustively each */
    DeriverPlanner DirectDeriverPlanner = DeriverRules::what;

    final class CentrallyMemoizedDeriverPlanner implements DeriverPlanner {

        public final ByteHijackMemoize<PremiseKey, short[]> whats;

        public CentrallyMemoizedDeriverPlanner() {
            whats = Memoizers.the.memoizeByte(this + "_what",
                    Memoizers.DEFAULT_MEMOIZE_CAPACITY*2,
                    d-> DeriverRules.what(d.x));
        }

        @Override
        public short[] apply(PreDerivation d) {
            return whats.apply(new PremiseKey(d, ((Derivation)d).ditherDT));
        }


    }
}

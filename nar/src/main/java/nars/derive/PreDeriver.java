package nars.derive;

import jcog.memoize.Memoizers;
import jcog.memoize.byt.ByteHijackMemoize;
import nars.concept.snapshot.Snapshot;
import nars.derive.util.PremiseKey;
import nars.link.TaskLink;
import nars.term.util.builder.InterningTermBuilder;

import java.util.function.Function;

import static jcog.memoize.Memoizers.DEFAULT_HIJACK_REPROBES;

/** winnows the subset of valid conclusion pathways applicable to a particular Pre-derivation.
 *  this is returned as a short[] of conclusions id's. */
@FunctionalInterface public interface PreDeriver extends Function<PreDerivation,short[]> {

    /** memory-less, evaluated exhaustively each (can re-use the array result) */
    PreDeriver DIRECT_DERIVATION_RUNNER = p -> p.preDerive().toArray();

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



    /** experimental: caches the memoizations in Concept meta maps.
     *  this is likely wasteful even though it attempts to use Soft ref's
     *  TODO use Concept Snapshot API
     *  */
    PreDeriver SnapshotMemoizer = preDerivation -> {
        var d = (Derivation) preDerivation;

        ByteHijackMemoize<PremiseKey, short[]> whats = Snapshot.get(preDerivation.taskTerm, d.nar,
            "ConceptMetaMemoizer_" + System.identityHashCode(d.deriver), d.time, -1, (c, w) -> {
                    var w1 = w;
                    if (w1 == null) {
                        var capacity = 512;
                w1 = new ByteHijackMemoize<>(PreDeriver::run,
                    capacity,
                    DEFAULT_HIJACK_REPROBES, false);
            }
            return w1;
        } );

        //failsafe
        return whats != null ? whats.apply(new PremiseKey(d)) : run(preDerivation);
    };

}

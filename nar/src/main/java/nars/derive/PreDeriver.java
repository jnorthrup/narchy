package nars.derive;

import jcog.memoize.Memoizers;
import jcog.memoize.byt.ByteHijackMemoize;
import nars.concept.snapshot.Snapshot;
import nars.derive.util.PremiseKey;
import nars.term.util.builder.InterningTermBuilder;

import java.util.function.Function;

import static jcog.memoize.Memoizers.DEFAULT_HIJACK_REPROBES;

/** winnows the subset of valid conclusion pathways applicable to a particular Pre-derivation.
 *  this is returned as a short[] of conclusions id's. */
@FunctionalInterface public interface PreDeriver extends Function<PreDerivation,short[]> {

    /** memory-less, evaluated exhaustively each */
    PreDeriver DIRECT_DERIVATION_RUNNER = p->p.preDerive().toArray(false);


    final class CentralMemoizer implements PreDeriver {

        final ByteHijackMemoize<PremiseKey, short[]> whats;

        public CentralMemoizer() {
            whats = Memoizers.the.memoizeByte(this + "_" + PreDeriver.class.getSimpleName(),
                    Memoizers.DEFAULT_MEMOIZE_CAPACITY*2,
                    bd -> ((PreDerivation) bd.x).preDerive().toArray(true));
        }

        @Override
        public short[] apply(PreDerivation d) {
            if (intern(d)) {
                return whats.apply(new PremiseKey(d));
            } else {
                return d.preDerive().toArray(true);
            }
        }

        /** decides what premises can be interned */
        protected boolean intern(PreDerivation d) {
            //return true;
            return d.taskTerm.volume() + d.beliefTerm.volume() <= 4 * InterningTermBuilder.volMaxDefault;
        }

    }


    /** experimental: caches the memoizations in Concept meta maps.
     *  this is likely wasteful even though it attempts to use Soft ref's
     *  TODO use Concept Snapshot API
     *  */
    PreDeriver SnapshotMemoizer = preDerivation -> {
        Derivation d = (Derivation) preDerivation;

        ByteHijackMemoize<PremiseKey, short[]> whats = Snapshot.get(preDerivation.taskTerm, d.nar,
            "ConceptMetaMemoizer" + d.deriver.id.toString(), d.time(), -1, (c, w) -> {
            if (w == null) {
                int capacity = 512;
                w = new ByteHijackMemoize<>(k -> ((PreDerivation) k.x).preDerive().toArray(true),
                    capacity,
                    DEFAULT_HIJACK_REPROBES, false);
            }
            return w;
        } );

        if (whats!=null)
            return whats.apply(new PremiseKey(d));
        else
            return preDerivation.preDerive().toArray(false); //failsafe
    };

}

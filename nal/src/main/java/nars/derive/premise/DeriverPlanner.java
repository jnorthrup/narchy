package nars.derive.premise;

import jcog.memoize.Memoizers;
import jcog.memoize.byt.ByteHijackMemoize;
import nars.concept.NodeConcept;
import nars.derive.Derivation;
import nars.derive.PreDerivation;

import java.util.function.Function;

import static jcog.memoize.Memoizers.DEFAULT_HIJACK_REPROBES;

/** determines the valid conclusions of a particular Pre-derivation.
 * this is returned as a short[] of conclusions id's. */
@FunctionalInterface public interface DeriverPlanner extends Function<PreDerivation,short[]> {

    /** memory-less, evaluated exhaustively each */
    DeriverPlanner DirectDeriverPlanner = DeriverRules::what;

    final class CentralMemoizer implements DeriverPlanner {

        public final ByteHijackMemoize<PremiseKey, short[]> whats;

        public CentralMemoizer() {
            whats = Memoizers.the.memoizeByte(this + "_what",
                    Memoizers.DEFAULT_MEMOIZE_CAPACITY*2,
                    d-> DeriverRules.what(d.x));
        }

        @Override
        public short[] apply(PreDerivation d) {
            return whats.apply(new PremiseKey(d));
        }

    }



    final DeriverPlanner ConceptMetaMemoizer = preDerivation -> {
        Derivation d = (Derivation) preDerivation;


        NodeConcept c = (NodeConcept) d.nar.conceptualize(preDerivation.taskTerm);
        if (c!=null) {
            //NodeConcept.memoize(c, d.deriver.id + "ConceptMetaMemoizer", )
            ByteHijackMemoize<PremiseKey, short[]> whats =
                c.metaWeak("ConceptMetaMemoizer" + d.deriver.id, true, () -> {

                    //int cv = c.term().volume();

                    int capacity = 512;

                    return new ByteHijackMemoize<>(k-> DeriverRules.what(k.x),
                            capacity,
                            DEFAULT_HIJACK_REPROBES, false);
                }
            );
            if (whats!=null)
                return whats.apply(new PremiseKey(d));
        }

        //failsafe:
        return DeriverRules.what(preDerivation);
    };

}

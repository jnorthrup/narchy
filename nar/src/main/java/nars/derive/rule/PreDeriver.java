package nars.derive.rule;

import jcog.memoize.Memoizers;
import jcog.memoize.byt.ByteHijackMemoize;
import jcog.util.ArrayUtil;
import nars.concept.Concept;
import nars.derive.model.Derivation;
import nars.derive.model.PreDerivation;
import nars.derive.premise.PremiseKey;
import nars.term.util.builder.InterningTermBuilder;
import nars.unify.Unify;

import java.util.function.Function;

import static jcog.memoize.Memoizers.DEFAULT_HIJACK_REPROBES;

/** determines the valid conclusions of a particular Pre-derivation.
 * this is returned as a short[] of conclusions id's. */
@FunctionalInterface public interface PreDeriver extends Function<PreDerivation,short[]> {

    /** memory-less, evaluated exhaustively each */
    PreDeriver DIRECT_DERIVATION_RUNNER = PreDeriver::what;

    static short[] what(Unify p) {
        Derivation d = (Derivation)p;
        d.canCollector.clear();
        d.deriver.rules.what.test(d);
        return d.canCollector.isEmpty() ? ArrayUtil.EMPTY_SHORT_ARRAY : d.canCollector.toArray();
    }


    final class CentralMemoizer implements PreDeriver {

        final ByteHijackMemoize<PremiseKey, short[]> whats;

        CentralMemoizer() {
            whats = Memoizers.the.memoizeByte(this + "_what",
                    Memoizers.DEFAULT_MEMOIZE_CAPACITY*2,
                    bd-> what(bd.x));
        }

        @Override
        public short[] apply(PreDerivation d) {
            if (intern(d)) {
                return whats.apply(new PremiseKey(d));
            } else {
                return what(d);
            }
        }

        /** decides what premises can be interned */
        protected boolean intern(PreDerivation d) {
            //return true;
            return d.taskTerm.volume() + d.beliefTerm.volume() <= 3 * InterningTermBuilder.volMaxDefault;
        }

    }


    /** experimental: caches the memoizations in Concept meta maps.
     *  this is likely wasteful even though it attempts to use Soft ref's */
    PreDeriver ConceptMetaMemoizer = preDerivation -> {
        Derivation d = (Derivation) preDerivation;


        Concept c = d.nar().conceptualize(preDerivation.taskTerm);
        if (c!=null) {
            //NodeConcept.memoize(c, d.deriver.id + "ConceptMetaMemoizer", )
            ByteHijackMemoize<PremiseKey, short[]> whats =
                c.metaWeak("ConceptMetaMemoizer" + d.deriver.id, true, () -> {

                    //int cv = c.term().volume();

                    int capacity = 512;

                    return new ByteHijackMemoize<>(k-> what(k.x),
                            capacity,
                            DEFAULT_HIJACK_REPROBES, false);
                }
            );
            if (whats!=null)
                return whats.apply(new PremiseKey(d));
        }

        //failsafe:
        return what(preDerivation);
    };

}
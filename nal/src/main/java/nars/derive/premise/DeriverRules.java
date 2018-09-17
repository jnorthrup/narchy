package nars.derive.premise;

import jcog.Util;
import jcog.decide.MutableRoulette;
import jcog.memoize.Memoizers;
import jcog.memoize.byt.ByteHijackMemoize;
import nars.control.Cause;
import nars.derive.Derivation;
import nars.term.control.PREDICATE;

import java.io.PrintStream;
import java.util.stream.Stream;

import static jcog.pri.ScalarValue.EPSILON;

/**
 * compiled derivation rules
 * what -> can
 */
public class DeriverRules {

    public final PREDICATE<Derivation> what;
    /**
     * the causes that this is responsible for, ie. those that may be caused by this
     */

    /*@Stable*/
    public final Cause[] causes;
    /**
     * TODO move this to a 'CachingDeriver' subclass
     */
    public final ByteHijackMemoize<PremiseKey, short[]> whats;

    /**
     * repertoire
     */
    private final DeriveAction[] could;


    public DeriverRules(PREDICATE<Derivation> what, DeriveAction[] actions) {

        this.what = what;

        assert (actions.length > 0);
        this.could = actions;

        this.causes = Stream.of(actions).flatMap(b -> Stream.of(b.cause)).toArray(Cause[]::new);

        this.whats = new ByteHijackMemoize<>(this::can, 64 * 1024, 4, false);

//            @Override
//            public float value(PremiseKey premiseKey, short[] shorts) {
//                return premiseKey.pri;
//            }
        Memoizers.the.add(toString() + "_what", whats);
    }

    private short[] can(PremiseKey k) {

        Derivation derivation  = k.derivation;

        k.derivation = null;

        derivation.can.clear();

        what.test(derivation);

        return Util.toShort( derivation.can.toArray() );
    }

    /**
     * choice id to branch id mapping
     */
    private final boolean test(Derivation d, int branch) {
        return could[branch].test(d);
    }


    public void run(Derivation d) {


        /**
         * weight vector generation
         */
        short[] _can = d.will;

        int fanOut;
        float[] maybe;
        short[] can;
        if (_can.length > 1) {
            can = _can.clone(); //dont modify the short[] stored in the premise key cache
            maybe = Util.remove(
                    Util.map(choice -> this.could[can[choice]].value(d), new float[can.length]),
                    can,
                    w -> w <= 0
            );
            fanOut = maybe.length;
        } else {
            can = _can;
            if (can.length == 1 && this.could[can[0]].value(d) > EPSILON) {
                fanOut = 1;
            } else {
                fanOut = 0;
            }
            maybe = null;
        }

        if (fanOut > 0) {


            int branchTTL = d.ttl;
                                //* fanOut;

            d.setTTL(branchTTL);

            switch (fanOut) {
                case 1: {
                    test(d, can[0]);
                    //d.revertLive(1); //not necessary
                    break;
                }
                default: {

                    @Deprecated int before = d.now();
                    assert (before == 0);

                    MutableRoulette.run(maybe, d.random, wi -> 0, b -> {

                        test(d, can[b]);

                        return d.revertLive(before, 1);
                    });
                    break;
                }
            }
        }

    }

    /**
     * the conclusions that in which this deriver can result
     */
    public Cause[] causes() {
        return causes;
    }

    public void printRecursive() {
        printRecursive(System.out);
    }

    public void printRecursive(PrintStream p) {
        PremiseDeriverCompiler.print(what, p);
        for (Object x : could)
            PremiseDeriverCompiler.print(x, p);
    }


    public void print(PrintStream p) {
        print(p, 0);
    }

    public void print(PrintStream p, int indent) {
        PremiseDeriverCompiler.print(what, p, indent);
        for (Object x : could)
            PremiseDeriverCompiler.print(x, p, indent);
    }


    public boolean derivable(Derivation x) {
        return (x.will = whats.apply(PremiseKey.get(x))).length > 0;
    }

}

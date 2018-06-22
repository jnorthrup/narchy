package nars.derive.premise;

import jcog.Util;
import jcog.decide.MutableRoulette;
import jcog.memoize.byt.ByteHijackMemoize;
import nars.Param;
import nars.control.Cause;
import nars.derive.Derivation;
import nars.term.control.PREDICATE;

import java.io.PrintStream;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static jcog.pri.Prioritized.EPSILON;

/**
 * compiled derivation rules
 * what -> can
 */
public class PremiseDeriver implements Predicate<Derivation> {

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


    public PremiseDeriver(DeriveAction[] actions, PREDICATE<Derivation> what) {

        this.what = what;
        this.could = actions;

        assert (actions.length > 0);

        this.causes = Stream.of(actions).flatMap(b -> Stream.of(b.cause)).toArray(Cause[]::new);

        this.whats = new ByteHijackMemoize<>(k -> k.solve(what), 64 * 1024, 4) {

            @Override
            public float value(PremiseKey premiseKey, short[] shorts) {
                return premiseKey.pri;
            }
        };
    }

    /**
     * choice id to branch id mapping
     */
    final boolean test(Derivation d, int branch) {
        return could[branch].test(d);
    }

    @Override
    public boolean test(Derivation d) {


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


            int branchTTL = d.nar.deriveBranchTTL.intValue();

            switch (fanOut) {
                case 1: {
                    d.setTTL(Math.max(Param.TTL_MIN, branchTTL));
                    return test(d, can[0]);
                }
                default: {

                    @Deprecated int before = d.now();
                    assert (d.now() == 0);


                    d.ttl = Math.max(Param.TTL_MIN, Math.round(branchTTL
                            //* Math.log(2+fanOut)
                            * (fanOut*0.5f)
                    ));

                    MutableRoulette.run(maybe, d.random, wi -> 0, b -> {

                        if (d.ttl < Param.TTL_MIN)
                            return false;

                        test(d, can[b]);

                        return d.revertLive(before, 1);
                    });
                }
            }
        }
        return true;
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

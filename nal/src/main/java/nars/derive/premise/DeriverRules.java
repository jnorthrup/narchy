package nars.derive.premise;

import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.decide.MutableRoulette;
import jcog.memoize.Memoizers;
import jcog.memoize.byt.ByteHijackMemoize;
import nars.control.Cause;
import nars.derive.Derivation;
import nars.link.TaskLink;
import nars.term.control.PREDICATE;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.stream.Stream;

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
    public final ByteHijackMemoize<PremiseKey, short[/*punc * singleOrDouble*/][/*conclusions*/]> whats;


    public short[] can(Derivation d, short[][] can) {

        byte punc = d.taskPunc;
        boolean singleOrDouble = d.hasBeliefTruth();

        int p = TaskLink.GeneralTaskLink.i(punc);
        int s = singleOrDouble ? 0 : 1;
        int ps = p * 2 + s;
        short[] cps = can[ps];
        if (cps != null)
            return cps;


        d.can.clear();

        what.test(d);

        return can[ps] = Util.toShort(d.can.toArray());
    }

    public boolean derivable(Derivation d) {
        PremiseKey k = PremiseKey.get(d);
        @Nullable short c[][] = whats.getIfPresent(k);
        if (c == null)
            whats.put(k, c = new short[4*2][]);

        return (d.will = can(d, c)).length > 0;
    }

    /**
     * repertoire
     */
    private final DeriveAction[] could;


    public DeriverRules(PREDICATE<Derivation> what, DeriveAction[] actions) {

        this.what = what;

        assert (actions.length > 0);
        this.could = actions;

        this.causes = Stream.of(actions).flatMap(b -> Stream.of(b.cause)).toArray(Cause[]::new);



//            @Override
//            public float value(PremiseKey premiseKey, short[] shorts) {
//                return premiseKey.pri;
//            }
        this.whats = Memoizers.the.memoizeByte(this + "_what", 128 * 1024, null);
    }


    /**
     * choice id to branch id mapping
     */
    private boolean test(Derivation d, int branch) {
        could[branch].run.test(d);
        return d.use(1); //return d.revertLive(0, 1);
    }


    public void run(Derivation d) {


        /**
         * weight vector generation
         */
        short[] could = d.will;

        float[] maybeHow;
        short[] maybeWhat;

        if (could.length > 1) {

            float[] f = Util.map(choice -> this.could[could[choice]].value(d), new float[could.length]);
            int n = f.length;

            MetalBitSet toRemove = null;
            for (int i = 0; i < n; i++) {
                if (f[i] <= 0) {
                    if (toRemove == null) toRemove = MetalBitSet.bits(n);
                    toRemove.set(i);
                }
            }

            if (toRemove == null) {
                maybeWhat = could; //no change
                maybeHow = maybeWhat.length > 1 ? f : null /* not necessary */;

            } else {
                int r = toRemove.cardinality();
                if (r == n) {
                    return; //all removed; nothing remains
                } /*else if (r == n-1) {
                    //TODO all but one
                } */ else {
                    int fanOut = n - r;

                    maybeHow = new float[fanOut];
                    maybeWhat = new short[fanOut];
                    int xx = 0;
                    int i;
                    for (i = 0; i < n; i++) {
                        if (!toRemove.get(i)) {
                            maybeHow[xx] = f[i];
                            maybeWhat[xx++] = could[i];
                        }
                    }
                }
            }

        } else {

            if (could.length == 1 && this.could[could[0]].value(d) > 0) {
                maybeWhat = could;
            } else {
                return;
            }
            maybeHow = null;
        }


        int fanOut = maybeWhat.length; assert(fanOut > 0);

        if (fanOut == 1) {
            test(d, maybeWhat[0]);
        } else {
            //assert((can.length == maybe.length)):  Arrays.toString(could) + " " + Arrays.toString(can) + " " + Arrays.toString(maybe);
            MutableRoulette.run(maybeHow, d.random, wi -> 0, b -> test(d, maybeWhat[b]));
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




}

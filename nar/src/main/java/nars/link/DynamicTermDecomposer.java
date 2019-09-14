package nars.link;

import jcog.Util;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Img;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.compound.SeparateSubtermsCompound;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

import static nars.Op.CONJ;

public abstract class DynamicTermDecomposer implements TermDecomposer {


    /** force descent to maximum 2 layers */
    public static final TermDecomposer Two = new WeightedDynamicTermDecomposer() {
        @Override
        protected int depth(Compound root, Random rng) {
            return 2;
        }
    };
    /** force descent to maximum 1 layers */
    public static final TermDecomposer One = new WeightedDynamicTermDecomposer() {
        @Override
        protected int depth(Compound root, Random rng) {
            return 1;
        }
    };
    public static final TermDecomposer StatementDecomposer = new TermDecomposer() {
        @Override
        public @Nullable Term decompose(Compound t, Random rng) {
            //TODO refine
            Term sub = t.sub(rng.nextBoolean() ? 0 : 1).unneg();
            if (sub instanceof Compound && sub.op()==CONJ) {
                return rng.nextBoolean() ? sub : One.decompose((Compound)sub, rng);
            }
            return sub;
        }
    };

    @Nullable @Override public final Term decompose(Compound t, Random rng) {
        return sampleDynamic(t, depth(t, rng), rng);
    }

    @Nullable private Term sampleDynamic(Compound t, int depthRemain, Random rng) {

        Subterms tt = t instanceof SeparateSubtermsCompound ?  t.subterms() : t;
        int n = tt.subs();

        Term u;
        if (n == 0)
            u = t;
        else if (n == 1)
            u = tt.sub(0);
        else
            u = choose(tt, t, rng);

        if (u instanceof Img)
            return t; //HACK

        u = u.unneg();

        if (depthRemain <= 1 || !(u instanceof Compound) /* || !u.op().conceptualizable */)
            return u;
        else
            return sampleDynamic((Compound)u, depthRemain-1, rng);
    }

    abstract protected int depth(Compound root, Random rng);

    /** simple subterm choice abstraction TODO a good interface providing additional context */
    abstract protected Term choose(Subterms tt, Term parent, Random rng);



    public static final DynamicTermDecomposer Uniform = new DynamicTermDecomposer() {
        @Override
        protected int depth(Compound root, Random rng) {
            return rng.nextFloat() < 0.5f ? 1 : 2;
        }

        @Override protected Term choose(Subterms s, Term parent, Random rng) {
            return s.sub(rng);
        }
    };

    /** uses roulette selection on arbitrary subterm weighting function */
    public static final DynamicTermDecomposer Weighted = new WeightedDynamicTermDecomposer();

    private static class WeightedDynamicTermDecomposer extends DynamicTermDecomposer {
        //        @Override
        //        protected int depth(Compound root, Random rng) {
        //            return 1;
        //        }
                @Override
                protected int depth(Compound root, Random rng) {
                    /* https://academo.org/demos/3d-surface-plotter/?expression=(1%2F(1%2Bx%2F(1%2By)))&xRange=0%2C32&yRange=0%2C8&resolution=23 */
                    int s = root.subs();
                    if (s == 0)
                        return 1;

                    float fanoutRatio =
                            //root.volume() / (1f + root.subs());
                            //1 / (1 + ((float)root.volume())/(1+root.subs()));
                            1 / (1 + (root.volume()-1f)/s);

                    float w =
                            fanoutRatio;
                            //Util.sqr(fanoutRatio);
                            //Util.sqrt(fanoutRatio);
                            //(float)Math.pow(fanoutRatio, 0.75f);
                            //(float)Math.pow(fanoutRatio, 1.5f);

                    float p = rng.nextFloat();

                    return p >= w ? 2 : 1;
                }

        @Override
        protected Term choose(Subterms s, Term parent, Random rng) {
//            if (parent instanceof Sequence) {

//            } else
//            if (parent.op()==CONJ && s.hasAny(CONJ) && Conj.isSeq(parent) && rng.nextBoolean()) {
//                s = ConjList.events(parent).asSubterms(false);
//            }
            return s.subRoulette(this::subValue, rng);
        }

//        private float _subValue(Term sub) {
//            if (sub instanceof Img || sub instanceof Interval /* HACK */)
//                return 0;
//            else
//                return subValue(sub);
//        }

        protected float subValue(Term sub) {
            if (sub instanceof Atomic)
                return 1;

            int v =
                    sub.unneg().volume();
                    //sub.unneg().complexity();
            return
                    1f / Util.sqrt(v); //inverse sqrt
                    //1f / v; //inverse
                    //1f/(v*v); //inverse_sq
                    //Util.sqrt(v);
                    //v;
                    //Util.sqr((float)v);
                    //1; //flat
        }
    }
}

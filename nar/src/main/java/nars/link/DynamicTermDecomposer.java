package nars.link;

import jcog.Util;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.*;
import nars.term.atom.Atomic;
import nars.term.atom.IdempotentBool;
import nars.term.compound.SeparateSubtermsCompound;
import nars.term.util.conj.Conj;
import nars.term.util.conj.ConjList;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

import static nars.Op.CONJ;

public abstract class DynamicTermDecomposer implements TermDecomposer {


//    /** force descent to maximum 2 layers */
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

    /** doesnt unnegate subterms */
    public static final TermDecomposer OnePolarized = new WeightedDynamicTermDecomposer() {
        @Override
        protected int depth(Compound root, Random rng) {
            return 1;
        }

        @Override
        protected boolean unneg() {
            return false;
        }
    };

//    public static final TermDecomposer StatementDecomposer = new TermDecomposer() {
//        @Override
//        public @Nullable Term decompose(Compound t, Random rng) {
//            //TODO refine
//            Term sub = t.sub(rng.nextBoolean() ? 0 : 1).unneg();
//            if (sub instanceof Compound && sub.op()==CONJ) {
//                return rng.nextBoolean() ? sub : One.decompose((Compound)sub, rng);
//            }
//            return sub;
//        }
//    };

    @Override
    public @Nullable Term decompose(Compound t, Random rng) {
        return sampleDynamic(t, depth(t, rng), rng);
    }

    private @Nullable Term sampleDynamic(Compound t, int depthRemain, Random rng) {

        Term u = subterm(t, rng);

        /* || !u.op().conceptualizable */
        return depthRemain <= 1 || !(u instanceof Compound) ? u : sampleDynamic((Compound) u, depthRemain - 1, rng);
    }

    protected Term subterm(Compound t, Random rng) {
        return subterm(t, t instanceof SeparateSubtermsCompound ?  t.subtermsDirect() : t, rng);
    }

    protected Term subterm(Compound x, Subterms tt, Random rng) {

        int n = tt.subs();

        Term y;
        switch (n) {
            case 0:
                y = x;
                break;
            case 1:
                y = tt.sub(0);
                break;
            default:
                y = subtermDecide(tt, x, rng);
                break;
        }

        if (y instanceof Img || y instanceof IdempotentBool)
            return x; //HACK

        return y instanceof Neg && unneg() ? y.unneg() : y;
    }

    protected boolean unneg() {
        return true;
    }

    protected abstract int depth(Compound root, Random rng);

    /** simple subterm choice abstraction TODO a good interface providing additional context */
    protected abstract Term subtermDecide(Subterms tt, Term parent, Random rng);



//    public static final DynamicTermDecomposer Uniform = new DynamicTermDecomposer() {
//        @Override
//        protected int depth(Compound root, Random rng) {
//            return rng.nextFloat() < 0.5f ? 1 : 2;
//        }
//
//        @Override protected Term subtermDecide(Subterms s, Term parent, Random rng) {
//            return s.sub(rng);
//        }
//    };

    /** uses roulette selection on arbitrary subterm weighting function */
    public static final DynamicTermDecomposer Weighted = new WeightedDynamicTermDecomposer();
    public static final DynamicTermDecomposer WeightedConjEvent = new WeightedDynamicTermDecomposer() {
        @Override
        public @Nullable Term decompose(Compound conj, Random rng) {
            if (rng.nextBoolean() && Conj.isSeq(conj))
                return subterm(conj, ConjList.events(conj).asSubterms(false), rng);
            else {
                //possibly embedded conj within conj
                //flat
                return Op.hasAny(conj.subStructure(), CONJ) ? Weighted.decompose(conj, rng) : One.decompose(conj, rng);
            }
        }
    };
    public static final DynamicTermDecomposer WeightedImpl = new WeightedDynamicTermDecomposer() {
        @Override
        public @Nullable Term decompose(Compound t, Random rng) {
            Term subjOrPred = subterm(t, rng);
            if (subjOrPred instanceof Compound && rng.nextBoolean()) {
                return subjOrPred.opID() == (int) CONJ.id ? WeightedConjEvent.decompose((Compound) subjOrPred, rng) : Weighted.decompose((Compound) subjOrPred, rng);
            } else {
                return subjOrPred;
            }
        }
    };

    private static class WeightedDynamicTermDecomposer extends DynamicTermDecomposer implements FloatFunction<Term> {
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
                            1.0F / (1.0F + ((float) root.volume() -1f)/ (float) s);

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
        protected Term subtermDecide(Subterms s, Term parent, Random rng) {
//            if (parent instanceof Sequence) {

//            } else
//            if (parent.op()==CONJ && s.hasAny(CONJ) && Conj.isSeq(parent) && rng.nextBoolean()) {
//                s = ConjList.events(parent).asSubterms(false);
//            }
            return s.subRoulette(rng, this);
        }

        @Override public float floatValueOf(Term subterm) {
            if (subterm instanceof Variable)
                return 0.5f;
            if (subterm instanceof Atomic)
                return 1.0F;

            int v =
                    subterm.unneg().volume();
                    //sub.unneg().complexity();
            return
                    //1f / Util.sqrt(v); //inverse sqrt
                    //1f / v; //inverse
                    //1f/(v*v); //inverse_sq
                    Util.sqrt((float) v);
                    //v;
                    //Util.sqr((float)v);
                    //1; //flat
        }
    }
}

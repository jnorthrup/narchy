package nars.link;

import jcog.Util;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Img;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.compound.SeparateSubtermsCompound;
import nars.term.compound.Sequence;
import nars.term.util.conj.Conj;
import nars.term.util.conj.ConjList;

import java.util.Random;

import static nars.Op.CONJ;

public abstract class DynamicTermLinker implements TermLinker {


    @Override public final Term sample(Term t, Random rng) {
        return t instanceof Compound ? sampleDynamic((Compound)t, depth((Compound)t, rng), rng) : t;
    }

    private Term sampleDynamic(Compound t, int depthRemain, Random rng) {

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



    public static final DynamicTermLinker Uniform = new DynamicTermLinker() {
        @Override
        protected int depth(Compound root, Random rng) {
            return rng.nextFloat() < 0.5f ? 1 : 2;
        }

        @Override protected Term choose(Subterms s, Term parent, Random rng) {
            return s.sub(rng);
        }
    };

    /** uses roulette selection on arbitrary subterm weighting function */
    public static final DynamicTermLinker Weighted = new DynamicTermLinker() {
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
                    //(float)Math.sqrt(fanoutRatio);
                    //(float)Math.pow(fanoutRatio, 0.75f);
                    //(float)Math.pow(fanoutRatio, 1.5f);

            float p = rng.nextFloat();
            if (p < 1-w*w)
                return 3;

            return p >= w ? 2 : 1;
        }

        @Override
        protected Term choose(Subterms s, Term parent, Random rng) {
            if (parent instanceof Sequence) {

            } else if (parent.op()==CONJ && s.hasAny(CONJ) && Conj.isSeq(parent) && rng.nextBoolean()) {
                s = ConjList.events(parent).asSubterms(false);
            }
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
    };
}

package nars.link;

import jcog.TODO;
import jcog.decide.Roulette;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Img;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Interval;
import nars.term.compound.SeparateSubtermsCompound;
import nars.term.compound.Sequence;
import nars.term.util.conj.Conj;
import nars.term.util.conj.ConjList;

import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;

import static nars.Op.CONJ;

public abstract class DynamicTermLinker implements TermLinker {

    @Override
    public Stream<? extends Term> targets() {
        throw new TODO();
    }

    @Override
    public final Term sample(Term t, Random rng) {
        return t instanceof Compound ? sampleDynamic((Compound)t, t instanceof Compound ? depth((Compound)t, rng) : 1, rng) : t;
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
            u = choose(tt, n, t, rng);

        if (u instanceof Img)
            return t; //HACK

        u = u.unneg();
        if (depthRemain <= 1 || !(u instanceof Compound))
            return u;

        Op uo = u.op();
        if (!uo.conceptualizable)
            return u;

        return sampleDynamic((Compound)u, depthRemain-1, rng);
    }

    abstract protected int depth(Compound root, Random rng);

    /** simple subterm choice abstraction TODO a good interface providing additional context */
    abstract protected Term choose(Subterms tt, int n, Term parent, Random rng);



    @Override
    public void sample(Random rng, Function<? super Term, SampleReaction> each) {
        throw new TODO();
    }

    public static final DynamicTermLinker Uniform = new DynamicTermLinker() {
        @Override
        protected int depth(Compound root, Random rng) {
            return rng.nextFloat() < 0.5f ? 1 : 2;
        }

        @Override protected Term choose(Subterms tt, int n, Term parent, Random rng) {
            int s = rng.nextInt(n);
            return tt.sub(s);
        }
    };

    /** uses roulette selection on arbitrary subterm weighting function */
    public static final DynamicTermLinker Weighted = new DynamicTermLinker() {
        @Override
        protected int depth(Compound root, Random rng) {
            return 1;
        }
        //        @Override
//        protected int depth(Compound root, Random rng) {
//            /* https://academo.org/demos/3d-surface-plotter/?expression=(1%2F(1%2Bx%2F(1%2By)))&xRange=0%2C32&yRange=0%2C8&resolution=23 */
//            float fanoutRatio =
//                    //root.volume() / (1f + root.subs());
//                    //1 / (1 + ((float)root.volume())/(1+root.subs()));
//                    1 / (1 + (((float)(root.volume()-1))/(1+root.subs())));
//
//            float w =
//                    fanoutRatio;
//                    //(float)Math.sqrt(fanoutRatio);
//                    //(float)Math.pow(fanoutRatio, 0.75f);
//                    //(float)Math.pow(fanoutRatio, 1.5f);
//
//            return rng.nextFloat() < w ? 1 : 2;
//        }

        @Override
        protected Term choose(Subterms _s, int n, Term parent, Random rng) {
            if (parent instanceof Sequence) {

            } else if (parent.op()==CONJ && _s.hasAny(CONJ) && Conj.isSeq(parent) && rng.nextBoolean()) {
                _s = ConjList.events(parent).asSubterms(false);
            }
            Subterms s = _s;
            return s.sub(Roulette.selectRoulette(n, i -> s.subFloat(i, this::subValue) , rng));
        }

        private float _subValue(Term sub) {
            if (sub instanceof Img || sub instanceof Interval /* HACK */)
                return 0;
            else
                return subValue(sub);
        }

        protected float subValue(Term sub) {
            if (sub instanceof Atomic)
                return 1;

            int v =
                    sub.unneg().volume();
                    //sub.unneg().complexity();
            return
                    1f/v; //inverse
                    //Util.sqrt(v);
                    //v;
                    //Util.sqr((float)v);
                    //1f/(v*v); //inverse_sq
        }
    };
}

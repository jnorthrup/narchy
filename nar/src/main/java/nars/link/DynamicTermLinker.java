package nars.link;

import jcog.TODO;
import jcog.decide.Roulette;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.var.Img;

import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class DynamicTermLinker implements TermLinker {

    @Override
    public Stream<? extends Term> targets() {
        throw new TODO();
    }

    @Override
    public final Term sample(Term t, Random rng) {
        return sampleDynamic(t, depth(t, rng), rng);
    }

    protected final Term sampleDynamic(Term t, int depthRemain, Random rng) {
        if (depthRemain <= 0 || t.op().atomic)
            return t;

        Subterms tt = t.subterms();
        int n = tt.subs();

        Term u;
        if (n == 0)
            u = t;
        else if (n == 1)
            u = tt.sub(0);
        else
            u = choose(tt, n, t, rng);


        if (u instanceof Img)
            return t;

        u = u.unneg();
        Op uo = u.op();

        if (uo.atomic || !(uo.conceptualizable))
            return u; //end
        else
            return sampleDynamic(u, depthRemain-1, rng);
    }

    abstract protected int depth(Term root, Random rng);

    /** simple subterm choice abstraction TODO a good interface providing additional context */
    abstract protected Term choose(Subterms tt, int n, Term parent, Random rng);



    @Override
    public void sample(Random rng, Function<? super Term, SampleReaction> each) {
        throw new TODO();
    }

    public static final DynamicTermLinker RandomDynamicTermLinker = new DynamicTermLinker() {
        @Override
        protected int depth(Term root, Random rng) {
            return rng.nextFloat() < 0.5f ? 1 : 2;
        }

        @Override protected Term choose(Subterms tt, int n, Term parent, Random rng) {
            int s = rng.nextInt(n);
            return tt.sub(s);
        }
    };

    public static final DynamicTermLinker VolWeighted = new DynamicTermLinker() {
        @Override
        protected int depth(Term root, Random rng) {
            float w = (float)Math.sqrt(root.volume() / (1 + root.subs()));
            return rng.nextFloat() < w ? 1 : 2;
        }
        @Override
        protected Term choose(Subterms tt, int n, Term parent, Random rng) {
            return tt.sub(Roulette.selectRoulette(n, i->tt.sub(i).volume(), rng));
        }
    };
}

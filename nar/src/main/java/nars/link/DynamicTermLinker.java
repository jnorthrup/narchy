package nars.link;

import jcog.TODO;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.var.Img;

import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;

public class DynamicTermLinker implements TermLinker {

    public static TermLinker DynamicLinker = new DynamicTermLinker();

    private DynamicTermLinker() {

    }

    @Override
    public Stream<? extends Term> targets() {
        throw new TODO();
    }

    @Override
    public Term sample(Term t, Random random) {
        return sampleDynamic(t, random.nextFloat() < 0.5f ? 1 : 2, random);
    }

    protected Term sampleDynamic(Term t, int depthRemain, Random rng) {
        if (depthRemain <= 0 || t.op().atomic)
            return t;

        Subterms tt = t.subterms();
        int n = tt.subs();
        if (n == 0)
            return t;

        int s = rng.nextInt(n);

        Term u = tt.sub(s);
        if (u instanceof Img)
            return t;

        u = u.unneg();
        Op uo = u.op();

        if (uo.atomic || !(uo.conceptualizable))
            return u; //end
        else
            return sampleDynamic(u, depthRemain-1, rng);
    }


    @Override
    public void sample(Random rng, Function<? super Term, SampleReaction> each) {
        throw new TODO();
    }
}

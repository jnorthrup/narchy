package nars.link;

import jcog.data.list.FasterList;
import nars.NAL;
import nars.Op;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.var.Img;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;

import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;

import static nars.link.TemplateTermLinker.layers;


public final class PathTermLinker extends FasterList<byte[]> implements TermLinker {

    public final Term root;


    /**
     * default recursive termlink templates constructor
     */
    PathTermLinker(Term root, int layers) {

        this.root = root;

        if (root.subs() > 0) {

            if (NAL.test.DEBUG_EXTRA) {
                if (!root.equals(root.concept()))
                    throw new RuntimeException("templates only should be generated for rooted terms:\n\t" + root + "\n\t" + root.concept());
            }


            ByteArrayList p = new ByteArrayList(3);
            root.subterms().forEachI((s, i) -> {
                add(root, s, 0, layers, p, (byte) i);
            });

        }

    }

    public static TermLinker of(Term term) {

        if (term instanceof Compound && term.subs() > 0) {
            PathTermLinker l = new PathTermLinker(term, layers(term));
            if (!l.isEmpty())
                return l;
        }

        return NullLinker;

    }

    @Override
    public Stream<? extends Term> targets() {
        return stream().map(root::subPath);
    }

    /**
     * recurses into subterms
     */
    private void add(Term parent, Term x, int depth, int maxDepth, ByteArrayList path, byte index) {

        if (x instanceof Img)
            return;

        path.add(index);

        boolean neg = x instanceof Neg;
        if (neg) {
            x = x.unneg();
            path.add((byte) 0);
        }

        add(path.toArray());

        //maxDepth += deeper(depth, root, x);
        if (++depth < maxDepth) {
            Op xo = x.op();
            if (!(xo.atomic || !xo.conceptualizable)) {

                int nextDepth = depth, nextMaxDepth = maxDepth;

                Term X = x;
                x.subterms().forEachI((sub, i) -> {

                    add(X, sub, nextDepth, nextMaxDepth, path, (byte) i);

                });
            }

        }

        if (neg)
            path.removeAtIndex(path.size() - 1); //pop

        path.removeAtIndex(path.size() - 1); //pop


    }

    @Override
    @Deprecated public void sample(Random rng, Function<? super Term, SampleReaction> each) {
        each.apply(root.subPath(get(rng)));
    }

    @Override
    public Term sample(Term term, Random random) {
        return sample(random);
    }
}

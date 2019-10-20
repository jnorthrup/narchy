package nars.control;

import jcog.TODO;
import jcog.Util;
import jcog.memoize.HijackMemoize;
import nars.term.Term;
import nars.term.atom.IdempotInt;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

import static nars.Op.CONJ;

/** TODO */
public class CauseMap<X> {

    public final HijackMemoize<Term, MutableIntSet> cause = new HijackMemoize<>(
            k -> new IntHashSet(1).asSynchronized(), 4096, 4, false);

    public void start(X x, short... cause) {
        MutableIntSet cc = this.cause.apply(causeTerm(cause));
        if (cc!=null)
            cc.add(System.identityHashCode(x));
    }
    public void next(X x, Term current, short... next) {
        /*
          X Y

         */
        throw new TODO();
    }

    protected Term causeTerm(short... c) {
        switch (c.length) {
            case 0: throw new NullPointerException();
            case 1: return IdempotInt.the((int) c[0]);
            default:
                return CONJ.the(Util.map(0,c.length, Term[]::new, (i)-> IdempotInt.the((int) c[i])));
        }
    }

}

package nars.term.buffer;

import jcog.TODO;
import jcog.data.iterator.ArrayIterator;
import nars.term.Term;
import nars.term.util.transform.TermTransform;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Iterator;
import java.util.Map;

/** term combination iterator
 * TODO finish and test
 * */
public class Termerator extends TermBuffer implements Iterable<Term> {

    Map<Term,Object> subs = new UnifiedMap();
    boolean subsChanged = false;

    private Termerator(Term x, TermTransform f) {
        super();
        append(x, f);
    }

    public final boolean is(Term x, Term y) {
        return is(x, (Object)y);
    }

    private boolean is(Term x, Object y) {
        assert(!(x.equals(y)));
        Object y0 = subs.put(x, y);
        assert(y0 ==null);
        subsChanged = true;
        return true;
    }

    public boolean isTry(Term x, Term y) {
        assert(!(x.equals(y)));
        Object y0 = subs.putIfAbsent(x, y);
        if (y0 == null) {
            subsChanged = true;
            return true;
        }
        return false;
    }

    public boolean canBe(Term x, Iterable<Term> y) {
        return is(x, y);
    }
    public boolean canBe(Term x, Term... y) {
        if (y.length == 1)
            return is(x, y[0]);
        else
            return canBe(x, ArrayIterator.iterable(y));
    }

    @Override
    protected Term nextTerm(byte[] bytes, int[] range) {

        Term x = super.nextTerm(bytes, range);
        Object y = subs.get(x);
        if(y!=null) {
            if (y instanceof Term) {
                return (Term) y;
            } else {
                //termutator
                //TODO remove subs entry, rely on termutator cartesian iterator afterward
                throw new TODO();
            }
        }
        return x;
    }

    @Override
    public Iterator<Term> iterator() {
//        if (subsChanged) {
//
//        }

        return null;
//        subs.entrySet().removeIf((e)->{
//            Object v = e.getValue();
//            if (v instanceof Term) {
//                this.sub.interReplace()
//                return true;
//            }
//
//        });
//        if (subs.isEmpty())
//            return Iterators.singletonIterator(term());
//        else {
//
//            return subs.entrySet().stream(x->{
//               TermBuffer t = clone();
//
//
//            });
//        }

    }


}

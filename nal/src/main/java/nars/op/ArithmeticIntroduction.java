package nars.op;

import jcog.Util;
import jcog.list.FasterList;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.bag.leak.LeakBack;
import nars.term.Term;
import nars.term.Variable;
import nars.term.anon.Anom;
import nars.term.anon.Anon;
import nars.term.atom.Int;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import static nars.Op.*;

/**
 * introduces arithmetic relationships between differing numeric subterms
 * TODO
 *      greater/less than comparisons
 *
 */
public class ArithmeticIntroduction extends LeakBack {

    public static Term apply(Term x, Random rng) {
        return apply(x, null, rng);
    }

    public static Term apply(Term x, @Nullable Anon anon, Random rng) {
        if ((anon == null && !x.hasAny(INT)) || x.complexity() < 3)
            return x;

        //find all unique integer subterms
        IntHashSet ints = new IntHashSet();
        x.recurseTerms((t) -> {
            Int it = null;
            if (t instanceof Anom) {
                Anom aa = ((Anom) t);
                Term ta = anon.get(aa);
                if (ta.op() == INT)
                    it = ((Int) ta);
            } else if (t instanceof Int) {
                it = (Int) t;
            }
            if (it == null)
                return;

            ints.add((it.id));
        });

        //Set<Term> ints = ((Compound) x).recurseTermsToSet(INT);
        int ui = ints.size();
        if (ui <= 1)
            return x; //nothing to do

        int[] ii = ints.toSortedArray();  //increasing so that relational comparisons can assume that 'a' < 'b'

        //potential mods to select from
        //FasterList<Supplier<Term[]>> mods = new FasterList(1);
        IntObjectHashMap<List<Supplier<Term[]>>> mods = new IntObjectHashMap(ii.length);

        Variable v =
                $.varDep("x");
                //$.varIndep("x");

        //test arithmetic relationships
        for (int a = 0; a < ui; a++) {
            int ia = ii[a];
            for (int b = a + 1; b < ui; b++) {
                int ib = ii[b];
                assert(ib > ia);

                if (ib - ia < ia && (ia!=0)) {

                    mods.getIfAbsentPut(ia, FasterList::new).add(()-> new Term[]{
                            Int.the(ib), $.func("add", v, $.the(ib - ia))
                    });

                    mods.getIfAbsentPut(ib, FasterList::new).add(()-> new Term[]{
                            Int.the(ia), $.func("add", v, $.the(ia - ib))
                    });

                } else if ((ia!=0 && ia!=1) && (ib!=0 && ib!=1) && Util.equals(ib/ia, (((float)ib)/ia), Float.MIN_NORMAL)) {

                    mods.getIfAbsentPut(ia, FasterList::new).add(()-> new Term[]{
                            Int.the(ib), $.func("mul", v, $.the(ib/ia))
                    });
                } else if (ia == -ib) {
                    //negation (x * -1)
                    mods.getIfAbsentPut(ia, FasterList::new).add(()-> new Term[]{
                            Int.the(ib), $.func("mul", v, $.the(-1))
                    });
                    mods.getIfAbsentPut(ib, FasterList::new).add(()-> new Term[]{
                            Int.the(ia), $.func("mul", v, $.the(-1))
                    });
                }

            }
        }
        if (mods.isEmpty())
            return x;

        //TODO fair select randomly if multiple of the same length

        RichIterable<IntObjectPair<List<Supplier<Term[]>>>> mkv = mods.keyValuesView();

        int ms = mkv.maxBy(e -> e.getTwo().size()).getTwo().size();
        mkv.reject(e->e.getTwo().size() < ms);

        //convention: choose lowest base
        MutableList<IntObjectPair<List<Supplier<Term[]>>>> mmm = mkv.toSortedListBy(IntObjectPair::getOne);

        IntObjectPair<List<Supplier<Term[]>>> m = mmm.get(0);
        int base = m.getOne();
        Term baseTerm = Int.the(base);
        if (anon!=null)
            baseTerm = anon.put(baseTerm);

        Term yy = x.replace(baseTerm, v);

        for (Supplier<Term[]> s : m.getTwo()) {
            Term[] mm = s.get();
            if (anon!=null)
                mm[0] = anon.put(mm[0]);
            yy = yy.replace(mm[0], mm[1]);
        }

        Term y =
                CONJ.the(yy, SIM.the(baseTerm, v));
                //IMPL.the(SIM.the(baseTerm, v), yy);
                //IMPL.the(yy, SIM.the(baseTerm, v));

        if (y.op()!=CONJ) {
        //if (y.op()!=IMPL) {
            return null; //something happened
        }

        if (x.isNormalized()) {
            y = y.normalize();
        }
        return y;
    }

    public static final Logger logger = LoggerFactory.getLogger(ArithmeticIntroduction.class);


    public ArithmeticIntroduction(int taskCapacity, NAR n) {
        super(taskCapacity, n);
    }

    @Override
    protected boolean preFilter(Task next) {
        return next.term().hasAny(Op.INT);
    }

    @Override
    protected float leak(Task xx) {
        Term x = xx.term();
        Term y = apply(x, nar.random());
        if (y!=null && !y.equals(x) && y.op().conceptualizable) {
            Task yy = Task.clone(xx, y);
            if (yy!=null) {
                input(yy);
                return 1;
            }
        } else {
//            if (Param.DEBUG)
//                logger.warn("fail: task={} result=", xx, y);
        }

        return 0;
    }



}

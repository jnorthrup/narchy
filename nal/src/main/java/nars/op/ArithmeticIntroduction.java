package nars.op;

import jcog.list.FasterList;
import nars.$;
import nars.term.Term;
import nars.term.anon.Anom;
import nars.term.anon.Anon;
import nars.term.atom.Int;
import nars.term.var.Variable;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Supplier;

import static nars.Op.CONJ;
import static nars.Op.INT;
import static nars.Op.SIM;

/**
 * introduces arithmetic relationships between differing numeric subterms
 */
public class ArithmeticIntroduction {

    public static Term apply(Term x, Random rng) {
        return apply(x, null, rng);
    }

    public static Term apply(Term x, @Nullable Anon anon, Random rng) {
        if (x.complexity() < 3 || (anon==null && !x.hasAny(INT)))
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
        FasterList<Supplier<Term[]>> mods = new FasterList(1);

        //test arithmetic relationships
        for (int a = 0; a < ui; a++) {
            for (int b = a + 1; b < ui; b++) {
                int ia = ii[a];
                int ib = ii[b];


                if (ib == ia + 1 && (ia!=0)) {
                    mods.add(() -> {
                        Variable vx = $.varDep("x");
                        return new Term[]{
                            Int.the(ia), vx,
                            Int.the(ib), $.func("add", vx, $.the(1))
                        };
                    });
                }

                if (ib == ia*2 && (ia!=1 /* use add if ia==1 */)) {
                    mods.add(() -> {
                        Variable vx = $.varDep("x");
                        return new Term[]{
                                Int.the(ia), vx,
                                Int.the(ib), $.func("mul", vx, $.the(2))
                        };
                    });
                }

            }
        }

        int nm = mods.size();

        Supplier<Term[]> m;
        switch (nm) {
            case 0:
                return x;
            case 1:
                m = mods.get(0);
                break;
            default:
                m = mods.get(rng);
                break;
        }

        Term[] mm = m.get();
        if (anon!=null) {
            mm[0] = anon.put(mm[0]);
            mm[2] = anon.put(mm[2]);
        }
        Term yy = x.replace(mm[0], mm[1]).replace(mm[2], mm[3]);
        Term y = CONJ.the(yy, SIM.the(mm[0], mm[1]));
        if (x.isNormalized()) {
            y = y.normalize();
        }
        return y;
    }

}

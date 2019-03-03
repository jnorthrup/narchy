package nars.unify;

import jcog.data.list.FasterList;
import nars.term.Term;
import nars.term.Variable;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Map;

public class MapUnification extends DeterministicUnification {

    Map<Term, Term> xy;

    //TODO
    int matchStructure = Integer.MAX_VALUE;

    public MapUnification() {
        super();
        this.xy = new UnifiedMap(4);
    }

    @Override
    protected boolean equals(DeterministicUnification obj) {
        if (obj instanceof nars.unify.MapUnification) {
            nars.unify.MapUnification u = (nars.unify.MapUnification) obj;
            if (u.matchStructure != matchStructure)
                return false;
            return xy.equals(u.xy);
        }
        return false;
    }

    @Override
    void apply(Unify u) {
        xy.forEach((tx, ty) -> {
            boolean applied = u.putXY((Variable/*HACK*/) tx, ty);
            assert (applied);
        });
    }

    public void put(Term x, Term y) {
        xy.put(x, y);
//            if (x.op() != VAR_PATTERN)
//                matchStructure |= (x.structure() & ~Op.Variable);
    }

    public void putIfAbsent(Term x, Term y) {
        xy.putIfAbsent(x, y);
//            if (x.op() != VAR_PATTERN)
//                matchStructure |= (x.structure() & ~Op.Variable);
    }

    @Override
    public final Term xy(Term x) {
        return xy.get(x);
    }


    @Override
    public String toString() {
        return "unification(" + xy + ")";
    }

    public nars.unify.MapUnification putIfAbsent(FasterList<Term> xyPairs) {
        for (int i = 0, n = xyPairs.size(); i < n; ) {
            putIfAbsent(xyPairs.get(i++), xyPairs.get(i++));
        }
        ((UnifiedMap)xy).trimToSize();
        return this;
    }
}

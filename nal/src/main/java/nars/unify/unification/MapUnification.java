package nars.unify.unification;

import jcog.data.list.FasterList;
import nars.term.Term;
import nars.term.Variable;
import nars.unify.Unify;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Map;

public class MapUnification extends DeterministicUnification {

    final Map<Variable, Term> xy;

    //TODO
    //int matchStructure = Integer.MAX_VALUE;

    public MapUnification() {
        super();
        this.xy = new UnifiedMap(4,1f);
    }

    @Override
    protected boolean equals(DeterministicUnification obj) {
        if (obj instanceof MapUnification) {
            MapUnification u = (MapUnification) obj;
//            if (u.matchStructure != matchStructure)
//                return false;
            return xy.equals(u.xy);
        }
        return false;
    }

    @Override
    public boolean apply(Unify u) {
        xy.forEach((tx, ty) -> {
            boolean applied = u.putXY((Variable/*HACK*/) tx, ty);
            assert (applied);
        });
        return true;
    }

    public void put(Variable x, Term y) {
        xy.put(x, y);
//            if (x.op() != VAR_PATTERN)
//                matchStructure |= (x.structure() & ~Op.Variable);
    }

    public void putIfAbsent(Variable x, Term y) {
        xy.putIfAbsent(x, y);
//            if (x.op() != VAR_PATTERN)
//                matchStructure |= (x.structure() & ~Op.Variable);
    }

    @Override
    public final Term xy(Variable x) {
        return xy.get(x);
    }


    @Override
    public String toString() {
        return "unification(" + xy + ')';
    }

    public MapUnification putIfAbsent(FasterList<Term> xyPairs) {
        for (int i = 0, n = xyPairs.size(); i < n; ) {
            putIfAbsent((Variable)xyPairs.get(i++), xyPairs.get(i++));
        }
        ((UnifiedMap)xy).trimToSize();
        return this;
    }
}

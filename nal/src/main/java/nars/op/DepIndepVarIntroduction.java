package nars.op;

import jcog.data.list.FasterList;
import nars.$;
import nars.Op;
import nars.term.Term;
import nars.term.Terms;
import nars.term.Variable;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.ToIntFunction;

import static nars.Op.CONJ;

/**
 * 1-iteration DepVar and IndepVar introduction that emulates and expands the original NAL6 Variable Introduction Rules
 */
public class DepIndepVarIntroduction extends VarIntroduction {

    public static final DepIndepVarIntroduction the = new DepIndepVarIntroduction();

    private final static int ConjOrStatementBits = Op.IMPL.bit | Op.CONJ.bit;

    private final static int DepOrIndepBits = Op.VAR_INDEP.bit | Op.VAR_DEP.bit | Op.VAR_PATTERN.bit;

    /**
     * sum by complexity if passes include filter
     */
    private static final ToIntFunction<Term> depIndepFilter = t -> {
        if (t.op().var) return 0;

        return t.hasAny(
                Op.VAR_INDEP.bit
        ) ? 0 : 1;
    };

    /** if no variables are present in the target term, use the normalized variable which can help ensure avoidance of a need for full compound normalization */
    private static final Variable UnnormalizedVarIndep = $.varIndep("X");
    private static final Variable FirstNormalizedVarIndep = $.varIndep(1);
    private static final Variable UnnormalizedVarDep = $.varDep("Y");
    private static final Variable FirstNormalizedVarDep = $.varDep(1);

    private static boolean validDepVarSuperterm(Op o) {
        return /*o.statement ||*/ o == CONJ;
    }

    private static boolean validIndepVarSuperterm(Op o) {
        return o.statement;

    }

    @Override
    public Pair<Term, Map<Term, Term>> apply(Term x, Random r) {
        if (x.hasAny(ConjOrStatementBits)) {
            return super.apply(x, r);
        } else
            return null;
    }

    @Override
    protected Term select(Term input, Random shuffle) {
        return Terms.nextRepeat(input, depIndepFilter, 2, shuffle);
    }

    @Nullable
    @Override
    protected Term introduce(Term input, Term selected, byte order) {

        Op inOp = input.op();
        List<ByteList> paths = new FasterList<>(4);
        int minPathLength = inOp.statement ? 2 : 0;
        input.pathsTo(selected, (path, t) -> {
            if (path.size() >= minPathLength)
                paths.add(path.toImmutable());
            return true;
        });

        int pSize = paths.size();
        if (pSize <= 1)
            return null;


        Op commonParentOp = input.commonParent(paths).op();


        boolean depOrIndep;
        switch (commonParentOp) {
            case CONJ:
                depOrIndep = true;
                break;
            case IMPL:
                depOrIndep = false;
                break;
            default:
                return null;

        }


        ObjectByteHashMap<Term> m = new ObjectByteHashMap<>(4);
        for (ByteList p: paths) {
            Term t = null;
            int pathLength = p.size();
            for (int i = -1; i < pathLength - 1 /* dont include the selected term itself */; i++) {
                t = (i == -1) ? input : t.sub(p.get(i));
                Op o = t.op();

                if (!depOrIndep && validIndepVarSuperterm(o)) {
                    byte inside = (byte) (1 << p.get(i + 1));
                    m.updateValue(t, inside, (previous) -> (byte) (previous | inside));
                } else if (depOrIndep && validDepVarSuperterm(o)) {
                    m.addToValue(t, (byte) 1);
                }
            }
        }


        return (!depOrIndep) ?
            ((m.anySatisfy(b -> b == 0b11)) ?
                    (input.hasVars()  ? UnnormalizedVarIndep : FirstNormalizedVarIndep) /*varIndep(order)*/ : null)
                        :
            (m.anySatisfy(b -> b >= 2) ?
                    (input.hasVars()  ? UnnormalizedVarDep : FirstNormalizedVarDep)  /* $.varDep(order) */ : null)
        ;

    }


}














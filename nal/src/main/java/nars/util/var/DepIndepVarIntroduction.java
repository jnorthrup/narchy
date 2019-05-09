package nars.util.var;

import jcog.data.list.FasterList;
import jcog.decide.Roulette;
import jcog.memoize.Memoizers;
import nars.$;
import nars.NAL;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.Terms;
import nars.term.Variable;
import nars.term.util.cache.Intermed;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import static nars.Op.CONJ;

/**
 * 1-iteration DepVar and IndepVar introduction that emulates and expands the original NAL6 Variable Introduction Rules
 */
public class DepIndepVarIntroduction extends VarIntroduction {

    public static final DepIndepVarIntroduction the = new DepIndepVarIntroduction();

    private final static int ConjOrStatementBits = Op.IMPL.bit | Op.CONJ.bit;

    /**
     * sum by complexity if passes include filter
     */
    public static final ToIntFunction<Term> depIndepFilter = t ->
        (t.op().var) ? 0 : (t.hasAny(Op.VAR_INDEP.bit) ? 0 : 1);
    public static final ToIntFunction<Term> nonNegdepIndepFilter = t ->
            t.op().isAny(Op.Variable|Op.NEG.bit) ? 0 : (t.hasAny(Op.VAR_INDEP.bit) ? 0 : 1);

    /** if no variables are present in the target target, use the normalized variable which can help ensure avoidance of a need for full compound normalization */
    private static final Variable UnnormalizedVarIndep = $.varIndep("_v");
    private static final Variable UnnormalizedVarDep = $.varDep("_v");
    private static final Variable FirstNormalizedVarIndep = $.varIndep(1);
    private static final Variable FirstNormalizedVarDep = $.varDep(1);

    private static boolean validDepVarSuperterm(Op o) {
        return /*o.statement ||*/ o == CONJ;
    }

    private static boolean validIndepVarSuperterm(Op o) { return o.statement; }

    @Nullable @Override
    public Pair<Term, Map<Term, Term>> apply(Term x, Random rng) {
        return x.hasAny(ConjOrStatementBits) ? super.apply(x, rng) : null;
    }

    @Override
    protected Term[] select(Subterms input) {
        return select.apply(input.commuted());
    }

    private static final int MEMOIZE_CAPACITY = 32 * 1024;

    private final static Function<Subterms,Term[]> select = Memoizers.the.memoizeByte(
            DepIndepVarIntroduction.class.getSimpleName() + "_select",
            Intermed.SubtermsKey::new,
            DepIndepVarIntroduction::_select, MEMOIZE_CAPACITY);

    private static Term[] _select(Intermed.SubtermsKey input) {
        Term[] n = Terms.nextRepeat(input.subs,
                NAL.term.VAR_INTRODUCTION_NEG_FILTER ? nonNegdepIndepFilter : depIndepFilter, 2);
        return Objects.requireNonNullElse(n, Op.EmptyTermArray);
    }


    @Override protected Term choose(Term[] x, Random rng) {
        IntToFloatFunction curve =
                //n -> 1f / Util.cube(x[n].volume());
                //n -> 1f / Util.sqr(x[n].volume());
                n -> 1f / x[n].volume();
        return x[Roulette.selectRouletteCached(x.length, curve, rng)];
    }

    @Nullable
    @Override
    protected Term introduce(Term input, Term selected) {


//        int vars = input.vars();
//        assert (vars < 127 - 1);
//        byte order = (byte) (vars + 1);


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
            for (int i = -1; i < pathLength - 1 /* dont include the selected target itself */; i++) {
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














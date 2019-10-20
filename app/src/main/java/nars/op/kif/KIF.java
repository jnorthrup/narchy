/*
 * Copyright (C) 2014 me
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:
 */
package nars.op.kif;

import com.google.common.collect.ForwardingSet;
import jcog.TODO;
import jcog.Util;
import jcog.WTF;
import jcog.data.set.ArrayHashSet;
import jcog.util.ArrayUtil;
import nars.$;
import nars.Narsese;
import nars.Op;
import nars.Task;
import nars.op.Equal;
import nars.op.MathFunc;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.theBool;
import nars.term.atom.theInt;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Set;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static nars.$.$$;
import static nars.Op.*;
import static nars.op.rdfowl.NQuadsRDF.equi;

/**
 * https:
 * http:
 * https:
 * https:
 * https:
 * <p>
 * holdsDuring(ImmediateFutureFn(WhenFn($1)),$2) is
 * when($1, ?W) & immediateFuture(?W,?P) & holdsDurrent(?P, $2)
 * <p>
 * https:
 **/
public class KIF implements Iterable<Task> {

    private static final Logger logger = LoggerFactory.getLogger(KIF.class);
    private static final Term SYMMETRIC_RELATION = $$("SymmetricRelation");
    private static final Term ASYMMETRIC_RELATION = $$("AsymmetricRelation");
    private static final boolean includePredArgCounts = false;
    private static final Set<Term> predExclusions = java.util.Set.of(

            $$("UnaryPredicate"),
            $$("BinaryPredicate"), $$("TernaryPredicate"),
            $$("QuaternaryPredicate"), $$("QuintaryPredicate"),

            $$("UnaryFunction"), $$("BinaryFunction"), $$("TernaryFunction"),
            $$("QuaternaryRelation"),
            $$("QuintaryRelation"),
            $$("SingleValuedRelation"), $$("TotalValuedRelation"),

            $$("AsymmetricRelation")
    );
    public final ArrayHashSet<Term> assertions = new ArrayHashSet<>();
    private final KIFParser kif;
    private final transient Map<Term, FnDef> fn = new HashMap<>();
    private final Set<Term> symmetricRelations = new HashSet<>();

    {
        symmetricRelations.add(SYMMETRIC_RELATION);
    }
    public KIF() {
        this.kif = new KIFParser();
    }

    public KIF(String s) {
        this();
        kif.parse(new StringReader(s));
        process();
    }

    public KIF(InputStream is) throws IOException {
        this();
        kif.read(is);
        process();
    }

    private static Term atomic(String sx) {
        sx = sx.replace("?", "#");
        try {
            return $.$(sx);
        } catch (Narsese.NarseseException e) {
            return $.quote(sx);
        }
    }

    private static UnaryOperator<Term> domainRangeMerger(Term type) {
        return (existing) -> {
            if (existing.equals(type))
                return existing;
            else
                return SETi.the(List.of(existing, type));
        };
    }

    public static KIF file(String filePath) throws IOException {
        return new KIF(new FileInputStream(filePath));
    }

    private static Term disjoint(List<Term> args, Term v0) {
        return Op.INH.the(
                DISJ(args.toArray(Op.EmptyTermArray)), v0
        ).neg();
    }

    public static Term impl(Term a, Term b, boolean implOrEquiv) {


        var tmp = IMPL.the(a, b);
        if (tmp.unneg().op() != IMPL) {
            logger.warn("un-impl: {} ==> {} ", a, b);
            return theBool.Null;
        }
        var negated = tmp.op() == NEG;

        tmp = tmp.unneg();
        a = tmp.sub(0);
        b = tmp.sub(1);

        MutableSet<Term> _aVars = new UnifiedSet<>();
        Set<Term> aVars = new VarOnlySet(_aVars);
        if (a instanceof Compound)
            ((Compound) a).recurseSubtermsToSet(Op.Variable, aVars, true);
        else if (a.op().var)
            aVars.add(a);
        MutableSet<Term> _bVars = new UnifiedSet<>();
        Set<Term> bVars = new VarOnlySet(_bVars);
        if (b instanceof Compound)
            ((Compound) b).recurseSubtermsToSet(Op.Variable, bVars, true);
        else if (b.op().var)
            bVars.add(b);

        Map<Term, Term> remap = new HashMap<>();

        var common = _aVars.intersect(_bVars);
        if (!common.isEmpty()) {
            common.forEach(t -> {
                var u = $.v(
                        Op.VAR_INDEP,


                        t.toString().substring(1));
                if (!t.equals(u) && !remap.containsKey(u))
                    remap.put(t, u);
            });
        }
        for (MutableSet<Term> ab : new MutableSet[]{_aVars, _bVars}) {
            for (var aa : ab) {
                if (aa.op() == VAR_INDEP && !common.contains(aa)) {
                    var str = aa.toString().substring(1);
                    var bb = $.v(VAR_DEP, str);
                    if (!remap.containsKey(bb))
                        remap.put(aa, bb);
                }
            }
        }

        if (!remap.isEmpty()) {
            a = a.replace(remap);
            if (a == null)
                throw new NullPointerException("transform failure");

            b = b.replace(remap);
            if (b == null)
                throw new NullPointerException("transform failure");
        }

        try {
            return
                    (implOrEquiv ?
                            IMPL.the(a, b) :
                            equi(a, b)).negIf(negated)
                    ;
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
        return null;
    }

    public Stream<Task> tasks() {
        return assertions.stream().map(Op::believe);
    }

    private void process() {
        var kb = KBmanager.manager.addKB("preprocess");

        for (var xx : kif.formulaMap.values()) {
            for (var x : FormulaPreprocessor.preProcess(xx, false, kb)) {
                try {
                    var y = formulaToTerm(x, 0);
                    if (y != null) {
                        assertions.add(y);
                    }
                } catch (Exception e) {
                    logger.error("{} {}", x, e.getMessage());
                    e.printStackTrace();
                }
            }
        }


        for (var entry : fn.entrySet()) {
            var f = entry.getKey();
            var s = entry.getValue();
            var ds = s.domain.isEmpty() ? 0 : s.domain.keySet().max();
            var vt = Util.map(0, ds, Term[]::new, i -> $.varDep(1 + i));
            Term v = null;
            if (s.range != null) {
                v = $.varDep("R");
                vt = ArrayUtil.add(vt, v);
            }
            int[] k = {1};
            var typeConds = Util.map(0, ds, Term[]::new, i ->
                    INH.the($.varDep(1 + i),
                            s.domain.getIfAbsent(1 + i, () -> $.varDep(k[0]++))));
            if (s.range != null) {
                typeConds = ArrayUtil.add(typeConds, INH.the(v, s.range));
            }
            var types = CONJ.the(
                    typeConds
            );
            var fxy = impl(INH.the($.p(vt), f), types, true);
            if (fxy != null) {
                if (fxy instanceof theBool) {
                    logger.error("bad function {} {} {}", f, s.domain, s.range);
                } else {
                    assertions.add(fxy);
                }
            }

        }

        if (symmetricRelations.size() > 1 /*SymmetricRelation exists in the set initially */) {

            assertions.removeIf(belief -> {
                if (belief.op() == INH) {
                    var fn = belief.sub(1);
                    if (symmetricRelations.contains(fn)) {

                        var ab = belief.sub(0);
                        if (ab.op() != PROD) {
                            return false;
                        }
                        assert (ab.subs() == 2);
                        var a = ab.sub(0);
                        var b = ab.sub(1);
                        var symmetric = INH.the(CONJ.the(PROD.the(a, b), PROD.the(b, a)), fn);

                        assertions.add(symmetric);
                        return true;
                    }
                }
                return false;
            });
        }

        assertions.removeIf(b -> {
            if (b.hasAny(BOOL)) {
                return true;
            }
            var bb = b.unneg().normalize();
            if (!Task.validTaskTerm(bb.unneg(), BELIEF, true)) {
                logger.error("invalid task target: {}\n\t{}", b, bb);
                return true;
            }
            return false;
        });
    }

    Term formulaToTerm(String sx, int level) {
        return formulaToTerm(new Formula(sx.replace("?", "#")), level);
    }

    private Term formulaToTerm(Formula x, int level) {
        Term result = null;


        var l = x.listLength();
        switch (l) {
            case -1:


                result = atomic(x.theFormula);
                break;
            case 1:
                result = $.p(formulaToTerm(x.getArgument(0), level + 1));
                break;
            default:
                if (l == 0) {
                    throw new WTF();
                }
                var sargs = IntStream.range(1, l).mapToObj(x::getArgument).collect(Collectors.toList());
                var args = sargs.stream().map(sarg -> formulaToTerm(sarg, level + 1)).collect(Collectors.toList());
                if (args.contains(null)) {
                    result = theBool.Null;
                } else if (args.isEmpty()) {
                    result = theBool.Null;
                } else { /**
                 *
                 *
                 * https:
                 * def special_link_type(predicate):
                 mapping = {
                 '=>':types.ImplicationLink,
                 '<=>':types.EquivalenceLink,
                 'and':types.AndLink,
                 'or':types.OrLink,
                 'not':types.NotLink,
                 'instance':types.MemberLink,
                 'attribute':types.InheritanceLink,
                 'member':types.MemberLink,
                 'subclass':types.InheritanceLink,
                 'exists':types.ExistsLink,
                 'forall':types.ForAllLink,
                 'causes':types.PredictiveImplicationLink
                 *
                 */var xCar = x.car();
                    var root = xCar;
                    Term y = null;
                    var includeDoc = false;
                    var finished = false;
                    switch (root) {
                        case "ListFn":
                            y = $.p(args);
                            break;


                        case "exhaustiveAttribute": {
                            y = INH.the(args.get(0), Op.SETi.the(args.subList(1, args.size())));
                            break;
                        }


                        case "subclass":
                        case "instance":
                        case "attribute":
                        case "subrelation":
                        case "subAttribute":

                            if (args.size() != 2) {
                                throw new RuntimeException("instance expects 2 arguments");
                            } else {
                                var pred = args.get(1);

                                var subj = args.get(0);

                                if (symmetricRelations.contains(pred)) {
                                    symmetricRelations.add(subj);
                                }

                                if (pred.equals(SYMMETRIC_RELATION) && !subj.hasVars()) {
                                    finished = true;
                                    break;
                                }

                                if (!includePredArgCounts && predExclusions.contains(pred)) {
                                    finished = true;
                                    break;
                                }

                                if ("instance".equals(root))
                                    y = $.inst(subj, pred);
                                else
                                    y = INH.the(subj, pred);

                                if (y instanceof theBool) {
                                    result = y;
                                    finished = true;
                                    break;
                                }
                            }

                            break;

                        case "relatedInternalConcept":
                            /*(documentation relatedInternalConcept EnglishLanguage "Means that the two arguments are related concepts within the SUMO, i.e. there is a significant similarity of meaning between them. To indicate a meaning relation between a SUMO concept and a concept from another source, use the Predicate relatedExternalConcept.")            */
                            var includeRelatedInternalConcept = true;
                            if (includeRelatedInternalConcept) {
                                if (args.size() != 2) {
                                    throw new UnsupportedOperationException("relatedInternalConcept expects 2 arguments");
                                } else {
                                    y = SIM.the(args.get(0), args.get(1));
                                }
                            }
                            break;

                        case "greaterThan":
                            if (args.size() == 2)
                                y = $.func("cmp", args.get(0), args.get(1), theInt.the(+1));
                            break;
                        case "lessThan":
                            if (args.size() == 2)
                                y = $.func("cmp", args.get(0), args.get(1), theInt.the(-1));
                            break;

                        case "equal":
                            y = Equal.the(args.get(0), args.get(1));
                            break;


                        case "<=>":

                            y = impl(args.get(0), args.get(1), false);
                            break;


                        case "forall":
                            var forVar = sargs.get(0);
                            if (forVar.startsWith("(")) {
                                forVar = forVar.substring(1, forVar.length() - 1);
                            }
                            var forVars = forVar.split(" ");
                            var missingAParamVar = Arrays.stream(forVars).anyMatch(vv -> !sargs.get(1).contains(vv));
                            if (!missingAParamVar) {
                                y = args.get(1);
                            } else {
                                y = impl(args.get(0), args.get(1), true);
                            }
                            break;
                        case "exists":
                            y = args.get(1);
                            break;
                        case "=>":
                            y = impl(args.get(0), args.get(1), true);
                            break;


                        case "cooccur":
                        case "during":
                            y = CONJ.the(args.get(0), 0, args.get(1));
                            break;


                        case "domain":

                            if (level == 0) {
                                if (args.size() >= 3) {
                                    var subj = (args.get(0));
                                    var arg = (args.get(1));
                                    var type = (args.get(2));
                                    if (type.equals(ASYMMETRIC_RELATION)) {
                                        finished = true;
                                        break;
                                    }
                                    var d = fn.computeIfAbsent(subj, (s) -> new FnDef());

                                    d.domain.updateValue(((theInt) arg).i, () -> type, (Function<? super Term, ? extends Term>) domainRangeMerger(type));

                                } else {
                                    throw new UnsupportedOperationException("unrecognized domain spec");
                                }
                                finished = true;
                                break;
                            }
                            break;
                        case "range":
                            if (level == 0) {
                                if (args.size() == 2) {
                                    var subj = args.get(0);
                                    var range = args.get(1);
                                    if (range.equals(ASYMMETRIC_RELATION)) {
                                        finished = true;
                                        break;
                                    }
                                    var d = fn.computeIfAbsent(subj, (s) -> new FnDef());
                                    d.range = range;
                                } else {
                                    throw new UnsupportedOperationException("unrecognized range spec");
                                }
                                finished = true;
                                break;
                            }
                            break;

                        case "AdditionFn":
                            if (args.size() == 2)
                                y = MathFunc.add(args.get(0), args.get(1));
                            else
                                throw new TODO();
                            break;

                        case "MultiplicationFn":
                            if (args.size() == 2)
                                y = MathFunc.mul(args.get(0), args.get(1));
                            else
                                throw new TODO();
                            break;


                        case "disjointDecomposition":
                        case "partition":
                        case "disjointRelation":
                        case "disjoint":
                        case "inverse":
                        case "contraryAttribute":


                            y = $.inh(VarAuto, SETe.the(args));


                            break;

                        case "comment":
                        case "documentation":
                        case "externalImage":
                            if (includeDoc) {
                                if (args.size() == 3) {
                                    var subj = args.get(0);
                                    var lang = args.get(1);
                                    Term desc = $.quote(args.get(2));
                                    try {
                                        y = INH.the($.p(subj, desc), lang);
                                    } catch (Exception e) {

                                        y = null;
                                    }
                                } else {
                                    throw new UnsupportedOperationException();
                                }
                            } else {
                                finished = true;
                                break;
                            }
                            break;

                        case "termFormat": {
                            if (includeDoc) {
                                var language = args.get(0).toString();
                                language = language.replace("Language", "");

                                var term = args.get(1);
                                var string = args.get(2);
                                y = INH.the($.p($.the(language), string), term);
                            } else {
                                finished = true;
                                break;
                            }
                            break;
                        }


                        default:

                            break;
                    }
                    if (!finished) {
                        if (y == null) {


                            var z = formulaToTerm(xCar, level + 1);

                            if (z != null) {
                                switch (z.toString()) {
                                    case "and":
                                        y = CONJ.the(args);
                                        break;
                                    case "or":
                                        y = DISJ(args.toArray(new Term[0]));
                                        break;
                                    case "not":
                                        y = args.get(0).neg();
                                        break;
                                    default:
                                        if (!z.op().var)
                                            y = INH.the($.p(args), z);
                                        else {
                                            args.add(0, z);
                                            y = $.p(args);
                                        }
                                        break;
                                }

                            }


                        }
                        if (y instanceof theBool) {
                            logger.warn("{} Bool singularity: args={}", x, args);
                            result = theBool.Null;
                        } else {
                            result = y;
                        }
                    }
                }
                break;
        }


        return result;
    }

    @Override
    public Iterator<Task> iterator() {
        return tasks().iterator();
    }


    static class FnDef {
        final IntObjectHashMap<Term> domain = new IntObjectHashMap<>();
        Term range;
    }


    private static class VarOnlySet extends ForwardingSet<Term> {
        private final MutableSet<Term> _aVars;

        VarOnlySet(MutableSet<Term> _aVars) {
            this._aVars = _aVars;
        }

        /**
         * HACK because recurseTermsToSet isnt designed to check only Op
         */
        @Override
        public boolean add(Term key) {
            return !key.op().var || super.add(key);
        }

        @Override
        protected java.util.Set<Term> delegate() {
            return _aVars;
        }
    }
}

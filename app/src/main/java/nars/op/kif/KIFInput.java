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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.op.kif;

import jcog.Util;
import jcog.WTF;
import nars.*;
import nars.task.CommandTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static nars.$.$$;
import static nars.Op.*;
import static nars.op.rdfowl.NQuadsRDF.equi;

/**
 * https://github.com/ontologyportal/sigmakee/blob/master/suo-kif.pdf
 * http://sigma.ontologyportal.org:8080/sigma/Browse.jsp?kb=SUMO&lang=EnglishLanguage&flang=SUO-KIF&term=subclass
 * https://raw.githubusercontent.com/ontologyportal/sumo/master/Merge.kif
 * https://raw.githubusercontent.com/ontologyportal/sumo/master/Mid-level-ontology.kif
 **/
public class KIFInput {

    private static final Logger logger = LoggerFactory.getLogger(KIFInput.class);
    private static final Term SYMMETRIC_RELATION = $$("SymmetricRelation");
    private static final Term ASYMMETRIC_RELATION = $$("AsymmetricRelation");

    public static Memory.BytesToTasks load = new Memory.BytesToTasks("kif") {

        @Override
        public Stream<Task> apply(InputStream i) {
            try {
                return new KIFInput(i).beliefs.stream().map(b ->
                        new CommandTask($.func(Op.BELIEF_TERM, b)));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    };



    private final SortedSet<Term> beliefs = new ConcurrentSkipListSet<>();
    private final KIF kif;
    private final boolean includeRelatedInternalConcept = true;
    private final boolean includeDoc = false;

    private transient final Map<Term, FnDef> fn = new HashMap();
    private final Set<Term> symmetricRelations = new HashSet();
    {
        symmetricRelations.add(SYMMETRIC_RELATION);
    }


    //usually these quantifiers semantics are inferrable from context.
    //adding them to the knowledge is at best redundant, at worst it starts
    //relating things which *should* remain related.
    //when this is input with varying degrees of confidence
    //then such statements could just be assigned lower confidence.
    private static final boolean includePredArgCounts = false;
    private static final Set<Term> predExclusions = Set.of(
            //Object?
            $$("UnaryPredicate"),
            $$("BinaryPredicate"),$$("TernaryPredicate"),
            $$("QuaternaryPredicate"), $$("QuintaryPredicate"),

            $$("UnaryFunction"), $$("BinaryFunction"), $$("TernaryFunction"),
            $$("QuaternaryRelation"),
            $$("QuintaryRelation"),
            $$("SingleValuedRelation"),$$("TotalValuedRelation"),

            $$("AsymmetricRelation") //product relation is by implicitly asymmetric

            //etc
    );

    KIFInput(InputStream is) throws Exception {
        this.kif = new KIF();
        kif.read(is);

        KB kb = KBmanager.getMgr().addKB("preprocess");

        FormulaPreprocessor fpp = new FormulaPreprocessor();
        kif.formulaMap.values().forEach(_x -> {

            ArrayList<Formula> xx = fpp.preProcess(_x, false, kb);
//            if (xx.size() > 1) {
//                System.out.println("expand " + _x + "to:\n" + xx);
//            }
            xx.forEach(x -> {
                try {
                    Term y = formulaToTerm(x, 0);
                    if (y != null) {
                        beliefs.add(y);
                    }
                } catch (Exception e) {
                    logger.error("{} {}", x, e.getMessage());
                    e.printStackTrace();
                }
            });

            //  => Implies
            //  <=> Equivalance
            /*Unknown operators: {=>=466, rangeSubclass=5, inverse=1, relatedInternalConcept=7, documentation=128, range=29, exhaustiveAttribute=1, trichotomizingOn=4, subrelation=22, not=2, partition=12, contraryAttribute=1, subAttribute=2, disjoint=5, domain=102, disjointDecomposition=2, domainSubclass=9, <=>=70}*/
        });

        //apply domain and range predicates
        fn.forEach((f, s) -> {
            int ds = s.domain.isEmpty() ? 0 : s.domain.keySet().max();
            Term[] vt = Util.map(0, ds, i -> $.varDep(1 + i), Term[]::new);
            Term v = null;
            if (s.range != null) {
                v = $.varDep("R");
                vt = ArrayUtils.add(vt, v);
            }
            final int[] k = {1};
            Term[] typeConds = Util.map(0, ds, i ->
                    INH.the($.varDep(1 + i),
                            s.domain.getIfAbsent(1 + i, () -> $.varDep(k[0]++))), Term[]::new);
            if (s.range != null) {
                typeConds = ArrayUtils.add(typeConds, INH.the(v, s.range));
            }
            Term types = CONJ.the(
                    typeConds
            );
            Term fxy = impl(INH.the($.p(vt), f), types, true);
            if (fxy != null) {
                if (fxy instanceof Bool) {
                    logger.error("bad function {} {} {}", f, s.domain, s.range);
                } else {
                    beliefs.add(fxy);
                }
            }

        });

        if (symmetricRelations.size()>1 /*SymmetricRelation exists in the set initially */) {
            //rewrite symmetric relations
            beliefs.removeIf(belief -> {
                if (belief.op() == INH) {
                    Term fn = belief.sub(1);
                    if (symmetricRelations.contains(fn)) {
                        //Term symmetric = INH.the(PROD.the(SETe.the(b.sub(0).subterms())), fn);
                        Term ab = belief.sub(0);
                        if (ab.op() != PROD) {
                            return false; //ie  inheritance involving symmetric relation
                        }
                        assert(ab.subs()==2);
                        Term a = ab.sub(0);
                        Term b = ab.sub(1);
                        Term symmetric = INH.the(SECTe.the(PROD.the(a, b), PROD.the(b, a)), fn);
                        //System.out.println(belief + " -> " + symmetric);
                        beliefs.add(symmetric);
                        return true; //remove original
                    }
                }
                return false;
            });
        }

        beliefs.removeIf(b -> {
            if (b.hasAny(BOOL)) {
                return true;
            }
            Term bb = b.unneg().normalize();
            if (!Task.validTaskTerm(bb, BELIEF, true)) {
                logger.error("invalid task term: {}\n\t{}", b, bb);
                return true;
            }
            return false;
        });
        //nar.input( beliefs.stream().map(x -> task(x)) );

//        long[] stamp = { new Random().nextLong() };


        //nar.believe(y);
    }

    private static Term atomic(String sx) {
        sx = sx.replace("?", "#"); //query var to depvar HACK
        try {
            return $.$(sx);
        } catch (Narsese.NarseseException e) {
            return $.quote(sx);
        }
    }

    private static Function<Term, Term> domainRangeMerger(Term type) {
        return (existing) -> {
            if (existing.equals(type))
                return existing;
            else
                return SETi.the(List.of(existing, type));
        };
    }



    private Term formulaToTerm(String sx, int level) {
        sx = sx.replace("?", "#"); //query var to depvar HACK

        Formula f = new Formula(sx);

        Term g = formulaToTerm(f, level);

        return g;
//        else {
//            return atomic(sx);
//        }
    }

    private Term formulaToTerm(final Formula x, int level) {

        String xCar = x.car();
        String root = xCar; //root operate

        int l = x.listLength();
        if (l == -1)
            return atomic(x.theFormula);
        else if (l == 1) {
            return $.p(formulaToTerm(x.getArgument(0), level+1));
        } else if (l == 0) {
            throw new WTF();
        }

        List<String> sargs = IntStream.range(1, l).mapToObj(x::getArgument).collect(Collectors.toList());
        List<Term> args = sargs.stream().map((z) -> formulaToTerm(z, level + 1)).collect(Collectors.toList());

        if (args.contains(null)) {
            //throw new NullPointerException("in: " + args);
            return Null;
        }

        if (args.isEmpty())
            return Null;
        //assert (!args.isEmpty()); //should have been handled first

        /**
         *
         *
         * https://github.com/opencog/opencog/blob/04db8e557a2d67da9025fe455095d2cda0261ea7/opencog/python/sumo/sumo.py
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
         */

        Term y = null;
        switch (root) {
            case "ListFn":
                y = $.p(args);
                break;


            case "exhaustiveAttribute": {
                //ex: (exhaustiveAttribute RiskAttribute HighRisk LowRisk)
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
                        Term pred = args.get(1);

                        Term subj = args.get(0);

                        if (symmetricRelations.contains(pred)) {
                            symmetricRelations.add(subj);
                        }

                        if (pred.equals(SYMMETRIC_RELATION)) {
                            return null; //direct subclass of symmetric relation, this is what we make implicit so we dont need this belief
                        }

                        if (!includePredArgCounts && predExclusions.contains(pred))
                            return null;

                        if (root.equals("instance"))
                            y = $.inst(subj, pred);
                        else
                            y = INH.the(subj, pred);

                        if (y instanceof Bool)
                            return y;
                    }

                break;

            case "relatedInternalConcept":
                /*(documentation relatedInternalConcept EnglishLanguage "Means that the two arguments are related concepts within the SUMO, i.e. there is a significant similarity of meaning between them. To indicate a meaning relation between a SUMO concept and a concept from another source, use the Predicate relatedExternalConcept.")            */
                if (includeRelatedInternalConcept) {
                    if (args.size() != 2) {
                        throw new UnsupportedOperationException("relatedInternalConcept expects 2 arguments");
                    } else {
                        y = SIM.the(args.get(0), args.get(1));
                    }
                }
                break;

            case "equal":
                if (!(args.get(0).hasVars() || args.get(1).hasVars())) {
                    //write in impl form
                    y = impl(args.get(0), args.get(1), false);
                } else {
                    //input as-is
                }
                //y = $.func("isEqual", args.get(0), args.get(1)); //"equal" is NARchy built-in
                //y = $.sim(args.get(0), args.get(1));
                break;


            case "forall":
                String forVar = sargs.get(0);
                if (forVar.startsWith("(")) {
                    forVar = forVar.substring(1, forVar.length() - 1); //remove parens
                }
                boolean missingAParamVar = false;
                String[] forVars = forVar.split(" ");
                for (String vv : forVars) {
                    if (!sargs.get(1).contains(vv)) {
                        missingAParamVar = true;
                        break;
                    }
                }
                if (!missingAParamVar) {
                    y = args.get(1); //skip over the for variables since it is contained in the expression
                } else {
                    y = impl(args.get(0), args.get(1), true);
                }
                break;
            case "exists":
                y = args.get(1); //skip over the first parameter, since depvar is inherently existential
                break;
            case "=>":
                y = impl(args.get(0), args.get(1), true);
                break;
            case "<=>":
                y = impl(args.get(0), args.get(1), false);
                break;


            case "causes":
                y = IMPL.the(args.get(0), 1, args.get(1));
                break;

            case "cooccur":
            case "during":
                y = CONJ.the(args.get(0), 0, args.get(1));
                break;
            case "meetsTemporally":
                y = CONJ.the(args.get(0), +1, args.get(1));
                break;

            case "domain":
                //TODO use the same format as Range, converting quantity > 1 to repeats in an argument list
                if (level == 0) {
                    if (args.size() >= 3) {
                        Term subj = (args.get(0));
                        Term arg = (args.get(1));
                        Term type = (args.get(2));
                        if (type.equals(ASYMMETRIC_RELATION)) {
                            return null; //obvious
                        }
                        FnDef d = fn.computeIfAbsent(subj, (s) -> new FnDef());

                        d.domain.updateValue(((Int) arg).id, () -> type, domainRangeMerger(type));
                        //assert (existing == null || existing.equals(type)): x + ": " + type + "!=" + existing;
                    } else {
                        throw new UnsupportedOperationException("unrecognized domain spec");
                    }
                    return null;
                }
                break;
            case "range":
                if (level == 0) {
                    if (args.size() == 2) {
                        Term subj = args.get(0);
                        Term range = args.get(1);
                        if (range.equals(ASYMMETRIC_RELATION)) {
                            return null; //obvious
                        }
                        FnDef d = fn.computeIfAbsent(subj, (s) -> new FnDef());
                        d.range = range;
                    } else {
                        throw new UnsupportedOperationException("unrecognized range spec");
                    }
                    return null;
                }
                break;

            case "partition":
            case "disjointDecomposition":
                if (args.size() > 2) {
                    y = disjoint(args.subList(1, args.size()), args.get(0));
                }
                break;
            case "disjointRelation":
            case "disjoint":
            case "inverse":
            case "contraryAttribute":
                //like n-ary disjoint
                Variable v0 = $.varDep(1);
                y = disjoint(args, v0);

                break;

            case "comment":
            case "documentation":
            case "externalImage":
                if (includeDoc) {
                    if (args.size() == 3) {
                        Term subj = args.get(0);
                        Term lang = args.get(1);
                        Term desc = $.quote(args.get(2));
                        try {
                            y = INH.the($.p(subj, desc), lang);
                        } catch (Exception e) {
                            //e.printStackTrace();
                            y = null;
                        }
                    } else {
                        throw new UnsupportedOperationException();
                    }
                } else {
                    return null;
                }
                break;

            case "termFormat": {
                if (includeDoc) {
                    String language = args.get(0).toString();
                    language = language.replace("Language", "");

                    Term term = args.get(1);
                    Term string = args.get(2);
                    y = INH.the($.p($.the(language), string), term);
                } else {
                    return null;
                }
                break;
            }


            default:
                //System.out.println("unknown: " + x);
                break;
        }

        if (y == null) {



            Term z = formulaToTerm(xCar, level + 1);

            if (z != null) {
                switch (z.toString()) {
                    case "and":
                        y = CONJ.the(args);
                        break;
                    case "or":
                        y = $.disj(args.toArray(new Term[0]));
                        break;
                    case "not":
                        y = args.get(0).neg();
                        break;
                    default:
                        if (!z.op().var)
                            y = INH.the($.p(args), z); //HACK
                        else {
                            args.add(0, z); //re-attach
                            y = $.p(args);
                        }
                        break;
                }

            }


        }

        if (y instanceof Bool) {
            logger.warn("{} Bool singularity: args={}",x, args);
            return Null;
        }

        return y;
    }

    private Term disjoint(List<Term> args, Term v0) {
        Term y;
        y = Op.INH.the(
                v0,
                Op.SECTe.the(args.toArray(Op.EmptyTermArray))
        ).neg();
        return y;
    }

//    private Variable nextVar(Op v) {
//        return $.v(v, nextVar());
//    }

//    private final AtomicInteger serial = new AtomicInteger(0);

//    private String nextVar() {
//        return Integer.toString(Math.abs(serial.incrementAndGet()), 36);
//    }

    //public final Set<Twin<Term>> impl = new HashSet();

    public static Term impl(Term a, Term b, boolean implOrEquiv) {

        //reduce as implication first
        Term tmp = IMPL.the(a, b);
        if (tmp.unneg().op() != IMPL) {
            logger.warn("un-impl: {} ==> {} ", a, b);
            return Null;
        }
        boolean negated = tmp.op()==NEG;

        tmp = tmp.unneg();
        a = tmp.sub(0);
        b = tmp.sub(1);

        MutableSet<Term> aVars = new VarOnlySet();
        if (a instanceof Compound)
            ((Compound) a).recurseTermsToSet(Op.VariableBits, aVars, true);
        else if (a.op().var)
            aVars.add(a);
        MutableSet<Term> bVars = new VarOnlySet();
        if (b instanceof Compound)
            ((Compound) b).recurseTermsToSet(Op.VariableBits, bVars, true);
        else if (b.op().var)
            bVars.add(b);

        Map<Term, Term> remap = new HashMap();

        MutableSet<Term> common = aVars.intersect(bVars);
        if (!common.isEmpty()) {
            common.forEach(t -> {
                Variable u = $.v(
                        Op.VAR_INDEP,
                        //Op.VAR_QUERY,
                        //Op.VAR_PATTERN,
                        t.toString().substring(1));
                if (!t.equals(u) && !remap.containsKey(u))
                    remap.put(t, u);
            });
        }
        for (MutableSet<Term> ab : new MutableSet[]{aVars, bVars}) {
            ab.forEach(aa -> {
                if (aa.op() == VAR_INDEP && !common.contains(aa)) {
                    String str = aa.toString().substring(1);
//                    if (str.equals("0"))//HACK avoid writing: #0
//                        str = "#0_";

                    Variable bb = $.v(Op.VAR_DEP, str);
                    if (!remap.containsKey(bb))
                        remap.put(aa, bb);
                }
            });
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
//            impl.add(Tuples.twin(conditionTerm, actionTerm));
//            if (!implOrEquiv) {
//                impl.add(Tuples.twin(actionTerm, conditionTerm)); //reverse
//            }

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


    static class FnDef {
        final IntObjectHashMap<Term> domain = new IntObjectHashMap();
        Term range;
    }

    /**
     * HACK because recurseTermsToSet isnt designed to check only Op
     */
    private static class VarOnlySet extends UnifiedSet {
        @Override
        public boolean add(Object key) { //HACK
            if (!((Term) key).op().var)
                return true;
            return super.add(key);
        }
    }
}

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

import jcog.Util;
import jcog.WTF;
import jcog.util.ArrayUtils;
import nars.*;
import nars.task.CommandTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Set;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
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
                        new CommandTask($.func(Op.Belief, b)));
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


    
    
    
    
    
    private static final boolean includePredArgCounts = false;
    private static final Set<Term> predExclusions = java.util.Set.of(
            
            $$("UnaryPredicate"),
            $$("BinaryPredicate"),$$("TernaryPredicate"),
            $$("QuaternaryPredicate"), $$("QuintaryPredicate"),

            $$("UnaryFunction"), $$("BinaryFunction"), $$("TernaryFunction"),
            $$("QuaternaryRelation"),
            $$("QuintaryRelation"),
            $$("SingleValuedRelation"),$$("TotalValuedRelation"),

            $$("AsymmetricRelation") 

            
    );

    KIFInput(InputStream is) throws Exception {
        this.kif = new KIF();
        kif.read(is);

        KB kb = KBmanager.getMgr().addKB("preprocess");

        FormulaPreprocessor fpp = new FormulaPreprocessor();
        kif.formulaMap.values().forEach(_x -> {

            ArrayList<Formula> xx = fpp.preProcess(_x, false, kb);



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

            
            
            /*Unknown operators: {=>=466, rangeSubclass=5, inverse=1, relatedInternalConcept=7, documentation=128, range=29, exhaustiveAttribute=1, trichotomizingOn=4, subrelation=22, not=2, partition=12, contraryAttribute=1, subAttribute=2, disjoint=5, domain=102, disjointDecomposition=2, domainSubclass=9, <=>=70}*/
        });

        
        fn.forEach((f, s) -> {
            int ds = s.domain.isEmpty() ? 0 : s.domain.keySet().max();
            Term[] vt = Util.map(0, ds, Term[]::new, i -> $.varDep(1 + i));
            Term v = null;
            if (s.range != null) {
                v = $.varDep("R");
                vt = ArrayUtils.add(vt, v);
            }
            final int[] k = {1};
            Term[] typeConds = Util.map(0, ds, Term[]::new, i ->
                    INH.the($.varDep(1 + i),
                            s.domain.getIfAbsent(1 + i, () -> $.varDep(k[0]++))));
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
            
            beliefs.removeIf(belief -> {
                if (belief.op() == INH) {
                    Term fn = belief.sub(1);
                    if (symmetricRelations.contains(fn)) {
                        
                        Term ab = belief.sub(0);
                        if (ab.op() != PROD) {
                            return false; 
                        }
                        assert(ab.subs()==2);
                        Term a = ab.sub(0);
                        Term b = ab.sub(1);
                        Term symmetric = INH.the(SECTe.the(PROD.the(a, b), PROD.the(b, a)), fn);
                        
                        beliefs.add(symmetric);
                        return true; 
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
            if (!Task.taskConceptTerm(bb, BELIEF, true)) {
                logger.error("invalid task term: {}\n\t{}", b, bb);
                return true;
            }
            return false;
        });
        




        
    }

    private static Term atomic(String sx) {
        sx = sx.replace("?", "#"); 
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
        sx = sx.replace("?", "#"); 

        Formula f = new Formula(sx);

        Term g = formulaToTerm(f, level);

        return g;



    }

    private Term formulaToTerm(final Formula x, int level) {

        String xCar = x.car();
        String root = xCar; 

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
            
            return Bool.Null;
        }

        if (args.isEmpty())
            return Bool.Null;
        

        /**
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
         */

        Term y = null;
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
                        Term pred = args.get(1);

                        Term subj = args.get(0);

                        if (symmetricRelations.contains(pred)) {
                            symmetricRelations.add(subj);
                        }

                        if (pred.equals(SYMMETRIC_RELATION)) {
                            return null; 
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

            case "greaterThan":
                if (args.size()==2)
                    y = $.func("cmp", args.get(0), args.get(1), Int.the(+1));
                break;
            case "lessThan":
                if (args.size()==2)
                    y = $.func("cmp", args.get(0), args.get(1), Int.the(-1));
                break;

            case "equal":
                if (!(args.get(0).hasVars() || args.get(1).hasVars())) {
                    
                    //y = impl(args.get(0), args.get(1), false);
                    y = SIM.the(args.get(0), args.get(1));
                } else {

                }
                
                
                break;


            case "forall":
                String forVar = sargs.get(0);
                if (forVar.startsWith("(")) {
                    forVar = forVar.substring(1, forVar.length() - 1); 
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
            case "<=>":
                y = impl(args.get(0), args.get(1), false);
                break;


//            case "causes":
//                y = IMPL.the(args.get(0), 1, args.get(1));
//                break;

            case "cooccur":
            case "during":
                y = CONJ.the(args.get(0), 0, args.get(1));
                break;
//            case "meetsTemporally":
//                y = CONJ.the(args.get(0), +1, args.get(1));
//                break;

            case "domain":
                
                if (level == 0) {
                    if (args.size() >= 3) {
                        Term subj = (args.get(0));
                        Term arg = (args.get(1));
                        Term type = (args.get(2));
                        if (type.equals(ASYMMETRIC_RELATION)) {
                            return null; 
                        }
                        FnDef d = fn.computeIfAbsent(subj, (s) -> new FnDef());

                        d.domain.updateValue(((Int) arg).id, () -> type, domainRangeMerger(type));
                        
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
                            return null; 
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
                            y = INH.the($.p(args), z); 
                        else {
                            args.add(0, z); 
                            y = $.p(args);
                        }
                        break;
                }

            }


        }

        if (y instanceof Bool) {
            logger.warn("{} Bool singularity: args={}",x, args);
            return Bool.Null;
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











    

    public static Term impl(Term a, Term b, boolean implOrEquiv) {

        
        Term tmp = IMPL.the(a, b);
        if (tmp.unneg().op() != IMPL) {
            //IMPL.the(a, b); //TEMPORARY
            logger.warn("un-impl: {} ==> {} ", a, b);
            return Bool.Null;
        }
        boolean negated = tmp.op()==NEG;

        tmp = tmp.unneg();
        a = tmp.sub(0);
        b = tmp.sub(1);

        MutableSet<Term> aVars = new VarOnlySet();
        if (a instanceof Compound)
            ((Compound) a).recurseSubtermsToSet(Op.Variable, aVars, true);
        else if (a.op().var)
            aVars.add(a);
        MutableSet<Term> bVars = new VarOnlySet();
        if (b instanceof Compound)
            ((Compound) b).recurseSubtermsToSet(Op.Variable, bVars, true);
        else if (b.op().var)
            bVars.add(b);

        Map<Term, Term> remap = new HashMap();

        MutableSet<Term> common = aVars.intersect(bVars);
        if (!common.isEmpty()) {
            common.forEach(t -> {
                Variable u = $.v(
                        Op.VAR_INDEP,
                        
                        
                        t.toString().substring(1));
                if (!t.equals(u) && !remap.containsKey(u))
                    remap.put(t, u);
            });
        }
        for (MutableSet<Term> ab : new MutableSet[]{aVars, bVars}) {
            ab.forEach(aa -> {
                if (aa.op() == VAR_INDEP && !common.contains(aa)) {
                    String str = aa.toString().substring(1);



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
        public boolean add(Object key) { 
            if (!((Term) key).op().var)
                return true;
            return super.add(key);
        }
    }
}

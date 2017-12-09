package nars.derive;

import jcog.list.FasterList;
import jcog.tree.perfect.TrieNode;
import nars.$;
import nars.Op;
import nars.control.Derivation;
import nars.derive.constraint.MatchConstraint;
import nars.derive.op.AbstractPatternOp;
import nars.derive.rule.PremiseRuleSet;
import nars.term.Term;
import nars.util.TermTrie;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.roaringbitmap.RoaringBitmap;

import java.util.*;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * predicate trie, for maximum folding
 * TODO generify this beyond Derivation state
 */
public final class PrediTrie {

    final TermTrie<PrediTerm<Derivation>, PrediTerm<Derivation>> pre;
    final FasterList<ValueFork> postChoices = new FasterList();

    public PrediTrie(PremiseRuleSet r) {

        pre = new TermTrie<>();

        Map<Set<Term>, RoaringBitmap> pre = new HashMap<>(r.size());
        List<PrediTerm<Derivation>> conclusions = $.newArrayList(r.size() * 4);

        ObjectIntHashMap<Term> preconditionCount = new ObjectIntHashMap(256);

        r.forEach(rule -> {

            assert (rule.POST != null) : "null POSTconditions:" + rule;

            for (PostCondition p : rule.POST) {

                Pair<Set<Term>, PrediTerm<Derivation>> c = rule.build(p);

                c.getOne().forEach((k) -> preconditionCount.addToValue(k, 1));

                int id = conclusions.size();
                conclusions.add(c.getTwo());

                pre.computeIfAbsent(c.getOne(), (x) -> new RoaringBitmap()).add(id);


            }
        });

//            System.out.println("PRECOND");
//            preconditionCount.keyValuesView().toSortedListBy((x)->x.getTwo()).forEach((x)->System.out.println(Texts.iPad(x.getTwo(),3) + "\t" + x.getOne() ));


        Comparator<PrediTerm<?>> sort = PrediTerm.sort(preconditionCount::get);

        List<List<Term>> paths = $.newArrayList();
        pre.forEach((k, v) -> {

            FasterList<PrediTerm<Derivation>> path = new FasterList(k);
            path.sort(sort);

            PrediTerm<Derivation>[] ll = StreamSupport.stream(v.spliterator(), false).map((i) -> conclusions.get(i).transform((Function) null)).toArray(PrediTerm[]::new);
            assert (ll.length != 0);

            ValueFork cx = ValueFork.the(ll, postChoices, v);
            path.add(cx.valueBranch);
            this.pre.put(path, cx);
        });

    }


    public static PrediTerm<Derivation> the(PremiseRuleSet r, Function<PrediTerm<Derivation>, PrediTerm<Derivation>> each) {
        PrediTrie t = new PrediTrie(r);

        FasterList<ValueFork> pc = t.postChoices;
        for (int i = 0, postChoices1Size = pc.size(); i < postChoices1Size; i++) {
            ValueFork x = pc.get(i);
            PrediTerm<Derivation>[] branches = x.branches;
            for (int j = 0, branchesLength = branches.length; j < branchesLength; j++) {
                PrediTerm xx = branches[j];
                if (xx instanceof AndCondition) {
                    PrediTerm yy = ((AndCondition) xx).transform((y) -> {
                        if (y instanceof AndCondition) {
                            return MatchConstraint.combineConstraints((AndCondition)y);
                        }
                        return y;
                    }, (sub) -> sub);
                    branches[j] = yy;
                }
            }

        }
        return AndCondition.the(
                PrediTrie.compile(t.pre, each),
                new Try(t.postChoices.toArrayRecycled(ValueFork[]::new)));

    }


    public PrediTerm<Derivation> compile(Function<PrediTerm<Derivation>, PrediTerm<Derivation>> each) {
        return compile(pre, each);
    }

    public static PrediTerm<Derivation> compile(TermTrie<PrediTerm<Derivation>, PrediTerm<Derivation>> trie, Function<PrediTerm<Derivation>, PrediTerm<Derivation>> each) {
        List<PrediTerm<Derivation>> bb = compile(trie.root);
        PrediTerm[] roots = bb.toArray(new PrediTerm[bb.size()]);

        PrediTerm<Derivation> tf = Fork.fork(roots);
        if (each != null)
            tf = tf.transform(each);

        return tf;
    }


    static List<PrediTerm<Derivation>> compile(@NotNull TrieNode<List<PrediTerm<Derivation>>, PrediTerm<Derivation>> node) {


        List<PrediTerm<Derivation>> bb = $.newArrayList(node.childCount());
//        assert(node.getKey()!=null);
//        assert(node.getValue()!=null);

        node.forEach(n -> {

            List<PrediTerm<Derivation>> conseq = compile(n);

            int nStart = n.start();
            int nEnd = n.end();
            PrediTerm<Derivation> branch = TrieDeriver.ifThen(
                    TrieDeriver.conditions(n.seq().stream().skip(nStart).limit(nEnd - nStart)),
                    !conseq.isEmpty() ? (PrediTerm<Derivation>) Fork.fork(conseq.toArray(new PrediTerm[conseq.size()])) : null
            );

            if (branch != null)
                bb.add(branch);
        });

        return compileSwitch(bb);
    }

    protected static List<PrediTerm<Derivation>> compileSwitch(List<PrediTerm<Derivation>> bb) {

        bb = factorSubOpToSwitch(bb, 0, 2);
        bb = factorSubOpToSwitch(bb, 1, 2);

        return bb;
    }

    @NotNull
    private static List<PrediTerm<Derivation>> factorSubOpToSwitch(@NotNull List<PrediTerm<Derivation>> bb, int subterm, int minToCreateSwitch) {
        if (!bb.isEmpty()) {
            Map<AbstractPatternOp.PatternOp, PrediTerm<Derivation>> cases = $.newHashMap(8);
            List<PrediTerm<Derivation>> removed = $.newArrayList(); //in order to undo
            bb.removeIf(p -> {
                if (p instanceof AndCondition) {
                    AndCondition ac = (AndCondition) p;
                    return ac.OR(x -> {
                        if (x instanceof AbstractPatternOp.PatternOp) {
                            AbstractPatternOp.PatternOp so = (AbstractPatternOp.PatternOp) x;
                            if (so.taskOrBelief == subterm) {
                                PrediTerm acw = ac.without(so);
                                if (null == cases.putIfAbsent(so, acw)) {
                                    removed.add(p);
                                    return true;
                                }
                            }
                        }
                        return false;
                    });

                }
                return false;
            });


            int numCases = cases.size();
            if (numCases >= minToCreateSwitch) {
                if (numCases != removed.size()) {
                    throw new RuntimeException("switch fault");
                }

                EnumMap<Op, PrediTerm<Derivation>> caseMap = new EnumMap(Op.class);
                cases.forEach((c, p) -> caseMap.put(Op.values()[c.opOrdinal], p));
                bb.add(new OpSwitch(subterm, caseMap));
            } else {
                bb.addAll(removed); //undo
            }
        }

        return bb;
    }


}

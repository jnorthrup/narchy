package nars.derive;

import jcog.list.FasterList;
import jcog.tree.perfect.TrieNode;
import nars.$;
import nars.Op;
import nars.control.Derivation;
import nars.control.ProtoDerivation;
import nars.derive.constraint.MatchConstraint;
import nars.derive.op.TaskBeliefOp;
import nars.derive.rule.PremiseRuleSet;
import nars.term.Term;
import nars.term.pred.AndCondition;
import nars.term.pred.Fork;
import nars.term.pred.PrediTerm;
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

    final TermTrie<PrediTerm<Derivation>, PrediTerm<Derivation>> path;
    final FasterList<ValueFork> postChoices = new FasterList();

    public PrediTrie(PremiseRuleSet r) {

        path = new TermTrie<>();

        Map<Set<PrediTerm<ProtoDerivation>>, RoaringBitmap> post = new HashMap<>(r.size());
        List<PrediTerm<Derivation>> conclusions = $.newArrayList(r.size() * 4);

        ObjectIntHashMap<Term> preconditionCount = new ObjectIntHashMap(256);

        r.forEach(rule -> {

            assert (rule.POST != null) : "null POSTconditions:" + rule;

            for (PostCondition p : rule.POST) {

                Pair<Set<PrediTerm<ProtoDerivation>>, PrediTerm<Derivation>> c = rule.build(p);

                c.getOne().forEach((k) -> preconditionCount.addToValue(k, 1));

                int id = conclusions.size();
                conclusions.add(c.getTwo());

                post.computeIfAbsent(c.getOne(), (x) -> new RoaringBitmap()).add(id);


            }
        });

//            System.out.println("PRECOND");
//            preconditionCount.keyValuesView().toSortedListBy((x)->x.getTwo()).forEach((x)->System.out.println(Texts.iPad(x.getTwo(),3) + "\t" + x.getOne() ));


        Comparator sort = PrediTerm.sort(preconditionCount::get);

        List<List<Term>> paths = $.newArrayList();
        post.forEach((k, v) -> {

            FasterList<PrediTerm<Derivation>> path = new FasterList(k);
            path.sort(sort);

            PrediTerm<Derivation>[] ll = StreamSupport.stream(v.spliterator(), false)
                    .map((i) -> conclusions.get(i).transform((Function) null)).toArray(PrediTerm[]::new);
            assert (ll.length != 0);

            ValueFork cx = ValueFork.the(ll, postChoices, v);
            path.add(cx.valueBranch);
            this.path.put(path, cx);
        });

    }


    public static DeriverRoot the(PremiseRuleSet r, Function<PrediTerm<Derivation>, PrediTerm<Derivation>> each) {
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
                            return MatchConstraint.combineConstraints((AndCondition) y);
                        }
                        return y;
                    }, (sub) -> sub);
                    branches[j] = yy;
                }
            }

        }
        return new DeriverRoot(//AndCondition.the(
                PrediTrie.compile(t.path, each),
                new Try(t.postChoices.toArrayRecycled(ValueFork[]::new)));
    }


    public PrediTerm<Derivation> compile(Function<PrediTerm<Derivation>, PrediTerm<Derivation>> each) {
        return compile(path, each);
    }

    static <D extends ProtoDerivation> PrediTerm<D> compile(TermTrie<PrediTerm<D>, PrediTerm<D>> trie, Function<PrediTerm<D>, PrediTerm<D>> each) {
        List<PrediTerm<D>> bb = compile(trie.root);
        PrediTerm<D>[] roots = bb.toArray(new PrediTerm[bb.size()]);

        PrediTerm tf = Fork.fork(roots, x -> new ForkDerivation(x));
        if (each != null)
            tf = tf.transform(each);

        return tf;
    }


    static <D extends ProtoDerivation> List<PrediTerm<D>> compile(TrieNode<List<PrediTerm<D>>, PrediTerm<D>> node) {


        List<PrediTerm<D>> bb = $.newArrayList(node.childCount());
//        assert(node.getKey()!=null);
//        assert(node.getValue()!=null);

        node.forEach(n -> {

            var conseq = compile(n);

            int nStart = n.start();
            int nEnd = n.end();
            PrediTerm<D> branch = PrediTerm.ifThen(
                    n.seq().stream().skip(nStart).limit(nEnd - nStart),
                    !conseq.isEmpty() ?
                            Fork.fork(conseq.toArray(new PrediTerm[conseq.size()]), x -> new ForkDerivation(x))
                            : null
            );

            if (branch != null)
                bb.add(branch);
        });

        return compileSwitch(bb);
    }

    protected static <D extends ProtoDerivation> List<PrediTerm<D>> compileSwitch(List<PrediTerm<D>> bb) {

        bb = factorSubOpToSwitch(bb, true, 2);
        bb = factorSubOpToSwitch(bb, false, 2);

        return bb;
    }

    @NotNull
    private static <D extends ProtoDerivation> List<PrediTerm<D>> factorSubOpToSwitch(List<PrediTerm<D>> bb, boolean taskOrBelief, int minToCreateSwitch) {
        if (!bb.isEmpty()) {
            Map<TaskBeliefOp, PrediTerm<D>> cases = $.newHashMap(8);
            List<PrediTerm<D>> removed = $.newArrayList(); //in order to undo
            bb.removeIf(p -> {
                if (p instanceof AndCondition) {
                    AndCondition ac = (AndCondition) p;
                    return ac.OR(x -> {
                        if (x instanceof TaskBeliefOp) {
                            TaskBeliefOp so = (TaskBeliefOp) x;
                            if (so.task == taskOrBelief && so.belief == !taskOrBelief) {
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

                EnumMap<Op, PrediTerm<D>> caseMap = new EnumMap(Op.class);
                cases.forEach((c, p) -> caseMap.put(Op.values()[c.op], p));
                bb.add(new OpSwitch<D>(taskOrBelief, caseMap));
            } else {
                bb.addAll(removed); //undo
            }
        }

        return bb;
    }


}

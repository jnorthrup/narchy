package nars.derive.premise;

import jcog.Util;
import jcog.list.FasterList;
import jcog.tree.perfect.TrieNode;
import nars.$;
import nars.Op;
import nars.control.Cause;
import nars.derive.Derivation;
import nars.derive.step.DeriveCan;
import nars.derive.step.Taskify;
import nars.term.control.OpSwitch;
import nars.derive.control.ValueFork;
import nars.term.Term;
import nars.term.control.AndCondition;
import nars.term.control.Fork;
import nars.term.control.PrediTerm;
import nars.unify.constraint.MatchConstraint;
import nars.unify.op.TaskBeliefOp;
import nars.unify.op.UnifyTerm;
import nars.util.term.TermTrie;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

import java.io.PrintStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static nars.Param.*;

/**
 * high-level interface for compiling premise deriver rules
 */
public enum PremiseDeriverCompiler { ;

    public static PremiseDeriver the(@NotNull Set<PremiseDeriverProto> r, @Nullable Function<PrediTerm<Derivation>, PrediTerm<Derivation>> each) {
        assert(!r.isEmpty());


        final TermTrie<PrediTerm<Derivation>, PrediTerm<Derivation>> path = new TermTrie<>();

        final FasterList<ValueFork> postChoices = new FasterList(128);

        Map<Set<PrediTerm<PreDerivation>>, RoaringBitmap> post = new HashMap<>(r.size());
        List<PrediTerm<Derivation>> conclusions = new FasterList<>(r.size() * 2);

        ObjectIntHashMap<Term> preconditionCount = new ObjectIntHashMap<>(256);

        r.forEach(rule -> {

            assert (rule.POST != null) : "null POSTconditions:" + rule;

            for (PostCondition p : rule.POST) {

                Pair<Set<PrediTerm<PreDerivation>>, PrediTerm<Derivation>> c = rule.build(p);

                c.getOne().forEach((k) -> preconditionCount.addToValue(k, 1));

                int id = conclusions.size();
                conclusions.add(c.getTwo());

                post.computeIfAbsent(c.getOne(), (x) ->
                        new RoaringBitmap()).add(id);
            }
        });

        Comparator<PrediTerm<?>> sortPrecondition = PrediTerm.sort(preconditionCount::get);

        post.forEach((k, v) -> {

            FasterList<PrediTerm<Derivation>> pre = new FasterList(k, PrediTerm[]::new, +1);
            pre.sortThis(sortPrecondition);

            PrediTerm<Derivation>[] branches = StreamSupport.stream(v.spliterator(), false)
                    .map(i -> compileBranch(conclusions.get(i))).toArray(PrediTerm[]::new);
            assert (branches.length > 0);


            ValueFork f;
            {
                //map the downstream conclusions to the causes upstream
                Taskify[] conc = Util.map(b -> (Taskify) AndCondition.last(((UnifyTerm.UnifySubtermThenConclude)
                        AndCondition.last(b)
                ).eachMatch), Taskify[]::new, branches);

                Cause[] causes = Util.map(c -> c.channel, Cause[]::new, conc);

                f = new ValueFork(branches, causes,

                    //weight vector func
                    d ->
                        Util.map(causes.length,
                                c -> Util.softmax(causes[c].value(), TRIE_DERIVER_TEMPERATURE),
                                new float[causes.length]),

                    //choice -> branch mapping: directly to the branch #
                    (d,choice) -> branches[choice]
                );
            }


            //attach this branch to the root fork
            {
                //append the conclusion step
                pre.add(new DeriveCan(
                    /* branch ID */ postChoices.size(), v));

                PrediTerm<Derivation> prev = path.put(pre, f);

                postChoices.add(f);

                assert (prev == null);
            }


        });

        assert(!path.isEmpty());


        PrediTerm<Derivation> compiledPaths = PremiseDeriverCompiler.compile(path, each);

        ValueFork[] rootBranches = postChoices.toArrayRecycled(ValueFork[]::new);
        return new PremiseDeriver(compiledPaths,
            new ValueFork(
                rootBranches,
                Stream.of(rootBranches).flatMap(b -> Stream.of(b.causes)).toArray(Cause[]::new),

                //weight vector function
                d ->
                    Util.map(d.will.length, (int i) ->
                        Util.softmax(
                            // sum of downstream cause values
                            Util.sum(
                                    (Cause c) -> c.value(),
                                rootBranches[d.will[i]].causes
                            ), TRIE_DERIVER_TEMPERATURE),
                        new float[d.will.length]),

                //choice -> branch mapping: mapped through the derivation's calculated possibilty set
                (d,choice) -> rootBranches[d.will[choice]]
        ));
    }

    static PrediTerm compileBranch(PrediTerm<Derivation> branch) {
        return branch instanceof AndCondition ?
                ((AndCondition) branch).transform(y -> y instanceof AndCondition ?
                    MatchConstraint.combineConstraints((AndCondition) y)
                    :
                    y
                , null)
                :
                branch;
    }

    public static void print(Object p, @NotNull PrintStream out, int indent) {

        TermTrie.indent(indent);

        if (p instanceof PremiseDeriver) {

            PremiseDeriver r = (PremiseDeriver) p;
            r.print(out, indent);

        } else if (p instanceof UnifyTerm.UnifySubtermThenConclude) {
            UnifyTerm.UnifySubtermThenConclude u = (UnifyTerm.UnifySubtermThenConclude) p;
            out.println("unify(" + UnifyTerm.label(u.subterm) + "," + u.pattern + ") {");
            print(u.eachMatch, out, indent + 2);
            TermTrie.indent(indent);
            out.println("}");
        } else if (p instanceof AndCondition) {
            out.println("and {");
            AndCondition ac = (AndCondition) p;
            for (PrediTerm b : ac.cond) {
                print(b, out, indent + 2);
            }
            TermTrie.indent(indent);
            out.println("}");
        } /*else if (p instanceof Try) {
            out.println("eval {");
            Try ac = (Try) p;
            int i = 0;
            for (PrediTerm b : ac.branches) {
                TermTrie.indent(indent + 2);
                out.println(i + ":");
                print(b, out, indent + 4);
                i++;
            }
            TermTrie.indent(indent);
            out.println("}");
        } */else if (p instanceof Fork) {
            out.println(Util.className(p) + " {");
            Fork ac = (Fork) p;
            for (PrediTerm b : ac.branch) {
                print(b, out, indent + 2);
            }
            TermTrie.indent(indent);
            out.println("}");

        } else if (p instanceof OpSwitch) {
            OpSwitch sw = (OpSwitch) p;
            out.println("switch(op(" + (sw.taskOrBelief ? "task" : "belief") + ")) {");
            int i = -1;
            for (PrediTerm b : sw.swtch) {
                i++;
                if (b == null) continue;

                TermTrie.indent(indent + 2);
                out.println('"' + Op.values()[i].toString() + "\": {");
                print(b, out, indent + 4);
                TermTrie.indent(indent + 2);
                out.println("}");

            }
            TermTrie.indent(indent);
            out.println("}");
        } else {
            out.print( /*Util.className(p) + ": " +*/ p);
            out.println();
        }


    }

//    static void forEach(Object p, @NotNull Consumer out) {
//        out.accept(p);
//
//        /*if (p instanceof IfThen) {
//
//            IfThen it = (IfThen) p;
//
//            TermTrie.indent(indent);
//            out.println(Util.className(p) + " (");
//            print(it.cond, out, indent + 2);
//
//            TermTrie.indent(indent);
//            out.println(") ==> {");
//
//            print(it.conseq, out, indent + 2);
//            TermTrie.indent(indent);
//            out.println("}");
//
//        } *//*else if (p instanceof If) {
//
//            indent(indent); out.println(Util.className(p) + " {");
//            {
//                If it = (If) p;
//                print(it.cond, out, indent + 2);
//            }
//            indent(indent); out.println("}");
//
//        }  else */
//        if (p instanceof AndCondition) {
//            //TermTrie.indent(indent);
//            //out.println("and {");
//            AndCondition ac = (AndCondition) p;
//            for (PrediTerm b : ac.cond) {
//                forEach(b, out);
//            }
//        } else if (p instanceof Fork) {
//            //TermTrie.indent(indent);
//            //out.println(Util.className(p) + " {");
//            Fork ac = (Fork) p;
//            for (PrediTerm b : ac.branches) {
//                forEach(b, out);
//            }
////            TermTrie.indent(indent);
////            out.println("}");
//
//        } else if (p instanceof OpSwitch) {
//            OpSwitch<?> sw = (OpSwitch) p;
//            //TermTrie.indent(indent);
//            //out.println("SubTermOp" + sw.subterm + " {");
//            for (PrediTerm b : sw.cases.values()) {
//                if (b == null) continue;
//
//                //TermTrie.indent(indent + 2);
//                //out.println('"' + Op.values()[i].toString() + "\": {");
//                //print(b, out, indent + 4);
//                forEach(b, out);
//                //TermTrie.indent(indent + 2);
//                //out.println("}");
//
//            }
//
//        } else if (p instanceof UnifyTerm.UnifySubtermThenConclude) {
//            forEach(((UnifyTerm.UnifySubtermThenConclude) p).eachMatch, out);
//        }
//
//
//    }
//
//    static void forEach(@Nullable PrediTerm in, PrediTerm x, @NotNull BiConsumer<PrediTerm, PrediTerm> out) {
//        out.accept(in, x);
//
//        /*if (p instanceof IfThen) {
//
//            IfThen it = (IfThen) p;
//
//            TermTrie.indent(indent);
//            out.println(Util.className(p) + " (");
//            print(it.cond, out, indent + 2);
//
//            TermTrie.indent(indent);
//            out.println(") ==> {");
//
//            print(it.conseq, out, indent + 2);
//            TermTrie.indent(indent);
//            out.println("}");
//
//        } *//*else if (p instanceof If) {
//
//            indent(indent); out.println(Util.className(p) + " {");
//            {
//                If it = (If) p;
//                print(it.cond, out, indent + 2);
//            }
//            indent(indent); out.println("}");
//
//        }  else */
//        if (x instanceof AndCondition) {
//            //TermTrie.indent(indent);
//            //out.println("and {");
//            AndCondition ac = (AndCondition) x;
//            for (PrediTerm y : ac.cond) {
//                forEach(x, y, out);
//            }
//        } else if (x instanceof Fork) {
//            //TermTrie.indent(indent);
//            //out.println(Util.className(p) + " {");
//            Fork ac = (Fork) x;
//            for (PrediTerm y : ac.branches) {
//                forEach(x, y, out);
//            }
////            TermTrie.indent(indent);
////            out.println("}");
//
//        } else if (x instanceof OpSwitch) {
//            OpSwitch<?> sw = (OpSwitch) x;
//            //TermTrie.indent(indent);
//            //out.println("SubTermOp" + sw.subterm + " {");
//            for (PrediTerm y : sw.cases.values()) {
//                if (y == null) continue;
//
//                //TermTrie.indent(indent + 2);
//                //out.println('"' + Op.values()[i].toString() + "\": {");
//                //print(b, out, indent + 4);
//                forEach(x, y, out);
//                //TermTrie.indent(indent + 2);
//                //out.println("}");
//
//            }
////            TermTrie.indent(indent);
////            out.println("}");
//        } else if (x instanceof UnifyTerm.UnifySubtermThenConclude) {
//            forEach(x, ((UnifyTerm.UnifySubtermThenConclude) x).eachMatch, out);
//        }
//    }

    public static void print(PrediTerm<Derivation> d) {
        print(d, System.out);
    }

    public static void print(Object d, PrintStream out) {
        PremiseDeriverCompiler.print(d, out, 0);

//        out.println("Fork {");
//
//        for (BoolPred p : roots())
//            print(p, out, 2);
//
//        out.println("}");
    }

//
//    public PrediTerm<Derivation> compile(Function<PrediTerm<Derivation>, PrediTerm<Derivation>> each) {
//        return compile(path, each);
//    }

    static <D extends PreDerivation> PrediTerm<D> compile(TermTrie<PrediTerm<D>, PrediTerm<D>> trie, Function<PrediTerm<D>, @Nullable PrediTerm<D>> each) {
        Collection<PrediTerm<D>> bb = compile(trie.root);

        PrediTerm tf = Fork.fork(bb.toArray(new PrediTerm[bb.size()]), Fork::new);
        if (each != null)
            tf = tf.transform(each);

        return tf;
    }


    @Nullable static <D extends PreDerivation> Collection<PrediTerm<D>> compile(TrieNode<List<PrediTerm<D>>, PrediTerm<D>> node) {


        Set<PrediTerm<D>> bb = new TreeSet();
//        assert(node.getKey()!=null);
//        assert(node.getValue()!=null);

        node.forEach(n -> {

            Collection<PrediTerm<D>> branches = compile(n);

            int nStart = n.start();
            int nEnd = n.end();
            PrediTerm<D> branch = PrediTerm.compileAnd(
                    n.seq().stream().skip(nStart).limit(nEnd - nStart),
                    branches!=null /* && !branches.isEmpty() */ ?
                            factorFork(new FasterList(branches), Fork::new)
                            : null
            );

            if (branch != null)
                bb.add(branch);
        });

        if (bb.isEmpty())
            return null;
        else
            return compileSwitch(bb);
    }



    private static class SubCond {
        public final PrediTerm p;
        final RoaringBitmap branches = new RoaringBitmap();

        private SubCond(PrediTerm p, int branch) {
            this.p = p;
            branches.add(branch);
        }

        void bump(int branch) {
            branches.add(branch);
        }

        static void bumpCond(Map<PrediTerm, SubCond> conds, PrediTerm p, int branch) {
            float pc = p.cost();
            if (pc == Float.POSITIVE_INFINITY)
                return; //postcondition

            conds.compute(p, (xx, e) -> {
                if (e == null) {
                    e = new SubCond(xx, branch);
                } else {
                    e.bump(branch);
                }
                return e;
            });
        }

        @Override
        public String toString() {
            return p + " x " + branches;
        }

        public int size() {
            return branches.getCardinality();
        }


        public float costIfBranch() {
            int s = size();
            if (s > 1)
                return s * p.cost();
            else
                return Float.NEGATIVE_INFINITY;
        }
    }

    private static <X> PrediTerm<X> factorFork(List<PrediTerm<X>> x, Function<PrediTerm<X>[], PrediTerm<X>> builder) {

        int n = x.size();
        if (n == 0)
            return null;

        if (n == 1)
            return x.get(0);

        MutableMap<PrediTerm, SubCond> conds = new UnifiedMap();
        for (int b = 0, xSize = n; b < xSize; b++) {
            PrediTerm p = x.get(b);
            if (p instanceof AndCondition) {
                for (PrediTerm xx : ((AndCondition<PrediTerm>) p).cond)
                    SubCond.bumpCond(conds, xx, b);
            } else if (p instanceof Fork) {
                //ignore
            } else {
                SubCond.bumpCond(conds, p, b);
            }
        }

        SubCond fx = conds.maxBy(SubCond::costIfBranch);
        if (fx.size() < 2) {
            //nothing to factor
        } else {
            List<PrediTerm> bundle = $.newArrayList();
            int i = 0;
            Iterator<PrediTerm<X>> xx = x.iterator();
            while (xx.hasNext()) {
                PrediTerm<X> px = xx.next();
                if (fx.branches.contains(i)) {
                    xx.remove();

                    if (px instanceof AndCondition) {
                        px = AndCondition.the(ArrayUtils.removeAllOccurences(((AndCondition) px).cond, fx.p));
                        bundle.add(px);
                    } else {

                    }
                }
                i++;
            }
            if (!bundle.isEmpty()) {
                assert (bundle.size() > 1);
                x.add(AndCondition.the(fx.p, Fork.fork(bundle.toArray(new PrediTerm[bundle.size()]), builder)));

                if (x.size() == 1)
                    return x.get(0);
            }
        }

        return Fork.fork(x.toArray(new PrediTerm[x.size()]), builder);
    }


    protected static <D extends PreDerivation> Collection<PrediTerm<D>> compileSwitch(Collection<PrediTerm<D>> branches) {


        branches = factorSubOpToSwitch(branches, true, 2);
        branches = factorSubOpToSwitch(branches, false, 2);

        return branches;
    }

    @NotNull
    private static <D extends PreDerivation> Collection<PrediTerm<D>> factorSubOpToSwitch(Collection<PrediTerm<D>> bb, boolean taskOrBelief, int minToCreateSwitch) {
        if (!bb.isEmpty()) {
            Map<TaskBeliefOp, PrediTerm<D>> cases = $.newHashMap(8);
            List<PrediTerm<D>> removed = $.newArrayList(); //in order to undo
            bb.removeIf(p -> {
                if (p instanceof AndCondition) {
                    AndCondition ac = (AndCondition) p;
                    return ac.OR(x -> {
                        if (x instanceof TaskBeliefOp) {
                            TaskBeliefOp so = (TaskBeliefOp) x;
                            if (so.isOrIsNot && so.task == taskOrBelief && so.belief == !taskOrBelief) {
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
                cases.forEach((c, p) -> {
                    int cs = c.structure;
                    Op[] opVals = Op.values();
                    for (int i = 0; i < opVals.length; i++) {
                        if ((cs & (1 << i)) > 0)
                            caseMap.put(opVals[i], p);
                    }

                });
                bb.add(new OpSwitch<>(taskOrBelief, caseMap));
            } else {
                bb.addAll(removed); //undo
            }
        }

        return bb;
    }


}

//    public static TrieDeriver get(String individualRule) throws Narsese.NarseseException {
//        return get(new PremiseRuleSet(true, PremiseRule.rule(individualRule)));
//    }


//    static class StackFrame {
//        private final int i;
//        int version;
//        BoolPred<Derivation> op;
//
//        public StackFrame(BoolPred<Derivation> op, int now, int i) {
//            this.version = now;
//            this.op = op;
//            this.i = i;
//        }
//
//        @Override
//        public String toString() {
//            return "StackFrame{" +
//                    "i=" + i +
//                    ", version=" + version +
//                    ", op=" + op.getClass() +
//                    '}';
//        }
//    }
//
//    /* one-method interpreter, doesnt recursive call but maintains its own stack */
//    @Override
//    public boolean test(@NotNull Derivation m) {
//
//        FasterList<StackFrame> stack = new FasterList(16);
//
//        BoolPred<Derivation> cur = this;
//        int i = 0;
//        //StackFrame x = new StackFrame(this, m.now(), 0);
//        BoolPred[] and = new BoolPred[]{this};
//
//        main:
//        do {
//
//            System.out.println(stack);
//
//
//            BoolPred y = and[i++];
//            if (y instanceof Fork) {
//                cur = y;
//                Fork f = (Fork) cur;
//                if (i == f.cached.length) {
//                    StackFrame pop = stack.removeLast();
//                    m.revert(pop.version);
//                    cur = pop.op;
//                    i = pop.i;
//                    continue;
//                } else if (i == 0) {
//                    stack.add(new StackFrame(f, m.now(), i)); //save
//                }
//                cur = f.cached[i++];
//            } else {
//                if (!y.test(m))
//                    break main;
//            }
//
//        } while (!stack.isEmpty());
//
//        return true;
//    }


//    public void recurse(@NotNull CauseEffect each) {
//        for (BoolCondition p : roots) {
//            recurse(null, p, each);
//        }
//    }

//        public interface CauseEffect extends BiConsumer<Term, Term> {
//
//        }

//    public static Term recurse(Term pred, Term curr, @NotNull CauseEffect each) {
//
//        each.accept(pred, curr);
//
//        /*if (curr instanceof IfThen) {
//
//            IfThen it = (IfThen) curr;
////            each.accept(/*recurse(curr, _/it.cond/_, each)-/, recurse(curr, it.conseq, each));
//
//        } else */
//        if (curr instanceof AndCondition) {
//
//            AndCondition ac = (AndCondition) curr;
//            Term p = curr;
//            for (BoolCondition b : ac.termCache) {
//                p = recurse(p, b, each);
//            }
//
//        } else if (curr instanceof Fork) {
//            Fork ac = (Fork) curr;
//            for (BoolCondition b : ac.termCache) {
//                recurse(curr, b, each);
//            }
//        } else if (curr instanceof PatternOpSwitch) {
//            PatternOpSwitch sw = (PatternOpSwitch) curr;
//            int i = -1;
//            for (BoolCondition b : sw.proc) {
//                i++;
//                if (b == null)
//                    continue;
//
//                //construct a virtual if/then branch to emulate the entire switch structure
//                recurse(curr,
//                        AndCondition.the(Lists.newArrayList(
//                                new PatternOp(sw.subterm, Op.values()[i]), b)),
//                        each);
//
//            }
//        }
//
//        return curr;
//    }

//    @NotNull
//    static Stream<PrediTerm<ProtoDerivation>> conditions(@NotNull Stream<PrediTerm<ProtoDerivation>> t) {
//
////
////            final AtomicReference<UnificationPrototype> unificationParent = new AtomicReference<>(null);
////
////            return t.filter(x -> {
////                if (x instanceof Conclude) {
////                    //link this derivation action to the previous Match,
////                    //allowing multiple derivations to fold within a Match's actions
////                    UnificationPrototype mt = unificationParent.getAndSet(null);
////                    if (mt == null) {
////                        throw new RuntimeException("detached Derive action: " + x + " in branch: " + t);
////                        //System.err.println("detached Derive action: " + x + " in branch: " + t);
////                    } else {
////                        mt.derive((Conclude) x);
////                    }
////                    return false;
////                } else if (x instanceof BoolPred) {
////                    if (x instanceof UnificationPrototype) {
////
////                        unificationParent.set((UnificationPrototype) x);
////                    }
////                }
////                return true;
//        /*filter(x -> !(x instanceof Conclude)).*/
//        return t;
//    }


//    @NotNull
//    private static ProcTerm compileActions(@NotNull List<ProcTerm> t) {
//
//        switch (t.size()) {
//            case 0: return null;
//            case 1:
//                return t.get(0);
//            default:
//                //optimization: find expression prefix types common to all, and see if a switch can be formed
//
//        }
//
//    }

//    //TODO not complete
//    protected void compile(@NotNull ProcTerm p) throws IOException, CannotCompileException, NotFoundException {
//        StringBuilder s = new StringBuilder();
//
//        final String header = "public final static String wtf=" +
//                '"' + this + ' ' + new Date() + "\"+\n" +
//                "\"COPYRIGHT (C) OPENNARS. ALL RIGHTS RESERVED.\"+\n" +
//                "\"THIS SOURCE CODE AND ITS GENERATOR IS PROTECTED BY THE AFFERO GENERAL PUBLIC LICENSE: https://gnu.org/licenses/agpl.html\"+\n" +
//                "\"http://github.com/opennars/opennars\";\n";
//
//        //System.out.print(header);
//        p.appendJavaProcedure(s);
//
//
//        ClassPool pool = ClassPool.getDefault();
//        pool.importPackage("nars.truth");
//        pool.importPackage("nars.nal");
//
//        CtClass cc = pool.makeClass("nars.nal.CompiledDeriver");
//        CtClass parent = pool.get("nars.nal.Deriver");
//
//        cc.addField(CtField.make(header, cc));
//
//        cc.setSuperclass(parent);
//
//        //cc.addConstructor(parent.getConstructors()[0]);
//
//        String initCode = "nars.Premise p = m.premise;";
//
//        String m = "public void run(nars.nal.PremiseMatch m) {\n" +
//                '\t' + initCode + '\n' +
//                '\t' + s + '\n' +
//                '}';
//
//        System.out.println(m);
//
//
//        cc.addMethod(CtNewMethod.make(m, cc));
//        cc.writeFile("/tmp");
//
//        //System.out.println(cc.toBytecode());
//        System.out.println(cc);
//    }
//

//final static Logger logger = LoggerFactory.getLogger(TrieDeriver.class);


//    final static void run(RuleMatch m, List<TaskRule> rules, int level, Consumer<Task> t) {
//
//        final int nr = rules.size();
//        for (int i = 0; i < nr; i++) {
//
//            TaskRule r = rules.get(i);
//            if (r.minNAL > level) continue;
//
//            PostCondition[] pc = m.run(r);
//            if (pc != null) {
//                for (PostCondition p : pc) {
//                    if (p.minNAL > level) continue;
//                    ArrayList<Task> Lx = m.apply(p);
//                    if(Lx!=null) {
//                        for (Task x : Lx) {
//                            if (x != null)
//                                t.accept(x);
//                        }
//                    }
//                    /*else
//                        System.out.println("Post exit: " + r + " on " + m.premise);*/
//                }
//            }
//        }
//    }


//    static final class Return extends Atom implements ProcTerm {
//
//        public static final ProcTerm the = new Return();
//
//        private Return() {
//            super("return");
//        }
//
//
//        @Override
//        public void appendJavaProcedure(@NotNull StringBuilder s) {
//            s.append("return;");
//        }
//
//        @Override
//        public void accept(PremiseEval versioneds) {
//            System.out.println("why call this");
//            //throw new UnsupportedOperationException("should not be invoked");
//        }
//
//    }

//    /** just evaluates a boolean condition HACK */
//    static final class If extends GenericCompound implements ProcTerm {
//
//
//        public final transient @NotNull BoolCondition cond;
//
//
//        public If(@NotNull BoolCondition cond) {
//            super(Op.IMPLICATION,
//                    TermVector.the( cond, Return.the)
//            );
//
//            this.cond = cond;
//        }
//
//        @Override public void accept(@NotNull PremiseEval m) {
//            final int stack = m.now();
//            cond.booleanValueOf(m);
//            m.revert(stack);
//        }
//
//    }


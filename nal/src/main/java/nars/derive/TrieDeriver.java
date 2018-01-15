package nars.derive;

import jcog.Util;
import jcog.list.FasterList;
import jcog.tree.perfect.TrieNode;
import nars.$;
import nars.Op;
import nars.control.Derivation;
import nars.control.ProtoDerivation;
import nars.derive.constraint.MatchConstraint;
import nars.derive.op.TaskBeliefOp;
import nars.derive.op.UnifyTerm;
import nars.derive.rule.PremiseRuleSet;
import nars.term.Term;
import nars.term.pred.AndCondition;
import nars.term.pred.Fork;
import nars.term.pred.PrediTerm;
import nars.util.TermTrie;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * predicate trie, for maximum folding
 * TODO generify this beyond Derivation state
 */
public final class TrieDeriver {

    final TermTrie<PrediTerm<Derivation>, PrediTerm<Derivation>> path;
    final FasterList<ValueFork> postChoices = new FasterList();

    public TrieDeriver(PremiseRuleSet r) {

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
        TrieDeriver t = new TrieDeriver(r);

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
                TrieDeriver.compile(t.path, each),
                new Try(t.postChoices.toArrayRecycled(ValueFork[]::new)));
    }

    public static void print(Object p, @NotNull PrintStream out, int indent) {

        TermTrie.indent(indent);

        if (p instanceof DeriverRoot) {

            DeriverRoot r = (DeriverRoot) p;
            print(AndCondition.the(new PrediTerm[]{r.what, r.can}) /* HACK */, out, indent);

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
        } else if (p instanceof Try) {
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
        } else if (p instanceof Fork) {
            out.println(Util.className(p) + " {");
            Fork ac = (Fork) p;
            for (PrediTerm b : ac.branches) {
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

    static void forEach(Object p, @NotNull Consumer out) {
        out.accept(p);

        /*if (p instanceof IfThen) {

            IfThen it = (IfThen) p;

            TermTrie.indent(indent);
            out.println(Util.className(p) + " (");
            print(it.cond, out, indent + 2);

            TermTrie.indent(indent);
            out.println(") ==> {");

            print(it.conseq, out, indent + 2);
            TermTrie.indent(indent);
            out.println("}");

        } *//*else if (p instanceof If) {

            indent(indent); out.println(Util.className(p) + " {");
            {
                If it = (If) p;
                print(it.cond, out, indent + 2);
            }
            indent(indent); out.println("}");

        }  else */
        if (p instanceof AndCondition) {
            //TermTrie.indent(indent);
            //out.println("and {");
            AndCondition ac = (AndCondition) p;
            for (PrediTerm b : ac.cond) {
                forEach(b, out);
            }
        } else if (p instanceof Fork) {
            //TermTrie.indent(indent);
            //out.println(Util.className(p) + " {");
            Fork ac = (Fork) p;
            for (PrediTerm b : ac.branches) {
                forEach(b, out);
            }
//            TermTrie.indent(indent);
//            out.println("}");

        } else if (p instanceof OpSwitch) {
            OpSwitch<?> sw = (OpSwitch) p;
            //TermTrie.indent(indent);
            //out.println("SubTermOp" + sw.subterm + " {");
            for (PrediTerm b : sw.cases.values()) {
                if (b == null) continue;

                //TermTrie.indent(indent + 2);
                //out.println('"' + Op.values()[i].toString() + "\": {");
                //print(b, out, indent + 4);
                forEach(b, out);
                //TermTrie.indent(indent + 2);
                //out.println("}");

            }

        } else if (p instanceof UnifyTerm.UnifySubtermThenConclude) {
            forEach(((UnifyTerm.UnifySubtermThenConclude) p).eachMatch, out);
        }


    }

    static void forEach(@Nullable PrediTerm in, PrediTerm x, @NotNull BiConsumer<PrediTerm, PrediTerm> out) {
        out.accept(in, x);

        /*if (p instanceof IfThen) {

            IfThen it = (IfThen) p;

            TermTrie.indent(indent);
            out.println(Util.className(p) + " (");
            print(it.cond, out, indent + 2);

            TermTrie.indent(indent);
            out.println(") ==> {");

            print(it.conseq, out, indent + 2);
            TermTrie.indent(indent);
            out.println("}");

        } *//*else if (p instanceof If) {

            indent(indent); out.println(Util.className(p) + " {");
            {
                If it = (If) p;
                print(it.cond, out, indent + 2);
            }
            indent(indent); out.println("}");

        }  else */
        if (x instanceof AndCondition) {
            //TermTrie.indent(indent);
            //out.println("and {");
            AndCondition ac = (AndCondition) x;
            for (PrediTerm y : ac.cond) {
                forEach(x, y, out);
            }
        } else if (x instanceof Fork) {
            //TermTrie.indent(indent);
            //out.println(Util.className(p) + " {");
            Fork ac = (Fork) x;
            for (PrediTerm y : ac.branches) {
                forEach(x, y, out);
            }
//            TermTrie.indent(indent);
//            out.println("}");

        } else if (x instanceof OpSwitch) {
            OpSwitch<?> sw = (OpSwitch) x;
            //TermTrie.indent(indent);
            //out.println("SubTermOp" + sw.subterm + " {");
            for (PrediTerm y : sw.cases.values()) {
                if (y == null) continue;

                //TermTrie.indent(indent + 2);
                //out.println('"' + Op.values()[i].toString() + "\": {");
                //print(b, out, indent + 4);
                forEach(x, y, out);
                //TermTrie.indent(indent + 2);
                //out.println("}");

            }
//            TermTrie.indent(indent);
//            out.println("}");
        } else if (x instanceof UnifyTerm.UnifySubtermThenConclude) {
            forEach(x, ((UnifyTerm.UnifySubtermThenConclude) x).eachMatch, out);
        }
    }

    public static void print(PrediTerm<Derivation> d) {
        print(d, System.out);
    }

    public static void print(PrediTerm<Derivation> d, PrintStream out) {
        TrieDeriver.print(d, out, 0);

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

    static <D extends ProtoDerivation> PrediTerm<D> compile(TermTrie<PrediTerm<D>, PrediTerm<D>> trie, Function<PrediTerm<D>, PrediTerm<D>> each) {
        List<PrediTerm<D>> bb = compile(trie.root);
        PrediTerm<D>[] roots = bb.toArray(new PrediTerm[bb.size()]);

        PrediTerm tf = Fork.fork(roots, x -> new Fork(x));
        if (each != null)
            tf = tf.transform(each);

        return tf;
    }


    static <D extends ProtoDerivation> List<PrediTerm<D>> compile(TrieNode<List<PrediTerm<D>>, PrediTerm<D>> node) {


        List<PrediTerm<D>> bb = $.newArrayList(node.childCount());
//        assert(node.getKey()!=null);
//        assert(node.getValue()!=null);

        node.forEach(n -> {

            List<PrediTerm<D>> branches = compile(n);

            int nStart = n.start();
            int nEnd = n.end();
            PrediTerm<D> branch = PrediTerm.compileAnd(
                    n.seq().stream().skip(nStart).limit(nEnd - nStart),
                    !branches.isEmpty() ?
                            factorFork(branches, (PrediTerm<D>[] ff) -> new Fork(ff))
//                            Fork.fork(branches.toArray(new PrediTerm[branches.size()]),
//                                    x -> new Fork(x))
                            : null
            );

            if (branch != null)
                bb.add(branch);
        });

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
            if (p.cost() == Integer.MAX_VALUE)
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

        SubCond fx = conds.maxBy(xx -> xx.costIfBranch());
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
            assert(bundle.size()>1);
            x.add(AndCondition.the(fx.p, Fork.fork(bundle.toArray(new PrediTerm[bundle.size()]), builder)));

            if (x.size() == 1)
                return x.get(0);
        }

        return Fork.fork(x.toArray(new PrediTerm[x.size()]), builder);
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


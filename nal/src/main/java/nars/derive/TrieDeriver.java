package nars.derive;

import jcog.Util;
import nars.Op;
import nars.control.Derivation;
import nars.derive.op.UnifyTerm;
import nars.util.TermTrie;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;


/**
 * separates rules according to task/belief term type but otherwise involves significant redundancy we'll eliminate in other Deriver implementations
 */
public enum TrieDeriver {
    ;


//    public static TrieDeriver get(String individualRule) throws Narsese.NarseseException {
//        return get(new PremiseRuleSet(true, PremiseRule.rule(individualRule)));
//    }

    public static void print(Object p, @NotNull PrintStream out, int indent) {

        TermTrie.indent(indent);

        if (p instanceof UnifyTerm.UnifySubtermThenConclude) {
            UnifyTerm.UnifySubtermThenConclude u = (UnifyTerm.UnifySubtermThenConclude)p;
            out.println("unify(" + UnifyTerm.label(u.subterm) + "," + u.pattern + ") {");
            print(u.eachMatch, out, indent+2);
            TermTrie.indent(indent); out.println("}");
        } else if (p instanceof AndCondition) {
            out.println("and {");
            AndCondition ac = (AndCondition) p;
            for (PrediTerm b : ac.cond) {
                print(b, out, indent + 2);
            }
            TermTrie.indent(indent); out.println("}");
        } else if (p instanceof Try) {
            out.println("eval {");
            Try ac = (Try) p;
            int i = 0;
            for (PrediTerm b : ac.branches) {
                TermTrie.indent(indent + 2); out.println(i + ":");
                print(b, out, indent + 4);
                i++;
            }
            TermTrie.indent(indent); out.println("}");
        } else if (p instanceof Fork) {
            out.println(Util.className(p) + " {");
            Fork ac = (Fork) p;
            for (PrediTerm b : ac.branches) {
                print(b, out, indent + 2);
            }
            TermTrie.indent(indent); out.println("}");

        } else if (p instanceof OpSwitch) {
            OpSwitch sw = (OpSwitch) p;
            out.println("switch(op(" + (sw.subterm == 0 ? "task" : "belief") + ")) {");
            int i = -1;
            for (PrediTerm b : sw.swtch) {
                i++;
                if (b == null) continue;

                TermTrie.indent(indent + 2);
                out.println('"' + Op.values()[i].toString() + "\": {");
                print(b, out, indent + 4);
                TermTrie.indent(indent + 2); out.println("}");

            }
            TermTrie.indent(indent); out.println("}");
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
            OpSwitch sw = (OpSwitch) p;
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
            OpSwitch sw = (OpSwitch) x;
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


    @Nullable
    public static PrediTerm<Derivation> ifThen(Stream<PrediTerm<Derivation>> cond, @Nullable PrediTerm<Derivation> conseq) {
        return AndCondition.the(
                (conseq != null ? Stream.concat(cond, Stream.of(conseq)) : cond).toArray(PrediTerm[]::new)
        );
    }

    public static void print(PrediTerm<Derivation> d) {
        print(d, System.out);
    }

    public static void print(PrediTerm<Derivation> d, PrintStream out) {
        print(d, out, 0);

//        out.println("Fork {");
//
//        for (BoolPred p : roots())
//            print(p, out, 2);
//
//        out.println("}");
    }


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

    @NotNull
    static Stream<PrediTerm<Derivation>> conditions(@NotNull Stream<PrediTerm<Derivation>> t) {

//
//            final AtomicReference<UnificationPrototype> unificationParent = new AtomicReference<>(null);
//
//            return t.filter(x -> {
//                if (x instanceof Conclude) {
//                    //link this derivation action to the previous Match,
//                    //allowing multiple derivations to fold within a Match's actions
//                    UnificationPrototype mt = unificationParent.getAndSet(null);
//                    if (mt == null) {
//                        throw new RuntimeException("detached Derive action: " + x + " in branch: " + t);
//                        //System.err.println("detached Derive action: " + x + " in branch: " + t);
//                    } else {
//                        mt.derive((Conclude) x);
//                    }
//                    return false;
//                } else if (x instanceof BoolPred) {
//                    if (x instanceof UnificationPrototype) {
//
//                        unificationParent.set((UnificationPrototype) x);
//                    }
//                }
//                return true;
        /*filter(x -> !(x instanceof Conclude)).*/
        return t;
    }


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


}

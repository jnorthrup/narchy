package nars.derive;

import nars.$;
import nars.control.Derivation;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;


/**
 * parallel branching
 * <p>
 * TODO generify beyond only Derivation
 */
public class Fork extends AbstractPred<Derivation> {

    public final PrediTerm<Derivation>[] branches;

    protected Fork(PrediTerm[] actions) {
        super($.s((Term[]) actions));
        assert (actions.length > 0);
        this.branches = actions;
    }

    @Override
    public float cost() {
        return Integer.MAX_VALUE; //post-condition: must be the last element in any sequence
    }

    @Override
    public PrediTerm<Derivation> transform(Function<PrediTerm<Derivation>, PrediTerm<Derivation>> f) {
        return fork(PrediTerm.transform(f, branches));
    }

    /**
     * simple exhaustive impl
     */
    @Override
    public boolean test(Derivation m) {

        int before = m.now();

        for (PrediTerm c : branches) {

            c.test(m);

            if (!m.revertLive(before)) { //maybe possible to eliminate for pre's
                return false;
            }
        }

        return true;
    }


//    public static class ShuffledFork extends Fork {
//
//        protected ShuffledFork(@NotNull PrediTerm[] actions) {
//            super(actions);
//        }
//
//        @Override
//        public boolean test(Derivation m) {
//
//            int before = m.now();
//
//            int branches = this.branches.length;
//            if (branches == 1) {
//                this.branches[0].test(m);
//                return m.revertLive(before);
//            } else {
//                ByteShuffler b = m.shuffler;
//                byte[] order = b.shuffle(m.random, branches, true); //must get a copy because recursion will re-use the shuffler's internal array
//
//                for (int i = 0; i < branches; i++) {
//                    this.branches[order[i]].test(m);
//                    if (!m.revertLive(before))
//                        return false;
//                }
//            }
//            return true;
//        }
//    }

    @Nullable
    public static PrediTerm<Derivation> fork(PrediTerm<Derivation>[] n) {
        switch (n.length) {
            case 0:
                return null;
            case 1:
                return n[0];
            default:
                return new Fork(n);
        }
    }

//    @Override
//    public PrediTerm<Derivation> exec(Derivation d, CPU c) {
//        c.fork(d, cache);
//        return null;
//    }


    //    @Override
//    public void appendJavaProcedure(@NotNull StringBuilder s) {
//        //s.append("/* " + this + "*/");
//        for (ProcTerm p : terms()) {
//            s.append("\t\t");
//            p.appendJavaProcedure(s);
//            s.append('\n');
//        }
//    }


//    public static PremiseMatch fork(PremiseMatch m, ProcTerm<PremiseMatch> proc) {
//        int revertTime = m.now();
//        proc.accept(m);
//        m.revert(revertTime);
//        return m;
//    }

//        @Override public void accept(C m) {
//
////            try {
////                method.invoke(m);
////            } catch (Throwable throwable) {
////                throwable.printStackTrace();
////            }
//
//            int revertTime = m.now();
//            for (ProcTerm<PremiseMatch> s : terms()) {
//                s.accept(m);
//                m.revert(revertTime);
//            }
//        }

//private final MethodHandle method;
//            try {
//                MethodHandles.Lookup l = MethodHandles.publicLookup();
//
//                Binder b = null;
//                for (ProcTerm p : children) {
//                    //MethodHandle ph = l.findVirtual(p.getClass(), "accept", methodType(PremiseMatch.class));
//
//
//                    Binder a = new Binder(Binder.from(PremiseMatch.class, PremiseMatch.class)
//                            .append(ProcTerm.class, p))
//                            .foldStatic(ThenFork.class, "fork").dropLast(2);
//
//                    if (b!=null)
//                        b = a.to(b);
//                    else
//                        b = a;
//
//                }
//                this.method = b!=null ? b.identity() : null;
//
//            } catch (Exception e) {
//                e.printStackTrace();
//                throw new RuntimeException(e);
//            }

    //        //1. test for switch on all op type (TODO several term types with a a default branch for non-op type conditions)
//
//        IntObjectHashMap<ProcTerm> table  = new IntObjectHashMap<ProcTerm>();
//
//        int[] which = new int[]{-1}; //subterm index, ensure they are all referring to the same
//
//        boolean isOpSwitch = Stream.of(n).allMatch(p -> {
//
//            if (p instanceof PremiseBranch) {
//               PremiseBranch b = (PremiseBranch)p;
//               BooleanCondition<PremiseEval> c = b.cond; //condition
//               if (c instanceof AndCondition) {
//                   AndCondition cAnd = (AndCondition) c;
//                   c = (BooleanCondition<PremiseEval>) cAnd.termCache[0]; //first term which happens to hold the op type, but for more robustness, find and pull out any op tpyes which are not in this first position
//               }
//               if (c instanceof SubTermOp) {
//                   SubTermOp sb = (SubTermOp)c;
//
//
//                   if (which[0] == -1)
//                       which[0] = sb.subterm;
//                   else if (which[0] != sb.subterm)
//                       return false;
//
//                   ProcTerm d; //consequence
//
//
//                   if (c instanceof AndCondition) {
//                       AndCondition<PremiseEval> cAnd = (AndCondition) c;
//                       d = (ProcTerm) cAnd.without(sb);
//                       if (d == null)
//                           d = b.conseq;
//                   } else {
//                       d = b.conseq;
//                   }
//
//                   table.put(sb.op, d);
//
//                   return true;
//               }
//            }
//
//            return false;
//
//        });
//        if (which[0]!=-1 && table.size() > 1) {
//            System.out.println(table);
//        }


}

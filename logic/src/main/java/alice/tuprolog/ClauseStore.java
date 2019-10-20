package alice.tuprolog;

import jcog.data.list.FasterList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.ListIterator;

/**
 * A list of clauses belonging to the same family as a goal. A family is
 * composed by clauses with the same functor and arity.
 */
public final class ClauseStore {


    private @Nullable Deque<ClauseInfo> clauses = null;
    private final Term goal;
    private final List<Var> vars;

    private ClauseStore(Term goal, List<Var> vars) {
        this.goal = goal;
        this.vars = vars;
    }

    public static @Nullable ClauseStore match(Term goal, Deque<ClauseInfo> familyClauses, @Nullable List<Var> vars) {
        if (!familyClauses.isEmpty()) {
            var clauseStore = new ClauseStore(goal, vars);
            if (clauseStore.matchFirst(familyClauses))
                return clauseStore;
        }

        return null;
    }


    /**
     * Restituisce la clausola da caricare
     */
    public ClauseInfo fetchNext(boolean pop, boolean save) {
        var clauses = this.clauses;
        if (clauses == null)
            return null;

        var v = vars.size();
        if (save && v == 0) save = false;

        ClauseInfo clause = null;
        while (!clauses.isEmpty()) {

            if (pop) {
                clause = clauses.removeFirst();
                if (clauses.isEmpty())
                    this.clauses = null;
            } else
                clause = clauses.peekFirst();

            var saveUnifications = deunify(vars, save ? new FasterList<>(v) : null);

            var u = goal.unifiable(clause.head);

            if (saveUnifications != null)
                reunify(vars, saveUnifications, v);

            if (!u) {
                clause = null;
                if (!pop) {
                    clauses.removeFirst(); //remove the un-unifiable entry since it wasnt popped already
                    if (clauses.isEmpty()) {
                        this.clauses = null;
                        break;
                    }
                }
            } else {
                break; //got it
            }

        }

        return clause;
    }


    public boolean haveAlternatives() {
        return clauses != null && !clauses.isEmpty();
    }


    /**
     * Verify if there is a term in compatibleGoals compatible with goal.
     *
     * @param goal
     * @param compGoals
     * @return true if compatible or false otherwise.
     */
    protected boolean unifiesMore() {

        //boolean found = unifiable(goal);
        var found = fetchNext(false, true) != null;


        return found;
    }

    protected boolean matchFirst(Deque<ClauseInfo> d) {

        deunify(vars, null);

        var found = unifiableFirst(goal, d);

        return found;
    }

    /**
     * Salva le unificazioni delle variabili da deunificare
     *
     * @param varsToDeunify
     * @return unificazioni delle variabili
     */
    private static List<Term> deunify(List<Var> varsToDeunify, @Nullable List<Term> saveUnifications) {


        for (var v : varsToDeunify) {
            if (saveUnifications != null)
                saveUnifications.add(v.link);
            v.link = null;
        }
        return saveUnifications;
    }


    /**
     * Restore previous unifications into variables.
     *
     * @param varsToReunify
     * @param saveUnifications
     */
    private static void reunify(List<Var> varsToReunify, List<Term> saveUnifications, int size) {

        if (varsToReunify instanceof FasterList && saveUnifications instanceof FasterList) {
            for (int i = size - 1, j = i; i >= 0; ) {
                varsToReunify.get(i--).setLink(saveUnifications.get(j--));
            }
        } else {

            var it1 = varsToReunify.listIterator(size);
            var it2 = saveUnifications.listIterator(size);
            while (it1.hasPrevious()) {
                it1.previous().setLink(it2.previous());
            }
        }
    }


//    /**
//     * Verify if a clause exists that is compatible with goal.
//     * As a side effect, clauses that are not compatible get
//     * discarded from the currently examined family.
//     *
//     * @param goal
//     */
//    @Deprecated private boolean unifiable(Term goal) {
//        if (!this.goal.equals(goal))
//            throw new WTF();
//
//        Deque<ClauseInfo> clauses = this.clauses;
//        if (clauses == null)
//            return false;
//        for (ClauseInfo clause : clauses) {
//            if (goal.unifiable(clause.head)) return true;
//        }
//        return false;
//    }

    private boolean unifiableFirst(Term goal, Deque<ClauseInfo> matching) {
        Deque<ClauseInfo> clauses = null;
        for (var ci : matching) {
            if (clauses == null) {
                deunify(vars, null);
                if (goal.unifiable(ci.head)) {

                    clauses = new ArrayDeque<>(/* other.size() - 1 - i */);
                    //start the unify queue beginning here
                    //TODO only need to store an iterator to continue
                }
            }
            if (clauses != null) {
                clauses.add(ci); //queue for future test
            }
        }
        if (clauses == null) {
            return false;
        } else {
            this.clauses = clauses;
            return true;
        }
    }

    public ClauseInfo fetchFirst() {
        return this.clauses.removeFirst();
    }

    public String toString() {
        return "clauses: " + clauses + '\n' +
                "goal: " + goal + '\n' +
                "vars: " + vars + '\n';
    }



    /*
     * Methods for spyListeners
     */


}
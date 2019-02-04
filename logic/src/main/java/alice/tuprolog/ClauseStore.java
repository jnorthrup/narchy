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


    @Nullable private Deque<ClauseInfo> clauses = null;
    private final Term goal;
    private final List<Var> vars;
    private boolean haveAlternatives;

    private ClauseStore(Term goal, List<Var> vars) {
        this.goal = goal;
        this.vars = vars;
    }

    public static ClauseStore match(Term goal, Deque<ClauseInfo> familyClauses, @Nullable List<Var> vars) {
        ClauseStore clauseStore = new ClauseStore(goal, vars);
        if (clauseStore.match(familyClauses))
            return clauseStore;
        else
            return null;
    }


    /**
     * Restituisce la clausola da caricare
     */
    public ClauseInfo fetch() {
        Deque<ClauseInfo> clauses = this.clauses;
        if (clauses == null || clauses.isEmpty()) return null;

        deunify(vars, null);

        ClauseInfo clause = this.clauses.removeFirst();

        if (clauses.isEmpty())
            this.clauses = null;
        else
            this.haveAlternatives = true;

        return clause;
    }


    public boolean haveAlternatives() {
        return haveAlternatives;
    }


    /**
     * Verify if there is a term in compatibleGoals compatible with goal.
     *
     * @param goal
     * @param compGoals
     * @return true if compatible or false otherwise.
     */
    @Deprecated protected boolean unifiable() {
        int n = vars.size();
        List<Term> saveUnifications = n > 0 ? deunify(vars, new FasterList<>(n)) : null;

        boolean found = unifiable(goal);

        if (n > 0)
            reunify(vars, saveUnifications, n);

        return found;
    }

    protected boolean match(Deque<ClauseInfo> d) {

        deunify(vars, null);

        boolean found = unifiable(goal, d);

        return found;
    }

    /**
     * Salva le unificazioni delle variabili da deunificare
     *
     * @param varsToDeunify
     * @return unificazioni delle variabili
     */
    private static List<Term> deunify(List<Var> varsToDeunify, @Nullable List<Term> saveUnifications) {


        for (Var v : varsToDeunify) {
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
            for (int i = size-1, j = i; i >= 0;) {
                varsToReunify.get(i--).setLink(saveUnifications.get(j--));
            }
        } else {

            ListIterator<Var> it1 = varsToReunify.listIterator(size);
            ListIterator<Term> it2 = saveUnifications.listIterator(size);
            while (it1.hasPrevious()) {
                it1.previous().setLink(it2.previous());
            }
        }
    }


    /**
     * Verify if a clause exists that is compatible with goal.
     * As a side effect, clauses that are not compatible get
     * discarded from the currently examined family.
     *
     * @param goal
     */
    @Deprecated private boolean unifiable(Term goal) {
        Deque<ClauseInfo> clauses = this.clauses;
        if (clauses == null)
            return false;
        for (ClauseInfo clause : clauses) {
            if (goal.unifiable(clause.head)) return true;
        }
        return false;
    }

    private boolean unifiable(Term goal, Deque<ClauseInfo> other) {
        //TODO if (!goal.isGround() && clauses instanceof ClauseSet) { .. //fast constant lookup
        Deque<ClauseInfo> clauses = null;
        for (ClauseInfo ci : other) {
            if (goal.unifiable(ci.head)) {
                if (clauses == null)
                    clauses = new ArrayDeque<>(4);
                clauses.add(ci);
            }
        }
        if (clauses == null) {
            return false;
        } else {
            this.clauses = clauses;
            return true;
        }
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
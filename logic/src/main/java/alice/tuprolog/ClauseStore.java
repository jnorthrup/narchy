package alice.tuprolog;

import alice.util.OneWayList;
import jcog.data.list.FasterList;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;
import java.util.List;
import java.util.ListIterator;

/**
 * A list of clauses belonging to the same family as a goal. A family is
 * composed by clauses with the same functor and arity.
 */
public final class ClauseStore {


    private OneWayList<ClauseInfo> clauses;
    private final Term goal;
    private final List<Var> vars;
    private boolean haveAlternatives;

    private ClauseStore(Term goal, List<Var> vars) {
        this.goal = goal;
        this.vars = vars;
        clauses = null;
    }










    public static ClauseStore build(Term goal, List<Var> vars, Deque<ClauseInfo> familyClauses) {
        ClauseStore clauseStore = new ClauseStore(goal, vars);
        return (clauseStore.clauses = OneWayList.get(familyClauses)) == null ||
                !clauseStore.existCompatibleClause() ? null
                :
                clauseStore;
    }


    /**
     * Restituisce la clausola da caricare
     */
    public ClauseInfo fetch() {
        OneWayList<ClauseInfo> clauses = this.clauses;
        if (clauses == null) return null;
        deunify(vars, null);
        if (!checkCompatibility(goal))
            return null;
        ClauseInfo clause = this.clauses.head;
        this.clauses = this.clauses.tail;
        haveAlternatives = checkCompatibility(goal);
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
    protected boolean existCompatibleClause() {
        int n = vars.size();

        List<Term> saveUnifications = n > 0 ? deunify(vars, new FasterList<>(n)) : null;

        boolean found = checkCompatibility(goal);

        if (n > 0)
            reunify(vars, saveUnifications, n);

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
                saveUnifications.add(v.link());
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

        ListIterator<Var> it1 = varsToReunify.listIterator(size);
        ListIterator<Term> it2 = saveUnifications.listIterator(size);
        while (it1.hasPrevious()) {
            it1.previous().setLink(it2.previous());
        }
    }


    /**
     * Verify if a clause exists that is compatible with goal.
     * As a side effect, clauses that are not compatible get
     * discarded from the currently examined family.
     *
     * @param goal
     */
    private boolean checkCompatibility(Term goal) {
        OneWayList<ClauseInfo> clauses = this.clauses;
        ClauseInfo clause;
        while (clauses!=null) {
            clause = clauses.head;
            if (goal.unifiable(clause.head)) return true;
            this.clauses = clauses = this.clauses.tail;
        }
        return false;
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
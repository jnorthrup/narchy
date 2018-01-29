package alice.tuprolog;


public class SubGoalStore {

    private SubGoalTree goals;
    private SubGoalTree commaStruct;
    private int index;
    private SubGoal curSGId;
    private boolean fetched;

    public SubGoalStore(SubGoalTree subTrees) {
        commaStruct = goals = new SubGoalTree();
        index = 0;
        curSGId = null;
        load(subTrees);
    }

    /**
     *
     */
    public boolean load(SubGoalTree subGoals) {
        commaStruct = subGoals;
        goals=commaStruct.copy();
        return true;

    }

    /**
     * Ripristina ClauseStore allo stato i-esimo
     */
    public Term backTo(SubGoal identifier) {
        popSubGoal(identifier);
        index--;
        return fetch();
    }

    public void pushSubGoal(SubGoalTree subGoals) {
        curSGId = new SubGoal(curSGId, commaStruct, index);
        commaStruct = subGoals;
        goals = commaStruct.copy();
        index = 0;
    }

    private void popSubGoal(SubGoal id) {
        commaStruct = id.root;
        goals = commaStruct.copy();
        index = id.index;
        curSGId = id.parent;
    }

    /**
     * Restituisce la clausola da caricare
     */
    public Term fetch() {
        while (true) {
            fetched = true;
            if (index >= commaStruct.size()) {
                if (curSGId == null) {
                    return null;
                } else {
                    popSubGoal(curSGId);

                }
            } else {

                int i = index++;
                SubTree s = commaStruct.get(i);
                if (s instanceof SubGoalTree) {
                    pushSubGoal((SubGoalTree) s);
                } else {
                    return (Term) s;
                }

            }
        }
    }

    /**
     * Indice del correntemente in esecuzione
     */
    public SubGoal getCurrentGoalId() {
        return new SubGoal(curSGId, commaStruct, index);
    }

    public boolean haveSubGoals() {
        return (index < goals.size());
    }

    public String toString() {
        return "goals: " + goals + ' '
                + "index: " + index;
    }

    /*
     * Methods for spyListeners
     */
    public SubGoalTree getSubGoals() {
        return goals;
    }

    public int getIndexNextSubGoal() {
        return index;
    }
    public boolean getFetched(){
        return fetched;
    }
    public SubGoal getCurSGId() {
        return curSGId;
    }
        
}

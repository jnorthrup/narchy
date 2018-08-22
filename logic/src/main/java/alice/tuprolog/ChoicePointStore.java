package alice.tuprolog;

public class ChoicePointStore {
    
    
    private ChoicePointContext pointer;
    
    public ChoicePointStore() {
        pointer = null;
    }
    
    public void add(ChoicePointContext cpc) {
        if (pointer == null) {
            pointer = cpc;
            return;
        }
        cpc.prevChoicePointContext = pointer;
        pointer = cpc;
    }
    
    public void cut(ChoicePointContext pointerAfterCut) {
        pointer = pointerAfterCut;
    }
    
    /**
     * Return the correct choice-point
     */
    public ChoicePointContext fetch() {
        return (existChoicePoint()) ? pointer : null;
    }
    
    /**
	 * Return the actual choice-point store
	 * @return
	 */
    public ChoicePointContext getPointer() {
        return pointer;
    }
    















    /**
     * Check if a choice point exists in the store.
     * As a side effect, removes choice points which have been already used and are now empty.
     * @return
     */
    protected boolean existChoicePoint() {
        ChoicePointContext pointer = this.pointer;
        while (pointer!=null) {
            if (pointer.compatibleGoals.existCompatibleClause())
                return true;
            this.pointer = pointer = pointer.prevChoicePointContext;
        }
        return false;
    }


    /**
     * Removes choice points which have been already used and are now empty.
     */
    protected void removeUnusedChoicePoints() {
        
        existChoicePoint();
    }
    
    /**
     * Cut at defined depth (toDepth)
     */





    
    public String toString(){
        return pointer + "\n";
    }
    
    /*
     * Methods for spyListeners
     */
    
//    public List<ChoicePointContext> getChoicePoints() {
//        ArrayList<ChoicePointContext> l = new ArrayList<>();
//        ChoicePointContext t = pointer;
//        while (t != null) {
//            l.add(t);
//            t = t.prevChoicePointContext;
//        }
//        return l;
//    }
    
}
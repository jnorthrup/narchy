package jcog.pri;

/**
 * reverse osmosis read-only budget
 */
public final class PriRO implements Prioritizable {

    private final float pri;

    public PriRO(float pri) {
        this.pri = pri;
    }

    @Override
    public final boolean isDeleted() {
        return false;
    }



    @Override
    public boolean delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public float pri(float p) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final float pri() {
        return pri;
    }


    /**
     * Fully display the BudgetValue
     *
     * @return String representation of the value
     */
    
    @Override
    public final String toString() {
        return getBudgetString();
    }

}

package jcog.constraint.discrete.search;

public class SearchStats {
    public long startTime;
    public boolean completed;
    public int nNodes;
    public int nFails;
    public int nSolutions;

    @Override
    public String toString() {
        StringBuilder bf = new StringBuilder();
        bf.append(completed ? "Complete search\n" : "Incomplete search\n");
        bf.append("#solutions  : ").append(nSolutions).append('\n');
        bf.append("#nodes      : ").append(nNodes).append('\n');
        bf.append("#fails      : ").append(nFails).append('\n');
        return bf.toString();
    }
}

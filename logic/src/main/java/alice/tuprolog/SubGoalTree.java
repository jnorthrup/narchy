package alice.tuprolog;

import jcog.data.list.FasterList;

import java.util.Iterator;
import java.util.List;


public final class SubGoalTree extends FasterList<SubTree> implements SubTree {

    public SubGoalTree() {
        super();
    }

    public SubGoalTree(List<SubTree> terms) {
        super(terms);
    }

    public SubGoalTree addChild() {
        SubGoalTree r = new SubGoalTree();
        add(r);
        return r;
    }


    @Override
    public final boolean isLeaf() { return false; }

    public String toString() {
        String result = " [ ";
        Iterator<SubTree> i = iterator();
        if (i.hasNext())
            result += i.next().toString();
        while (i.hasNext()) {
            result += " , " + i.next();
        }
        return result + " ] ";
    }


}

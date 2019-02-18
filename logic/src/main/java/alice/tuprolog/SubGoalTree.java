package alice.tuprolog;

import jcog.data.list.FasterList;

import java.util.Iterator;


public final class SubGoalTree extends FasterList<SubTree> implements SubTree {

    public SubGoalTree() {
        super(1);
    }

    public SubGoalTree(Term body) {
        this();
        while (body instanceof Struct) {
            Struct s = (Struct) body;
            if (!s.name().equals(","))
                break;

            Term t = s.sub(0);
            if (t instanceof Struct && ((Struct) t).name().equals(",")) {
                addChild(t);
            } else {
                add(t);
            }
            body = s.sub(1);
        }
        add(body);
    }

    void addChild(Term t) {
        add(new SubGoalTree(t));
    }

    public SubGoalTree addChild() {
        SubGoalTree r = new SubGoalTree();
        add(r);
        return r;
    }


    @Override
    public final boolean isLeaf() { return false; }

    public String toString() {
        StringBuilder result = new StringBuilder(" [ ");
        Iterator<SubTree> i = iterator();
        if (i.hasNext())
            result.append(i.next());
        while (i.hasNext()) {
            result.append(" , ").append(i.next());
        }
        return result + " ] ";
    }



}

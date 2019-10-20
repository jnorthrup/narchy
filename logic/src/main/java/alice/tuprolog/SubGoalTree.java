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
            var s = (Struct) body;
            if (!",".equals(s.name()))
                break;

            var t = s.sub(0);
            if (t instanceof Struct && ",".equals(((Struct) t).name())) {
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
        var r = new SubGoalTree();
        add(r);
        return r;
    }


    @Override
    public final boolean isLeaf() { return false; }

    public String toString() {
        var result = new StringBuilder(" [ ");
        var i = iterator();
        if (i.hasNext())
            result.append(i.next());
        while (i.hasNext()) {
            result.append(" , ").append(i.next());
        }
        return result + " ] ";
    }



}

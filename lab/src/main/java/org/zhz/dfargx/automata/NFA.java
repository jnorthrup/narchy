package org.zhz.dfargx.automata;

import jcog.data.list.FasterList;
import org.zhz.dfargx.node.*;

import java.util.List;
import java.util.Stack;

import static org.zhz.dfargx.automata.NFAState.create;

/**
 * Created on 2015/5/10.
 */
public class NFA     { 

    private final Stack<NFAState> stateStack;

    public final List<NFAState> states;

    public NFA(Node root) {
        super();

        states = new FasterList();
        var initState = newState();
        var finalState = newState();
        stateStack = new Stack<>();
        stateStack.push(finalState);
        stateStack.push(initState);
        dfs(root);
    }

    private NFAState newState() {
        var nfaState = create();
        states.add(nfaState);
        return nfaState;
    }

    private void dfs(Node node) {
        node.accept(this);
        if (node.hasLeft()) {
            dfs(node.left());
            dfs(node.right());
        }
    }

    public void visit(LChar lChar) {
        var ss = this.stateStack;
        var i = ss.pop();
        var f = ss.pop();
        i.transitionRule(lChar.c, f);
    }

    public void visit(LNull lNull) {
        
    }

    public void visit(BOr bOr) {
        var ss = this.stateStack;
        var i = ss.pop();
        var f = ss.pop();
        ss.push(f);
        ss.push(i);
        ss.push(f);
        ss.push(i);
    }

    public void visit(BConcat bConcat) {
        var ss = this.stateStack;
        var i = ss.pop();
        var f = ss.pop();
        var n = newState();
        ss.push(f);
        ss.push(n);
        ss.push(n);
        ss.push(i);
    }

    public void visit(BMany bMany) {
        var ss = this.stateStack;
        var i = ss.pop();
        var f = ss.pop();
        var n = newState();
        i.directRule(n);
        n.directRule(f);
        ss.push(n);
        ss.push(n);
    }

    public void visit(LClosure lClosure) {
        var ss = this.stateStack;
        var i = ss.pop();
        var f = ss.pop();
        i.directRule(f);
    }
}

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
        NFAState initState = newState();
        NFAState finalState = newState();
        stateStack = new Stack<>();
        stateStack.push(finalState);
        stateStack.push(initState);
        dfs(root);
    }

    private NFAState newState() {
        NFAState nfaState = create();
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
        Stack<NFAState> ss = this.stateStack;
        NFAState i = ss.pop();
        NFAState f = ss.pop();
        i.transitionRule(lChar.c, f);
    }

    public void visit(LNull lNull) {
        
    }

    public void visit(BOr bOr) {
        Stack<NFAState> ss = this.stateStack;
        NFAState i = ss.pop();
        NFAState f = ss.pop();
        ss.push(f);
        ss.push(i);
        ss.push(f);
        ss.push(i);
    }

    public void visit(BConcat bConcat) {
        Stack<NFAState> ss = this.stateStack;
        NFAState i = ss.pop();
        NFAState f = ss.pop();
        NFAState n = newState();
        ss.push(f);
        ss.push(n);
        ss.push(n);
        ss.push(i);
    }

    public void visit(BMany bMany) {
        Stack<NFAState> ss = this.stateStack;
        NFAState i = ss.pop();
        NFAState f = ss.pop();
        NFAState n = newState();
        i.directRule(n);
        n.directRule(f);
        ss.push(n);
        ss.push(n);
    }

    public void visit(LClosure lClosure) {
        Stack<NFAState> ss = this.stateStack;
        NFAState i = ss.pop();
        NFAState f = ss.pop();
        i.directRule(f);
    }
}

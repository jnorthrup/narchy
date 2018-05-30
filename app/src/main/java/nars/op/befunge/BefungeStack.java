package nars.op.befunge;


import org.jetbrains.annotations.NotNull;

import java.util.Stack;

/**
 * Created by didrik on 30.12.2014.
 */
public class BefungeStack {
    
    Stack<Long> stack;

    public BefungeStack(){
        stack = new Stack();
    }

    void push(Long l){
        stack.push(l);
    }

    @NotNull
    Long pop(){
        return stack.isEmpty() ? 0L : stack.pop();
    }

    @NotNull
    Long peek(){
        return stack.isEmpty() ? 0L : stack.peek();
    }
}

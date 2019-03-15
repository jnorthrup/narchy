package jcog.exe.action;

import java.util.function.Predicate;

/**
 * stochastic action. invoked, no reliable prediction of invocation duration,
 * wraps a PrediTerm.  just runs it and returns boolean result  */
public interface StochAction<X> extends Predicate<X> {


    //TODO
}

package nars.term.util.transform;

/** category of guaranteed-quick and deterministic evaluations (no random, no side effects etc)
 *  more inline than Inline */
public interface InstantFunctor<E> extends InlineFunctor<E> {

}

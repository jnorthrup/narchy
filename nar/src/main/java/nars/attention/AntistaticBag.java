package nars.attention;

import jcog.Skill;
import jcog.pri.PriMap;
import jcog.pri.Prioritizable;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.op.PriMerge;
import nars.term.Term;

/** useful for schedulers.  fast sampling access as tradeoff for expectation of rare updates that
 * might cause re-indexing.
 *
 * antistatic = keep system active
 *
 *
 * */
@Skill("Antistatic_bag") abstract public class AntistaticBag<X extends Prioritizable> extends ArrayBag<Term, X> {

    public AntistaticBag(int capacity) {
        super(PriMerge.replace, capacity, PriMap.newMap(false));
    }

}

package jcog.pri;

/** marker interface for implementations which limit values to the range of 0..1.0 (and deleted, NaN) */
public interface UnitPrioritizable extends Prioritizable {

    /**
     * assumes 1 max value (Plink not NLink)
     */
    default float priAddOverflow(float inc /* float upperLimit=1 */) {

        if (inc <= EPSILON)
            return 0;

        var beforeAfter = priDelta((x, y)-> ((x!=x) ? 0 : x) + y, inc);

        var after = beforeAfter[1];
        var before = beforeAfter[0];
        var delta = (before != before) ? after : (after - before);
        return Math.max(0, inc - delta); //should be >= 0
    }
}

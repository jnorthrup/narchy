package jcog.pri;


import jcog.Skill;
import jcog.Texts;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.Nullable;

/**
 * something which has a priority floating point value
 *      reports a priority scalar value (32-bit float precision)
 *      NaN means it is 'deleted' which is a valid and testable state
 */
@Skill({"Demand", "Microeconomics", "Macroeconomics"})
public interface Prioritized extends Deleteable {

    /**
     * returns the local (cached) priority value
     * if the value is NaN, then it means this has been deleted
     */
    float pri();

    /**
     * common instance for a 'Deleted budget'.
     */
    Prioritized Deleted = new PriRO(Float.NaN);
    /**
     * common instance for a 'full budget'.
     */
    Prioritized One = new PriRO(1f);
    /**
     * common instance for a 'half budget'.
     */
    Prioritized Half = new PriRO(0.5f);
    /**
     * common instance for a 'zero budget'.
     */
    Prioritized Zero = new PriRO(0);


    static String toString(Prioritized b) {
        return toStringBuilder(null, Texts.n4(b.pri())).toString();
    }

    
    static StringBuilder toStringBuilder(@Nullable StringBuilder sb, String priorityString) {
        int c = 1 + priorityString.length();
        if (sb == null)
            sb = new StringBuilder(c);
        else {
            sb.ensureCapacity(c);
        }

        return sb.append('$').append(priorityString);
    }

    
    static Ansi.Color budgetSummaryColor(Prioritized tv) {
        int s = (int) Math.floor(tv.priElseZero() * 5);
        switch (s) {
            default:
                return Ansi.Color.DEFAULT;
            case 1:
                return Ansi.Color.MAGENTA;
            case 2:
                return Ansi.Color.GREEN;
            case 3:
                return Ansi.Color.YELLOW;
            case 4:
                return Ansi.Color.RED;
        }
    }


    default float priElse(float valueIfDeleted) {
        float p = pri();
        return p == p ? p : valueIfDeleted;
    }

    default float priElseZero() {
        return priElse(0);
    }

    default float priElseNeg1() {
        return priElse(-1);
    }

    /** deleted if pri()==NaN */
    @Override default boolean isDeleted() {
        float p = pri();
        return p!=p; 
    }

    default String getBudgetString() {
        return Prioritized.toString(this);
    }


//    static float sum(Prioritized... src) {
//        return Util.sum(Prioritized::priElseZero, src);
//    }
//    static float max(Prioritized... src) {
//        return Util.max(Prioritized::priElseZero, src);
//    }
//
//    static <X extends Prioritizable> void normalize(X[] xx, float target) {
//        int l = xx.length;
//        assert (target == target);
//        assert (l > 0);
//
//        float ss = sum(xx);
//        if (ss <= ScalarValue.EPSILON)
//            return;
//
//        float factor = target / ss;
//
//        for (X x : xx)
//            x.priMult(factor);
//
//    }
}

package nars.task.util;

import jcog.math.Longerval;
import nars.NAR;
import nars.Task;

public class PredictionFeedback {

    

    static final boolean delete =
            true;
            









































    /**
     * TODO handle stretched tasks
     */
    public static boolean absorbNonSignal(Task y, long seriesStart, long seriesEnd, NAR nar) {

        long end = y.end();
        if (end >= seriesEnd)
            return false; 

        if (Longerval.intersectLength(y.start(), end, seriesStart, seriesEnd)!=-1) {
            

            return true;
        }

        return false;











    }




















































































}

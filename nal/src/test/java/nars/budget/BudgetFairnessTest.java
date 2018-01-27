//package nars.budget;
//
//import jcog.math.MultiStatistics;
//import nars.term.Termed;
//
///**
// * Created by me on 1/12/17.
// */
//public class BudgetFairnessTest {
//
//
//    private static <X extends Termed> MultiStatistics.BooleanClassifierWithStatistics<X> complexityLTE(int complexityLTE) {
//        return new MultiStatistics.BooleanClassifierWithStatistics<X>("complexity<=" + complexityLTE, t -> t.complexity() <= complexityLTE);
//    }
//    private static <X extends Termed> MultiStatistics.BooleanClassifierWithStatistics<X> volumeLTE(int complexityLTE) {
//        return new MultiStatistics.BooleanClassifierWithStatistics<X>("volume<=" + complexityLTE, t -> t.volume() <= complexityLTE);
//    }
//    private static <X extends Termed> MultiStatistics.BooleanClassifierWithStatistics<X> volumeIn(int min, int max) {
//        return new MultiStatistics.BooleanClassifierWithStatistics<X>("volume=" + min + ".." + max, t -> {
//            int v = t.volume();
//            return (v >= min && v <= max);
//        });
//    }
//}

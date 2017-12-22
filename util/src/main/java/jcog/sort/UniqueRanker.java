//package jcog.sort;
//
//import org.eclipse.collections.api.block.function.primitive.FloatFunction;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.Comparator;
//
//public class UniqueRanker<T> implements Comparator<T> {
//
//    private final FloatFunction<T> function;
//
//    public UniqueRanker(@NotNull FloatFunction<T> function) {
//        this.function = function;
//    }
//
//    @Override
//    public int compare(@NotNull T o1, @NotNull T o2) {
//
//
//        if (o1.equals(o2)) return 0;
//
//        float one = this.function.floatValueOf(o1);
//        float two = this.function.floatValueOf(o2);
//
//        //System.out.println("compare: " + o1 + "=" + one + "\t" + o2 + "=" + two);
//
//        int x = Float.compare(one, two);
//        if (x == 0) {
//
//            int y = Integer.compare(o1.hashCode(), o2.hashCode());
//            if (y == 0) {
//                return Integer.compare(System.identityHashCode(o1), System.identityHashCode(o2));
//            }
//            return y;
//        }
//        return x;
//    }
//
//}

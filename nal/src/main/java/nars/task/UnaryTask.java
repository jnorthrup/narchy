//package nars.task;
//
//import jcog.Texts;
//import jcog.pri.PLink;
//import org.jetbrains.annotations.NotNull;
//
///**
// * task which is identified by one value parameter, and the class type itself
// */
//abstract public class UnaryTask<X> extends PLink<X> implements ITask {
//
//
//    private final int hash;
//
//    protected UnaryTask(X id, float pri) {
//        super(id, pri);
//        this.hash = id.hashCode();
//    }
//
//
//    @Override
//    public final boolean equals(Object obj) {
//        return
//            (this == obj)
//                ||
//            (hash == obj.hashCode()) && ((obj.getClass() == getClass()) && id.equals(((PLink) obj).id));
//    }
//
//
//    @Override
//    public final @NotNull String toString() {
//        return '$' + Texts.n4(pri) + ' ' + getClass().getSimpleName() + '(' + id + ')';
//    }
//
//    @Override
//    public final int hashCode() {
//        return hash;
//    }
//
//}

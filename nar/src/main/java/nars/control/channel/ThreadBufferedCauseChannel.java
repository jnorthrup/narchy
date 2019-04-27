//package nars.control.channel;
//
//import jcog.pri.Priority;
//
//import java.util.Collection;
//import java.util.Iterator;
//import java.util.List;
//import java.util.stream.Stream;
//
//public class ThreadBufferedCauseChannel<X extends Priority> extends ThreadLocal<BufferedCauseChannel> {
//    private final CauseChannel<X> target;
//    public short id;
//
//    public ThreadBufferedCauseChannel(CauseChannel<X> c) {
//        super();
//        this.target = c;
//        this.id = target.cause.id;
//    }
//
//    public float value() {
//        return target.value();
//    }
//
//    @Override
//    protected BufferedCauseChannel initialValue() {
//        return new BufferedCauseChannel(target);
//    }
//
//    public void input(X x) {
//        get().input(x);
//    }
//    public void input(X... x) { get().input((Object[])x);    }
//    public void input(Iterable<? extends X> x) {
//        get().input(x);
//    }
//    public void input(Iterator<? extends X> x) {
//        get().input(x);
//    }
//    public void input(Stream<? extends X> x) {
//        get().input(x);
//    }
//    public void input(Collection<? extends X> x) {
//        get().input(x);
//    }
//    public void input(List<? extends X> x) {
//        get().input(x);
//    }
//
//}

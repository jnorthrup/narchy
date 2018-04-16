//package nars.nal.meta.op;
//
//import nars.term.Term;
//import org.jetbrains.annotations.NotNull;
//
///**
// * Created by me on 12/17/15.
// */
//public final class TermSizeEquals extends MatchOp {
//    public final int size;
//
//    public TermSizeEquals(int size) {
//        this.size = size;
//    }
//
//    @Override
//    public boolean match(@NotNull Term t) {
//        return t.size() == size;
//    }
//
//    @NotNull
//    @Override
//    public String toString() {
//        return "size=" + size;
//    }
// }

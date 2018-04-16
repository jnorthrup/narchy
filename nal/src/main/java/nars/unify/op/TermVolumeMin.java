//package nars.nal.meta.op;
//
//import nars.term.Term;
//import org.jetbrains.annotations.NotNull;
//
///**
// * Created by me on 12/17/15.
// */
//public final class TermVolumeMin extends MatchOp {
//    public final int volume;
//
//    public TermVolumeMin(int volume) {
//        this.volume = volume;
//    }
//
//    @Override
//    public boolean match(@NotNull Term t) {
//        return t.volume() >= volume;
//    }
//
//    @NotNull
//    @Override
//    public String toString() {
//        return "vol>=" + volume;
//    }
// }

//package nars.truth;
//
///**
// * Records a change in truth state from before and after a task is inserteed into a belief table.
// * Used for accurate feedback measurements.
// * Recorded during a synchronized block to ensure that it assigns the credit to the inserted task accurately.
// */
//public class TruthDelta {
//
//    public final static TruthDelta zero = new TruthDelta(new DiscreteTruth(0.5f, 0.5f), new DiscreteTruth(0.5f, 0.5f));
//
//    public final Truth before, after;
//
//    public TruthDelta(Truth beliefBefore, Truth beliefAfter) {
//        this.before = beliefBefore;
//        this.after = beliefAfter;
//    }
//
//    @Override
//    public String toString() {
//        return before + " -> " + after;
//    }
//}

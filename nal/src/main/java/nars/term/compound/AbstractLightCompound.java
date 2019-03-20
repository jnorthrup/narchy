//package nars.term.compound;
//
//import nars.subterm.Subterms;
//import nars.term.Compound;
//
//import static nars.time.Tense.DTERNAL;
//
//public interface AbstractLightCompound extends Compound {
//
//    @Override
//    boolean equals(Object obj);
//
//    @Override
//    int hashCode();
//
//    String toString();
//
//
//    @Override
//    Subterms subterms();
//
//
//
//    @Override
//    default boolean the() {
//        //throw new TODO();
//        //return op().the(dt(), arrayShared());
//        return false;
//    }
//
//    @Override
//    default int dt() {
//        return DTERNAL;
//    }
//
//}

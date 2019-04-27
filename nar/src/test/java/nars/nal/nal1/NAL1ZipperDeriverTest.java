//package nars.nal.nal1;
//
//import nars.NAR;
//import nars.NARS;
//import nars.derive.Derivers;
//import nars.derive.impl.ZipperDeriver;
//
///** NAL1 tests solved using the alternate SimpleDeriver (not MatrixDeriver) */
//public class NAL1ZipperDeriverTest extends NAL1Test {
//
//    @Override protected NAR nar() {
//        cycles = 1000;
//        NAR n = NARS.tmp(0);
//        new ZipperDeriver(Derivers.nal(n, 1, 1));
//        return n;
//    }
//
//}

package nars.nal.nal1;

import nars.NAR;
import nars.NARS;
import nars.derive.Derivers;
import nars.derive.impl.SimpleDeriver;

/** NAL1 tests solved using the alternate SimpleDeriver (not MatrixDeriver) */
public class NAL1SimpleDeriverTest extends NAL1Test {

    @Override protected NAR nar() {
        NAR n = NARS.tmp(0);
        new SimpleDeriver(Derivers.nal(n, 1, 1));
        return n;
    }

}

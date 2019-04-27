package nars.term.compound;

import nars.NAR;
import nars.NARS;
import nars.nal.nal1.NAL1Test;
import org.junit.jupiter.api.Disabled;

/** runs NAL1Test with FastCompound's instead of GenericCompound's */
@Disabled
public class FastCompoundNAL1Test extends NAL1Test {

    @Override
    protected NAR nar() {
        
        return NARS.tmp(1);
    }
}

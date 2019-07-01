package nars;

import com.google.common.util.concurrent.MoreExecutors;
import nars.time.clock.CycleTime;

import java.util.concurrent.ThreadLocalRandom;

public class DummyNAL extends NAL {

    public DummyNAL() {
        super(MoreExecutors.directExecutor(), new CycleTime(), ThreadLocalRandom::current);
    }

    @Override
    public float dur() {
        return 1;
    }

    @Override
    public long time() {
        return 0;
    }
}

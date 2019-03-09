package nars.nal.nal7;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.task.util.TaskBuffer;
import org.junit.jupiter.api.Test;

class NAL7RealtimeTest {
    @Test
    void test1() throws Narsese.NarseseException {
        
        NAR n = NARS.realtime(1f).withNAL(new TaskBuffer.MapTaskBuffer(64),1,8).get();
        
        n.input("wake. |");
        n.input("(wake &&+16hours sleep).");
        n.input("(sleep &&+8hours wake).");
        n.input("(wake ==>+30min awake).");
        n.input("(sleep ==>+30min asleep).");
        n.input("(--(&|,wake,asleep) && --(&|, --wake,--sleep))."); 
        n.run(100);
    }
    @Test
    void test2() throws Narsese.NarseseException {
        NAR n = NARS.realtime(1f).withNAL(new TaskBuffer.MapTaskBuffer(64),1,8).get();

        //n.log();
        n.input("(a &&+1min b).");
        n.input("(b &&+1min c).");
        n.input("(c &&+1min a).");
        n.run(100);
    }
}

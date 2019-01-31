package nars.nal.nal8;


import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Test;

class MathTest {








    @Test
    void testImplVarAdd1() throws Narsese.NarseseException {

        NAR t = new NARS().get();
        
        
        
        t.input("i:{1}.");
        t.input("i:{2}.");
        t.input("i:{4}.");
        t.input("((&&,({$x} --> i),({$y} --> i)) ==> ({($x,$y),($y,$x)} --> j)).");
        t.run(100);

        t.input("(({(#x,#y)} --> j) ==> ({addAt(#x,#y)} --> i)).");
        t.run(100);

        
        
        
        
        
        
        

    }
}

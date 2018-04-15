package nars.nal.nal8;


import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Test;

public class MathTest {

//    @Test
//    public void testAdd1() {
//        Terminal t = NARS.shell();
//        t.log();
//        t.input("(add(1,2,?x)<->result).");
//        t.next();
//    }
    @Test
    public void testImplVarAdd1() throws Narsese.NarseseException {

        NAR t = new NARS().get();
        //t.log();
        //t.input("i:{0,1,2,3,4}.");
        //t.input("i:{0,1,2}.");
        t.input("i:{1}.");
        t.input("i:{2}.");
        t.input("i:{4}.");
        t.input("((&&,({$x} --> i),({$y} --> i)) ==> ({($x,$y),($y,$x)} --> j)).");
        t.run(100);

        t.input("(({(#x,#y)} --> j) ==> ({add(#x,#y)} --> i)).");
        t.run(100);

        //t.input("(({#x,#y} --> i) && (add(#x,#y)<->sum)).");
        //t.input("(({$x,$y} --> i) ==> (add($x,$y)<->sum)).");
        //t.input("(&&,({$x} --> i),({$y} --> i),({add($x,$y)}-->i)).");
        ///t.input("(((#x --> i)&&(#y --> i)) ==> (add(#x,#y)<->sum)).");
        //t.input("((($x --> i)&&($y --> i)) ==> (add($x,$y)<->sum)).");
        //t.input("(({?x,?y} --> i) ==> (add(?x,?y)<->sum))?");
        //t.input("({?x}-->i)?");

    }
}

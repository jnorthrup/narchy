//package nars;
//
//import nars.nar.Default;
//import nars.op.shell;
//import org.junit.jupiter.api.Test;
//
///**
// * Created by me on 2/24/16.
// */
//public class ShellTest {
//
//    static {
//        Global.DEBUG = true;
//    }
//    final NAR d = new Default(1024, 8, 2, 3);
//    final shell s = new shell(d);
//
//    public ShellTest() throws Exception {
//    }
//
//    @Test
//    public void testLobjectized()  {
//
//        //assertTrue(d.exe.containsKey($.operator("sh")));
//
//        d.log();
//        d.input("sh(pwd,I,(),#x)!");
//        d.run(5);
//        //expect: ("file:///home/me/opennars/nars_logic"-->(/,^sh,pwd,I,(),_)). :|: %1.0;.90%
//    }
//
//    @Test
//    public void testWrappedDirectoryConcept()  {
//
//        //assertTrue(d.exe.containsKey($.operator("sh")));
//
//        d.log();
//
//        d.input("(go($w) <=> sh($w,I,(),#z)). %1.0;0.95%");
//        d.input("go(ls)! :|:");
//
//        d.run(16);
//        //expect: ("file:///home/me/opennars/nars_logic"-->(/,^sh,pwd,I,(),_)). :|: %1.0;.90%
//    }
//}













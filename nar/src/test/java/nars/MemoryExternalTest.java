//package nars;
//
//import nars.term.Term;
//import org.junit.jupiter.api.Test;
//
//import static java.util.stream.Collectors.toList;
//import static nars.$.$$;
//
//class MemoryExternalTest {
//
//    @Test
//    void testURLDirectory() {
//        MemoryExternal m = new MemoryExternal();
//        Term url = $$("file:///tmp");
//        System.out.println(
//            m.contents(url).collect(toList())
//        );
//    }
//}
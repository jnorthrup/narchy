package nars;

import nars.term.Term;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toList;
import static nars.$.$$;

class MemoryTest {

    @Test
    void testURLDirectory() {
        Memory m = new Memory();
        Term url = $$("file:///tmp");
        System.out.println(
            m.contents(url).collect(toList())
        );
    }
}
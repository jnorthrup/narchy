package nars;

import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toList;
import static nars.$.$$;

class MemoryTest {

    @Test
    public void testURLDirectory() {
        Memory m = new Memory();
        System.out.println(
            m.contents($$("file:///tmp")).collect(toList())
        );
    }
}
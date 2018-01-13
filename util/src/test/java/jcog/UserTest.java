package jcog;

import jcog.list.FasterList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    @Test
    public void testPutGetString() {
        testPutGet("value");
    }

    @Test
    public void testPutGetByteArray() {
        testPutGet(new byte[]{1, 2, 3, 4, 5});
    }

    @Test
    public void testPutGetSerialized() {
        testPutGet(Maps.mutable.of("x", 6, "y", Lists.mutable.of("z", "z")));
    }

    static User testPutGet(Object obj) {
        User u = new User();
        u.put("key", obj);
        List l = new FasterList();
        u.get("key", l::add);
        assertEquals(1, l.size());
        if (obj instanceof byte[]) {
            assertArrayEquals((byte[]) obj, (byte[]) l.get(0));
        } else {
            assertEquals(obj, l.get(0));
        }
        return u;
    }
}
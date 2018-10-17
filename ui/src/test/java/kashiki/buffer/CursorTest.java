package kashiki.buffer;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class CursorTest {

  @Test
  public void testCompareTo() {
    assertEquals(0, new CursorPosition(10, 10).compareTo(new CursorPosition(10, 10)));
    assertEquals(-1, new CursorPosition(10, 10).compareTo(new CursorPosition(10, 11)));
    assertEquals(1, new CursorPosition(10, 10).compareTo(new CursorPosition(10, 9)));

    assertEquals(-1, new CursorPosition(10, 10).compareTo(new CursorPosition(11, 10)));
    assertEquals(1, new CursorPosition(10, 10).compareTo(new CursorPosition(9, 10)));
  }

}

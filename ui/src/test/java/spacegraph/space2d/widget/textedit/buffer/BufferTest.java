package spacegraph.space2d.widget.textedit.buffer;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BufferTest {

  private Buffer buf= new Buffer("testBuffer", "");;


  @Test
  public void initialState() {
    assertEquals(buf.getLines().size(), (1));
    assertEquals(buf.getLines().get(0).getLength(), (0));
    assertEquals(buf.getCaret(), (new CursorPosition(0, 0)));
    buf.forward();
    assertEquals(buf.getCaret(), (new CursorPosition(0, 0)));
    buf.back();
    assertEquals(buf.getCaret(), (new CursorPosition(0, 0)));
    buf.previous();
    assertEquals(buf.getCaret(), (new CursorPosition(0, 0)));
    buf.next();
    assertEquals(buf.getCaret(), (new CursorPosition(0, 0)));
  }

  @Test
  public void insertString1() {
    buf.insertString("abcde");
    assertEquals(buf.getCaret(), (new CursorPosition(0, 5)));
    buf.insertEnter();
    assertEquals(buf.getCaret(), (new CursorPosition(1, 0)));
  }

  @Test
  public void insertCRLF() {
    buf.insertString("\r\n");
    assertEquals(buf.getCaret(), (new CursorPosition(1, 0)));
  }

  @Test
  public void insertLF() {
    buf.insertString("\n");
    assertEquals(buf.getCaret(), (new CursorPosition(1, 0)));
  }

  @Test
  public void insertEmpty() {
    buf.insertString("");
    assertEquals(buf.getCaret(), (new CursorPosition(0, 0)));
  }

  @Test
  public void insertEnter() {
    buf.insertString("abcde\n12345\n");
    assertEquals(buf.getCaret(), (new CursorPosition(2, 0)));
    buf.back();
    assertEquals(buf.getCaret(), (new CursorPosition(1, 5)));
    buf.head();
    buf.previous();
    assertEquals(buf.getCaret(), (new CursorPosition(0, 0)));
    buf.forward();
    buf.forward();
    buf.insertEnter();
    assertEquals(buf.getCaret(), (new CursorPosition(1, 0)));
    assertEquals(buf.toBufferString(), ("ab\ncde\n12345\n"));
  }

  @Test
  public void backspace() {
    buf.insertString("abcde\n12345\n");
    assertEquals(buf.getCaret(), (new CursorPosition(2, 0)));
    buf.backspace();
    assertEquals(buf.toBufferString(), ("abcde\n12345"));
    for (int i = 0; i < 5; i++) {
      buf.backspace();
    }
    assertEquals(buf.toBufferString(), ("abcde\n"));
    for (int i = 0; i < 5; i++) {
      buf.backspace();
    }
    assertEquals(buf.toBufferString(), ("a"));
    buf.backspace();
    assertEquals(buf.toBufferString(), (""));
  }

  @Test
  public void delete() {
    buf.insertString("abcde\n12345\n");
    assertEquals(buf.getCaret(), (new CursorPosition(2, 0)));
    buf.previous();
    buf.forward();
    buf.forward();
    assertEquals(buf.getCaret(), (new CursorPosition(1, 2)));
    buf.delete();
    buf.delete();
    buf.delete();
    assertEquals(buf.toBufferString(), ("abcde\n12\n"));
    buf.delete();
    buf.delete();
    buf.delete();
    assertEquals(buf.toBufferString(), ("abcde\n12"));
    assertEquals(buf.getCaret(), (new CursorPosition(1, 2)));
    buf.bufferHead();
    buf.delete();
    buf.delete();
    buf.delete();
    buf.delete();
    buf.delete();
    buf.delete();
    assertEquals(buf.toBufferString(), ("12"));
    buf.delete();
    buf.delete();
    assertEquals(buf.toBufferString(), (""));
    buf.delete();
    assertEquals(buf.toBufferString(), (""));
  }

}

package kashiki.view;

import kashiki.buffer.Buffer;
import kashiki.buffer.BufferListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class BufferViewTest {

  private Buffer buffer;

  @BeforeEach
  public void setup() {
    buffer = new Buffer("testBuffer", "あいうえお");

    BufferListener listener = new BufferView(buffer);
    buffer.addListener(listener);
  }

  @Test
  public void test() {
    buffer.back();
    buffer.backspace();
    buffer.delete();
    buffer.insertEnter();
  }

}

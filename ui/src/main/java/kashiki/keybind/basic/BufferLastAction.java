package kashiki.keybind.basic;

import kashiki.Editor;
import kashiki.keybind.Action;

public class BufferLastAction implements Action {

  @Override
  public String name() {
    return "buffer-last";
  }

  @Override
  public void execute(Editor editor, String... args) {
    editor.getCurrentBuffer().bufferLast();
  }

}

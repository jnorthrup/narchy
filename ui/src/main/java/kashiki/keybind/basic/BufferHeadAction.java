package kashiki.keybind.basic;

import kashiki.Editor;
import kashiki.keybind.Action;

public class BufferHeadAction implements Action {

  @Override
  public String name() {
    return "buffer-head";
  }

  @Override
  public void execute(Editor editor, String... args) {
    editor.buffer().bufferHead();
  }

}

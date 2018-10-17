package kashiki.keybind.basic;

import kashiki.Editor;
import kashiki.keybind.Action;

public class NewBufferAction implements Action {

  @Override
  public String name() {
    return "new-buffer";
  }

  @Override
  public void execute(Editor editor, String... args) {
    editor.createNewBuffer();
  }

}

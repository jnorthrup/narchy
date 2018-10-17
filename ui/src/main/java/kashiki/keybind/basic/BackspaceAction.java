package kashiki.keybind.basic;

import kashiki.Editor;
import kashiki.keybind.Action;

public class BackspaceAction implements Action {

  @Override
  public String name() {
    return "backspace";
  }

  @Override
  public void execute(Editor editor, String... args) {
    editor.getCurrentBuffer().backspace();
  }

}

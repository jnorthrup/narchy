package kashiki.keybind.basic;

import kashiki.Editor;
import kashiki.keybind.Action;

public class ReturnAction implements Action {

  @Override
  public String name() {
    return "return";
  }

  @Override
  public void execute(Editor editor, String... args) {
    editor.buffer().insertString("\n");
  }

}

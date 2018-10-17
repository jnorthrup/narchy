package kashiki.keybind.basic;

import kashiki.Editor;
import kashiki.keybind.Action;

public class BackAction implements Action {

  @Override
  public String name() {
    return "back";
  }

  @Override
  public void execute(Editor editor, String... args) {
    editor.getCurrentBuffer().back();
  }

}

package kashiki.keybind.basic;

import kashiki.Editor;
import kashiki.keybind.Action;

public class NextAction implements Action {

  @Override
  public String name() {
    return "next";
  }

  @Override
  public void execute(Editor editor, String... args) {
    editor.buffer().next();
  }

}

package kashiki.keybind.basic;

import kashiki.Editor;
import kashiki.keybind.Action;

public class MarkAction implements Action {

  @Override
  public String name() {
    return "mark";
  }

  @Override
  public void execute(Editor editor, String... args) {
    editor.getCurrentBuffer().mark();
  }
}

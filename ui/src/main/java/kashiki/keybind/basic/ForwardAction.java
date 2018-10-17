package kashiki.keybind.basic;

import kashiki.Editor;
import kashiki.keybind.Action;

public class ForwardAction implements Action {

  @Override
  public String name() {
    return "forward";
  }

  @Override
  public void execute(Editor editor, String... args) {
    editor.getCurrentBuffer().forward();
  }

}

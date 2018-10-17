package kashiki.keybind.basic;

import kashiki.Editor;
import kashiki.keybind.Action;

public class LastAction implements Action {

  @Override
  public String name() {
    return "last";
  }

  @Override
  public void execute(Editor editor, String... args) {
    editor.buffer().last();
  }

}

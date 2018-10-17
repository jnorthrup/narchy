package kashiki.keybind.basic;

import kashiki.Editor;
import kashiki.keybind.Action;

public class PreviousAction implements Action {

  @Override
  public String name() {
    return "previous";
  }

  @Override
  public void execute(Editor editor, String... args) {
    editor.buffer().previous();
  }

}

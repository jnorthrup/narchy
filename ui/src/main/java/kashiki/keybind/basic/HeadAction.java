package kashiki.keybind.basic;

import kashiki.Editor;
import kashiki.keybind.Action;

public class HeadAction implements Action {

  @Override
  public String name() {
    return "head";
  }

  @Override
  public void execute(Editor editor, String... args) {
    editor.buffer().head();
  }

}

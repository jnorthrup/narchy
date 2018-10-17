package kashiki.keybind.basic;

import kashiki.Editor;
import kashiki.keybind.Action;

public class SaveAction implements Action {

  @Override
  public String name() {
    return "save-buffer";
  }

  @Override
  public void execute(Editor editor, String... args) {
    //editor.getCurrentBuffer().save();
    throw new UnsupportedOperationException();
  }

}

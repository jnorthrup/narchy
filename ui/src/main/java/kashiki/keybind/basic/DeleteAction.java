package kashiki.keybind.basic;

import kashiki.Editor;
import kashiki.keybind.Action;

public class DeleteAction implements Action {

  @Override
  public String name() {
    return "delete";
  }

  @Override
  public void execute(Editor editor, String... args) {
    editor.getCurrentBuffer().delete();
  }

}

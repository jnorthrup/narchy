package kashiki.keybind.basic;

import kashiki.Editor;
import kashiki.keybind.Action;

public class ViewRefleshAction implements Action {

  @Override
  public String name() {
    return "view-reflesh";
  }

  @Override
  public void execute(Editor editor, String... args) {
    editor.reflesh();
  }

}

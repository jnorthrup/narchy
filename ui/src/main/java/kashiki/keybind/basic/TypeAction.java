package kashiki.keybind.basic;

import kashiki.Editor;
import kashiki.keybind.Action;

public class TypeAction implements Action {

  @Override
  public String name() {
    return "type";
  }

  @Override
  public void execute(Editor editor, String... args) {
    for (String string : args) {
      editor.getCurrentBuffer().insertString(string);
    }
  }

}

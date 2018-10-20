package spacegraph.space2d.widget.textedit.keybind.basic;

import spacegraph.space2d.widget.textedit.TextEditModel;
import spacegraph.space2d.widget.textedit.keybind.Action;

public class ViewRefleshAction implements Action {

  @Override
  public String name() {
    return "view-reflesh";
  }

  @Override
  public void execute(TextEditModel editor, String... args) {
    editor.reflesh();
  }

}

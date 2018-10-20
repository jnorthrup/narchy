package spacegraph.space2d.widget.textedit.keybind.basic;

import spacegraph.space2d.widget.textedit.TextEditModel;
import spacegraph.space2d.widget.textedit.keybind.Action;
import spacegraph.space2d.widget.textedit.view.SmoothValue;

public class ViewScaleDownAction implements Action {

  @Override
  public String name() {
    return "view-scale-down";
  }

  @Override
  public void execute(TextEditModel editor, String... args) {
    SmoothValue scale = editor.scale();
    scale.set(scale.getLastValue() / 1.25);
  }

}

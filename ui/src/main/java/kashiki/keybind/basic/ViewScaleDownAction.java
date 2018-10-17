package kashiki.keybind.basic;

import kashiki.Editor;
import kashiki.keybind.Action;
import kashiki.view.SmoothValue;

public class ViewScaleDownAction implements Action {

  @Override
  public String name() {
    return "view-scale-down";
  }

  @Override
  public void execute(Editor editor, String... args) {
    SmoothValue scale = editor.getScale();
    scale.setValue(scale.getLastValue() / 1.25);
  }

}

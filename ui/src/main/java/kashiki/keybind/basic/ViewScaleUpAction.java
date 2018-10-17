package kashiki.keybind.basic;

import kashiki.Editor;
import kashiki.keybind.Action;
import kashiki.view.SmoothValue;

public class ViewScaleUpAction implements Action {

  @Override
  public String name() {
    return "view-scale-up";
  }

  @Override
  public void execute(Editor editor, String... args) {
    SmoothValue scale = editor.getScale();
    scale.setValue(scale.getLastValue() * 1.25);
  }

}

package kashiki.keybind.demo;

import kashiki.Editor;
import kashiki.keybind.Action;
import kashiki.view.Base;

import java.util.List;

public class YRollPlusAction implements Action {

  @Override
  public String name() {
    return "y-roll-plus";
  }

  @Override
  public void execute(Editor editor, String... args) {
    List<Base> drawables = editor.getDrawables();
    for (Base base : drawables) {
      base.getAngle().getY().addValue(10);
    }
  }

}

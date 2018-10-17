package kashiki.keybind.demo;

import kashiki.Editor;
import kashiki.keybind.Action;
import kashiki.view.Base;

import java.util.List;

public class YRollMinusAction implements Action {

  @Override
  public String name() {
    return "y-roll-minus";
  }

  @Override
  public void execute(Editor editor, String... args) {
    List<Base> drawables = editor.drawables();
    for (Base base : drawables) {
      base.getAngle().getY().addValue(-10);
    }
  }

}

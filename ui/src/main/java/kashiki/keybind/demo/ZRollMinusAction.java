package kashiki.keybind.demo;

import kashiki.Editor;
import kashiki.keybind.Action;
import kashiki.view.Base;

import java.util.List;

public class ZRollMinusAction implements Action {

  @Override
  public String name() {
    return "z-roll-minus";
  }

  @Override
  public void execute(Editor editor, String... args) {
    List<Base> drawables = editor.getDrawables();
    for (Base base : drawables) {
      base.getAngle().getZ().addValue(-10);
    }
  }

}

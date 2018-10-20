package spacegraph.space2d.widget.textedit.keybind.demo;

import spacegraph.space2d.widget.textedit.TextEditModel;
import spacegraph.space2d.widget.textedit.keybind.Action;
import spacegraph.space2d.widget.textedit.view.TextEditView;

import java.util.List;

public class XRollMinusAction implements Action {

  @Override
  public String name() {
    return "x-roll-minus";
  }

  @Override
  public void execute(TextEditModel editor, String... args) {
    List<TextEditView> drawables = editor.drawables();
    for (TextEditView base : drawables) {
      base.angle.getX().add(-10);
    }
  }

}

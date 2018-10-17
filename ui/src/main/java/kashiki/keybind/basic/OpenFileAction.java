package kashiki.keybind.basic;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import kashiki.Editor;
import kashiki.keybind.Action;

import javax.swing.*;
import java.io.IOException;

/*
 * 暫定実装。将来捨てるべき。
 */
public class OpenFileAction implements Action {

  @Override
  public String name() {
    return "open-file";
  }

  @Override
  public void execute(Editor editor, String... args) {
    JFileChooser fileChooser = new JFileChooser();
    int selected = fileChooser.showOpenDialog(null);

    if (selected == JFileChooser.APPROVE_OPTION) {
      try {
        String textString = Files.toString(fileChooser.getSelectedFile(), Charsets.UTF_8);
        editor.createNewBuffer();
        editor.getCurrentBuffer().insertString(textString);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}

package kashiki.keybind.basic;

import com.google.common.base.Charsets;
import kashiki.Editor;
import kashiki.keybind.Action;

import javax.swing.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class SaveFileAction implements Action {

  @Override
  public String name() {
    return "save-file";
  }

  @Override
  public void execute(Editor editor, String... args) {
    JFileChooser fileChooser = new JFileChooser();
    int selected = fileChooser.showSaveDialog(null);
    if (selected == JFileChooser.APPROVE_OPTION) {
      try (OutputStreamWriter writer =
          new OutputStreamWriter(new FileOutputStream(fileChooser.getSelectedFile()),
              Charsets.UTF_8)) {
        editor.buffer().getLines().forEach(l -> {
          try {
            writer.append(l.toLineString());
            writer.append("\n");
          } catch (Exception e) {
            throw new RuntimeException("ファイルの書込みに失敗しました。");
          }
        });
      } catch (IOException e) {
        JOptionPane.showMessageDialog(null, e.getMessage());
      }
    }
  }
}

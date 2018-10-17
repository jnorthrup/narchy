package kashiki.keybind.basic;

import kashiki.Editor;
import kashiki.buffer.Buffer;
import kashiki.keybind.Action;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class KillRingAction implements Action {

  @Override
  public String name() {
    return "kill-ring";
  }

  @Override
  public void execute(Editor editor, String... args) {
    Buffer currentBuffer = editor.getCurrentBuffer();
    if (currentBuffer.isLineLast()) {
      currentBuffer.delete();
    } else {
      currentBuffer.mark();
      currentBuffer.last();
      setClipboardString(currentBuffer.copy());
      currentBuffer.cut();
    }
  }

  private static void setClipboardString(String value) {
    StringSelection selection = new StringSelection(value);

    Toolkit toolKit = Toolkit.getDefaultToolkit();
    Clipboard clipboard = toolKit.getSystemClipboard();
    clipboard.setContents(selection, selection);
  }
}

package kashiki.keybind;

import kashiki.Editor;

public interface Action {
  String name();

  void execute(Editor editor, String... args);
}

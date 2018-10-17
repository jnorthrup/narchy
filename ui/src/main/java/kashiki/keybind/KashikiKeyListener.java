package kashiki.keybind;


public interface KashikiKeyListener {

  boolean keyPressed(SupportKey supportKey, int keyCode, long when);

  boolean keyTyped(char typedString, long when);

}

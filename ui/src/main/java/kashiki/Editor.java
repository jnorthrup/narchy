package kashiki;

import kashiki.buffer.Buffer;
import kashiki.keybind.ActionRepository;
import kashiki.keybind.KashikiKeyListener;
import kashiki.keybind.SupportKey;
import kashiki.view.Base;
import kashiki.view.BufferView;
import kashiki.view.SmoothValue;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Editor implements KashikiKeyListener {



  private Buffer currentBuffer;
  private final Collection<Buffer> buffers = new ArrayList<>();
  private final List<Base> currentDrawables = new ArrayList<>();

  public ActionRepository actionRepository;
  public KashikiKeyListener keyListener;
  private final SmoothValue scale = new SmoothValue(1);
  private Frame frame;

  public Editor() {
    Buffer buf = //new BufferRepository().loadBuffer("scratch");
            new Buffer("", "");
    buffers.add(buf);
    currentBuffer = buf;

    BufferView bufView = new BufferView(buf);
    currentDrawables.add(bufView);
  }


  private KashikiKeyListener currentListener() {
    return keyListener;
  }

  @Override
  public boolean keyPressed(SupportKey supportKey, int keyCode, long when) {
    return currentListener().keyPressed(supportKey, keyCode, when);
  }

  @Override
  public boolean keyTyped(char typedString, long when) {
    return currentListener().keyTyped(typedString, when);
  }

  public void executeAction(String name, String... args) {
    actionRepository.run(this, name, args);
  }

  public Buffer getCurrentBuffer() {
    return this.currentBuffer;
  }

  public List<Base> getDrawables() {
    return currentDrawables;
  }

  public SmoothValue getScale() {
    return this.scale;
  }

  public void createNewBuffer() {
    Buffer buf = new Buffer("scratch-" + System.currentTimeMillis(), "");
    buffers.add(buf);
    currentBuffer = buf;
    currentDrawables.clear();
    BufferView bufView = new BufferView(buf);
    currentDrawables.add(bufView);
  }

  public void reflesh() {
    currentDrawables.clear();
    currentDrawables.add(new BufferView(currentBuffer));
  }

  public void tearDown() {
    // FIXME
  }

  public void toggleFullScreen() {
    GraphicsDevice screenDevice =
        GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    if (screenDevice.isFullScreenSupported()) {
      if (screenDevice.getFullScreenWindow() != null) {
        screenDevice.setFullScreenWindow(null);
      } else {
        screenDevice.setFullScreenWindow(frame);
      }
    }
  }

  public void setFrame(Frame frame) {
    this.frame = frame;
  }
}

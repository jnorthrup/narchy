package spacegraph.space2d.widget.textedit.buffer;


import jcog.data.list.FasterList;

import java.util.Collection;
import java.util.function.Consumer;

public class BufferCharObserver implements Consumer<BufferChar> {

  private final Collection<Consumer<BufferChar>> listeners = new FasterList<>();

  public void addListener(Consumer<BufferChar> listener) {
    listeners.add(listener);
  }

  @Override
  public void accept(BufferChar bc) {
      for (var l : listeners) {
          l.accept(bc);
      }
  }
}

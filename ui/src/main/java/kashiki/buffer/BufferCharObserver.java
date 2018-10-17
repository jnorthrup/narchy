package kashiki.buffer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

public class BufferCharObserver implements Consumer<BufferChar> {

  private final Collection<Consumer<BufferChar>> listeners = new ArrayList<>();

  public void addListener(Consumer<BufferChar> listener) {
    listeners.add(listener);
  }

  @Override
  public void accept(BufferChar bc) {
    listeners.forEach((l) -> l.accept(bc));
  }
}

package spacegraph.space2d.widget.textedit.buffer;

import java.util.function.Consumer;

public class BufferChar implements Comparable<BufferChar> {

  private final String c;
  private int row, col;
  private final BufferCharObserver observer = new BufferCharObserver();

  public BufferChar(String c, int row, int col) {
    this.c = c;
    this.row = row;
    this.col = col;
  }

  public void addListener(Consumer<BufferChar> listener) {
    observer.addListener(listener);
  }

  public void updatePosition(int row, int col) {
    this.row = row;
    this.col = col;
    observer.accept(this);
  }

  public String getChar() {
    return c;
  }

  public int getRow() {
    return row;
  }

  public int getCol() {
    return col;
  }

  @Override
  public int compareTo(BufferChar o) {
    int rowCompare = Integer.compare(row, o.row);
    if (rowCompare != 0) {
      return rowCompare;
    }
    return Integer.compare(col, o.col);
  }
}

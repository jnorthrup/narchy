package spacegraph.space2d.widget.textedit.buffer;

@Deprecated public class BufferChar  {

  private final char c;
//  private int row, col;

  public BufferChar(char c) {
    this.c = c;
//    this.row = row;
//    this.col = col;
  }



//  void updatePosition(int row, int col) {
//    this.row = row;
//    this.col = col;
//    observer.accept(this);
//  }

  public char getChar() {
    return c;
  }

//  @Override
//  public int compareTo(BufferChar o) {
//    int rowCompare = Integer.compare(row, o.row);
//    if (rowCompare != 0) {
//      return rowCompare;
//    }
//    return Integer.compare(col, o.col);
//  }
}

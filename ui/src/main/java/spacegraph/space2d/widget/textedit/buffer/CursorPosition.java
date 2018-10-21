package spacegraph.space2d.widget.textedit.buffer;

public class CursorPosition implements Comparable<CursorPosition> {
  private volatile int col, row;

  public CursorPosition(int row, int col) {
    this.row = row;
    this.col = col;
  }

  @Override
  public int compareTo(CursorPosition o) {
    int rowCompare = Integer.compare(this.row, o.row);
    if (rowCompare == 0) {
      return Integer.compare(this.col, o.col);
    } else {
      return rowCompare;
    }
  }

  @Override
  public boolean equals(Object o) {
    return compareTo((CursorPosition) o) == 0;
  }

  @Override
  public String toString() {
    return "[row:" + row + ", col:" + col + ']';
  }

  public void incCol(int gain) {
    col += gain;
  }

  public void incRow(int gain) {
    row += gain;
  }

  public void decCol(int gain) {
    incCol(-gain);
  }

  public void decRow(int gain) {
    incRow(-gain);
  }

  public int getCol() {
    return col;
  }

  public void setCol(int col) {
    this.col = col;
  }

  public int getRow() {
    return row;
  }

  public void setRow(int row) {
    this.row = row;
  }
}

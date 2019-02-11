package spacegraph.space2d.widget.textedit.buffer;

public class CursorPosition implements Comparable<CursorPosition> {
    private int col, row;

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

    public boolean incCol(int gain) {
        return setCol(col + gain);
    }

    public boolean incRow(int gain) {
        return setRow(row + gain);
    }

    public boolean decCol(int gain) {
        return incCol(-gain);
    }

    public boolean decRow(int gain) {
        return incRow(-gain);
    }

    public int getCol() {
        return col;
    }

    public boolean setCol(int col) {
        if (this.col != col) {
            this.col = col;
            return true;
        }
        return false;
    }

    public int getRow() {
        return row;
    }

    public boolean setRow(int row) {
        if (this.row != row) {
            this.row = row;
            return true;
        }
        return false;
    }
}

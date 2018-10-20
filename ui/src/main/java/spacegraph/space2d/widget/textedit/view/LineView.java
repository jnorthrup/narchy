package spacegraph.space2d.widget.textedit.view;

import com.jogamp.opengl.GL2;
import jcog.data.list.FasterList;
import spacegraph.space2d.widget.textedit.buffer.BufferChar;
import spacegraph.space2d.widget.textedit.buffer.BufferLine;
import spacegraph.space2d.widget.textedit.buffer.BufferLineListener;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class LineView extends TextEditView implements BufferLineListener, Comparable<LineView> {

  private final BufferLine bufferLine;
  private final List<CharView> chars = new FasterList<>();
  private List<CharView> removedChars = new FasterList<>();

  public LineView(BufferLine bufferLine) {
    this.bufferLine = bufferLine;
    bufferLine.addListener(this);
    List<BufferChar> bufferChars = bufferLine.getChars();
    for (BufferChar bc : bufferChars) {
      chars.add(new CharView(bc));
    }
    updatePositions();
  }

  public static double getHeight() {
    return 1;
  }

  public double getWidth() {
    double width = 0;
    for (CharView c : chars) {
      width += c.width();
    }
    return width;
  }

  @Override
  public void innerDraw(GL2 gl) {
    for (CharView c : chars) {
      c.draw(gl);
    }
    List<CharView> removed = new FasterList<>();
    for (CharView c : removedChars) {
      c.draw(gl);
      if (!c.isAnimated()) {
        removed.add(c);
      }
    }
    removedChars = removed;
  }

  private void updatePositions() {
    color.getBlue().set(1);
    Collections.sort(chars);
    double width = 0;
    for (CharView c : chars) {
      double w = c.width() / 2;
      width += w;
      c.position.update(width, 0, 0);
      width += w;
    }
  }

  @Override
  public void update(BufferLine bl) {
    updatePositions();
  }

  @Override
  public void addChar(BufferChar bufferChar) {
    CharView cv = new CharView(bufferChar);
    chars.add(cv);

    Collections.sort(chars);
    double width = 0;
    for (CharView c : chars) {
      double w = c.width() / 2;
      width += w;
      if (c == cv) {
        c.position.getX().setWithoutSmooth(width);
      } else {
        c.position.getX().set(width);
      }
      width += w;
    }

    cv.scale.updateWithoutSmooth(0, 0, 0);
    cv.scale.update(1, 1, 1);
  }

  @Override
  public void removeChar(BufferChar bufferChar) {
    Iterator<CharView> ci = chars.iterator();
    while (ci.hasNext()) {
      CharView cv = ci.next();
      if (cv.bufferChar() == bufferChar) {
        cv.angle.update(0, 360, 0);
        cv.scale.update(0.5, 0.5, 0.5);
        cv.position.getY().add(-0.5);
        removedChars.add(cv);
        ci.remove();
        break;
      }
    }
    updatePositions();
  }

  public BufferLine getBufferLine() {
    return bufferLine;
  }

  public List<CharView> getChars() {
    return chars;
  }

  @Override
  public int compareTo(LineView o) {
    return bufferLine.compareTo(o.bufferLine);
  }

  public CharView leaveChar(BufferChar bc) {
    CharView leave = chars.stream().filter(c -> c.bufferChar() == bc).findFirst().orElse(null);
    chars.remove(leave);
    updatePositions();
    return leave;
  }

  public void visitChar(CharView cv) {
    chars.add(cv);
    updatePositions();
  }
}

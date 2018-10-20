package spacegraph.space2d.widget.textedit.view;


import org.junit.jupiter.api.Test;

public class SmoothValueTest {

  @Test
  public void testGains() {
    SmoothValue sv = new SmoothValue(0, Interpolator.LINER);
    sv.set(15);
    int count = 0;
    while (!sv.isAnimated()) {
      if (count++ == 7) {
        sv.setWithoutSmooth(20);
      }
      System.out.println(sv.value());
    }
  }

}

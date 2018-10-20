package spacegraph.space2d.widget.textedit.view;

import org.junit.jupiter.api.Test;

public class InterpolatorTest {

  @Test
  public void testGains() {
    // for (double d : MovingType.LINER.gains(10)) {
    // System.out.println(d);
    // }
    // for (double d : MovingType.SMOOTH.gains(10)) {
    // System.out.println(d);
    // }
    double inc = 0.0;
    for (double d : Interpolator.BOUND.gains(20)) {
      inc += d;
      System.out.println(d);
    }
    System.out.println(inc);
  }

}

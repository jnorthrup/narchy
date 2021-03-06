package spacegraph.space2d.widget.textedit.view;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import jcog.TODO;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public enum Interpolator {

  LINEAR {
    @Override
    protected double[] newCurve(int divOfNum) {
        double gain = 1.0 / (double) divOfNum;
        double[] result = new double[10];
        int count = 0;
        for (int i = 0; i < divOfNum; i++) {
            if (result.length == count) result = Arrays.copyOf(result, count * 2);
            double v = gain;
            result[count++] = v;
        }
        result = Arrays.copyOfRange(result, 0, count);
        return result;
    }
  },
  SMOOTH {
    @Override
    protected double[] newCurve(int divOfNum) {
        double[] result = new double[divOfNum];
        double gain = 1.0 / (double) divOfNum;
        double g = Math.PI * gain;
      for (int i = 0; i < divOfNum; i++) {
          double divGain = Math.cos(g * (double) i) - Math.cos(g * (double) (i + 1));
        result[i] = (divGain / 2.0);
      }
      return result;
    }
  },
  SMOOTH_OUT {
    @Override
    protected double[] newCurve(int divOfNum) {
        double[] result = new double[divOfNum];
        double gain = 1.0 / (double) divOfNum;
        double g = Math.PI * gain / 2.0;
      for (int i = 0; i < divOfNum; i++) {
          double divGain = Math.sin(g * (double) (i + 1)) - Math.sin(g * (double) i);
        result[i] = (divGain);
      }
      return result;
    }
  },
  SMOOTH_IN {
    @Override
    protected double[] newCurve(int divOfNum) {
        double[] result = new double[divOfNum];
        double gain = 1.0 / (double) divOfNum;
        double g = Math.PI * gain / 2.0;
        double start = Math.PI * 1.5;
      for (int i = 0; i < divOfNum; i++) {
          double divGain = Math.sin(start + (g * (double) (i + 1))) - Math.sin(start + (g * (double) i));
        result[i] = (divGain);
      }
      return result;
    }
  },
  BOUND {
    @Override
    protected double[] newCurve(int divOfNum) {
        double[] result = new double[divOfNum];
        double gain = 1.0 / (double) divOfNum;
        double g = Math.PI * 1.5 * gain;
        double qg = Math.PI / 4.0;
        double dd = Math.sin(qg) * 2.0;
      for (int i = 0; i < divOfNum; i++) {
          double divGain = Math.sin(g * (double) i + qg) - Math.sin(g * (double) (i + 1) + qg);
        result[i] = (divGain / dd);
      }
      return result;
    }
  };

  @Deprecated private final LoadingCache<Integer, double[]> cache =
      CacheBuilder.newBuilder().maximumSize(1000L).build(new CacheLoader<>() {
        @Override
        public double[] load(Integer divOfNum) {
          return newCurve(divOfNum);
        }
      });

  /** point sample: x in range 0..1, y in range 0..1 */
  public static float get(float x) {
      throw new TODO();
  }

  protected abstract double[] newCurve(int divOfNum);

  public double[] curve(int divOfNum) {
    try {
      return cache.get(divOfNum);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }
}

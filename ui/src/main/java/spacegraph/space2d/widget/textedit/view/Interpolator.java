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
      var gain = 1.0 / divOfNum;
      var result = new double[10];
      var count = 0;
        for (var i = 0; i < divOfNum; i++) {
            if (result.length == count) result = Arrays.copyOf(result, count * 2);
          var v = gain;
            result[count++] = v;
        }
        result = Arrays.copyOfRange(result, 0, count);
        return result;
    }
  },
  SMOOTH {
    @Override
    protected double[] newCurve(int divOfNum) {
      var result = new double[divOfNum];
      var gain = 1.0 / divOfNum;
      var g = Math.PI * gain;
      for (var i = 0; i < divOfNum; i++) {
        var divGain = Math.cos(g * i) - Math.cos(g * (i + 1));
        result[i] = (divGain / 2);
      }
      return result;
    }
  },
  SMOOTH_OUT {
    @Override
    protected double[] newCurve(int divOfNum) {
      var result = new double[divOfNum];
      var gain = 1.0 / divOfNum;
      var g = Math.PI * gain / 2.0;
      for (var i = 0; i < divOfNum; i++) {
        var divGain = Math.sin(g * (i + 1)) - Math.sin(g * i);
        result[i] = (divGain);
      }
      return result;
    }
  },
  SMOOTH_IN {
    @Override
    protected double[] newCurve(int divOfNum) {
      var result = new double[divOfNum];
      var gain = 1.0 / divOfNum;
      var g = Math.PI * gain / 2.0;
      var start = Math.PI * 1.5;
      for (var i = 0; i < divOfNum; i++) {
        var divGain = Math.sin(start + (g * (i + 1))) - Math.sin(start + (g * i));
        result[i] = (divGain);
      }
      return result;
    }
  },
  BOUND {
    @Override
    protected double[] newCurve(int divOfNum) {
      var result = new double[divOfNum];
      var gain = 1.0 / divOfNum;
      var g = Math.PI * 1.5 * gain;
      var qg = Math.PI / 4.0;
      var dd = Math.sin(qg) * 2;
      for (var i = 0; i < divOfNum; i++) {
        var divGain = Math.sin(g * i + qg) - Math.sin(g * (i + 1) + qg);
        result[i] = (divGain / dd);
      }
      return result;
    }
  };

  @Deprecated private final LoadingCache<Integer, double[]> cache =
      CacheBuilder.newBuilder().maximumSize(1000).build(new CacheLoader<>() {
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

package edu.decision.stream;

import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.apache.commons.math3.util.FastMath;
import org.jblas.DoubleMatrix;

import java.util.Arrays;

public class MatrixUtil {

    public static double[] getData(DoubleMatrix dm) {
        return dm.data;
    }

    public static int indexOf(final double[] array, final double v) {
        int lastId = array.length - 1;
        for (int i = 0; i < array.length; i++) if (array[i] > v) return i;
        return lastId;
    }

    private static double approximateP(double d, int n, int m) {
        return 1 - kt.ksSum(d * FastMath.sqrt(((double) m * (double) n) / ((double) m + (double) n)),
                1E-20, Integer.MAX_VALUE);
    }

    static public double kolmogorovSmirnovTest(double[] x, double[] y) {
        if (x.length * y.length < LARGE_SAMPLE_PRODUCT)
            return kt.exactP(kolmogorovSmirnovStatistic(x, y), x.length, y.length, true);
        return approximateP(kolmogorovSmirnovStatistic(x, y), x.length, y.length);
    }

    private static double kolmogorovSmirnovStatistic(double[] x, double[] y) {
        return integralKolmogorovSmirnovStatistic(x, y) / ((double) (x.length * (long) y.length));
    }

    static private long integralKolmogorovSmirnovStatistic(double[] x, double[] y) {
        Arrays.sort(x);
        Arrays.sort(y);
        final int n = x.length;
        final int m = y.length;
        int rankX = 0;
        int rankY = 0;
        long curD = 0L;
        long supD = 0L;
        do {
            double z = Double.compare(x[rankX], y[rankY]) <= 0 ? x[rankX] : y[rankY];
            while (rankX < n && Double.compare(x[rankX], z) == 0) {
                rankX += 1;
                curD += m;
            }
            while (rankY < m && Double.compare(y[rankY], z) == 0) {
                rankY += 1;
                curD -= n;
            }
            if (curD > supD) {
                supD = curD;
            } else if (-curD > supD) {
                supD = -curD;
            }
        } while (rankX < n && rankY < m);
        return supD;
    }

    private static final int LARGE_SAMPLE_PRODUCT = 10000;
    private static final KolmogorovSmirnovTest kt = new KolmogorovSmirnovTest();
}

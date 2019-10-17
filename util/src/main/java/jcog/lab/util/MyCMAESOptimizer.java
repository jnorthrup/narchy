package jcog.lab.util;

import jcog.Util;
import jcog.data.list.FasterList;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.optim.ConvergenceChecker;
import org.apache.commons.math3.optim.OptimizationData;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.MultivariateOptimizer;
import org.apache.commons.math3.util.MathArrays;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * adapted from Apache Commons Math 3.6
 */
public class MyCMAESOptimizer extends MultivariateOptimizer {


	private static final double dimensionDivisorWTF = 10.0;
	private static final double big_magic_number_WTF = 1.0e14;
	private static final double tENmILLION = 1.0e7;
	private static final double hUNDreDtHOUSAND = 1.0E5;
	private static final double oNEtHOUSAND = 1.0e3;
	private static final double epsilonWTF11 = 1.0e-11;
	private static final double EPSILON_WTF12 = 1.0e-12;
	private static final double epsilonwtf13 = 1.0e-13;
	private static final double epsilon6WTF = 1.0e-6;


	/**
	 * Population size, offspring number. The primary strategy parameter to play
	 * with, which can be increased from its default value. Increasing the
	 * population size improves global search properties in exchange to speed.
	 * Speed decreases, as a rule, at most linearly with increasing population
	 * size. It is advisable to begin with the default small population size.
	 * <p>
	 * Population size.
	 * The number of offspring is the primary strategy parameter.
	 * In the absence of better clues, a good default could be an
	 * integer close to {@code 4 + 3 ln(n)}, where {@code n} is the
	 * number of optimized parameters.
	 * Increasing the population size improves global search properties
	 * at the expense of speed (which in general decreases at most
	 * linearly with increasing population size).
	 */
	private final int lambda;
	/**
	 * Covariance update mechanism, default is active CMA. isActiveCMA = true
	 * turns on "active CMA" with a negative update of the covariance matrix and
	 * checks for positive definiteness. OPTS.CMA.active = 2 does not check for
	 * pos. def. and is numerically faster. Active CMA usually speeds up the
	 * adaptation.
	 */
	private final boolean isActiveCMA;
	/**
	 * Determines how often a new random offspring is generated in case it is
	 * not feasible / beyond the defined limits, default is 0.
	 */
	private final int checkFeasableCount;

	/**
	 * Input sigma values.
	 * They define the initial coordinate-wise standard deviations for
	 * sampling new search points around the initial guess.
	 * It is suggested to set them to the estimated distance from the
	 * initial to the desired optimum.
	 * Small values induce the search to be more local (and very small
	 * values are more likely to find a local optimum close to the initial
	 * guess).
	 * Too small values might however lead to early termination.
	 */
	private final double[] inputSigma;
	/**
	 * Indicates whether statistic data is collected.
	 */
	private final boolean generateStatistics;
	/**
	 * Maximal number of iterations allowed.
	 */
	private final int maxIterations;
	/**
	 * Limit for fitness value.
	 */
	private final double stopFitness;
	/**
	 * Random generator.
	 */
	private final Random random;
	/**
	 * History of sigma values.
	 */
	private final List<Double> statisticsSigmaHistory = new FasterList<>();
	/**
	 * History of mean matrix.
	 */
	private final List<RealMatrix> statisticsMeanHistory = new FasterList<>();
	/**
	 * History of fitness values.
	 */
    public final List<Double> statisticsFitnessHistory = new FasterList<>();
	/**
	 * History of D matrix.
	 */
	public final List<RealMatrix> statisticsDHistory = new FasterList<>();
	/**
	 * Number of objective variables/problem dimension
	 */
	private int dimension;
	/**
	 * Defines the number of initial iterations, where the covariance matrix
	 * remains diagonal and the algorithm has internally linear time complexity.
	 * diagonalOnly = 1 means keeping the covariance matrix always diagonal and
	 * this setting also exhibits linear space complexity. This can be
	 * particularly useful for dimension > 100.
	 *
	 * @see <a href="http:
	 */
	private int diagonalOnly;
	/**
	 * Number of objective variables/problem dimension
	 */
	private boolean isMinimize = true;
	/**
	 * Stop if x-changes larger stopTolUpX.
	 */
	private double stopTolUpX;
	/**
	 * Stop if x-change smaller stopTolX.
	 */
	private double stopTolX;
	/**
	 * Stop if fun-changes smaller stopTolFun.
	 */
	private double stopTolFun;
	/**
	 * Stop if back fun-changes smaller stopTolHistFun.
	 */
	private double stopTolHistFun;
	/**
	 * Number of parents/points for recombination.
	 */
	private int mu;
	/**
	 * Array for weighted recombination.
	 */
	private RealMatrix weights;
	/**
	 * Variance-effectiveness of sum w_i x_i.
	 */
	private double mueff;
	/**
	 * Overall standard deviation - search volume.
	 */
	private double sigma;
	/**
	 * Cumulation constant.
	 */
	private double cc;
	/**
	 * Cumulation constant for step-size.
	 */
	private double cs;
	/**
	 * Damping for step-size.
	 */
	private double damps;
	/**
	 * Learning rate for rank-one update.
	 */
	private double ccov1;
	/**
	 * Learning rate for rank-mu update'
	 */
	private double ccovmu;
	/**
	 * Expectation of ||N(0,I)|| == norm(randn(N,1)).
	 */
	private double chiN;
	/**
	 * Learning rate for rank-one update - diagonalOnly
	 */
	private double ccov1Sep;
	/**
	 * Learning rate for rank-mu update - diagonalOnly
	 */
	private double ccovmuSep;
	/**
	 * Objective variables.
	 */
	private RealMatrix xmean;
	/**
	 * Evolution path.
	 */
	private RealMatrix pc;
	/**
	 * Evolution path for sigma.
	 */
	private RealMatrix ps;
	/**
	 * Norm of ps, stored for efficiency.
	 */
	private double normps;
	/**
	 * Coordinate system.
	 */
	private RealMatrix B;
	/**
	 * Scaling.
	 */
	private RealMatrix D;
	/**
	 * B*D, stored for efficiency.
	 */
	private RealMatrix BD;
	/**
	 * Diagonal of sqrt(D), stored for efficiency.
	 */
	private RealMatrix diagD;
	/**
	 * Covariance matrix.
	 */
	private RealMatrix C;
	/**
	 * Diagonal of C, used for diagonalOnly.
	 */
	private RealMatrix diagC;
	/**
	 * Number of iterations already performed.
	 */
	private int iterations;
	/**
	 * History queue of best values.
	 */
	private double[] fitnessHistory;

	/**
	 * @param maxIterations      Maximal number of iterations.
	 * @param stopFitness        Whether to stop if objective function value is smaller than
	 *                           {@code stopFitness}.  use NaN to disable
	 * @param isActiveCMA        Chooses the covariance matrix update method.
	 * @param diagonalOnly       Number of initial iterations, where the covariance matrix
	 *                           remains diagonal.
	 * @param checkFeasableCount Determines how often new random objective variables are
	 *                           generated in case they are out of bounds.
	 * @param random             Random generator.
	 * @param generateStatistics Whether statistic data is collected.
	 * @param checker            Convergence checker.
	 * @param populationSize
	 * @param sigma
	 * @since 3.1
	 */
	public MyCMAESOptimizer(int maxIterations,
							double stopFitness,
							boolean isActiveCMA,
							int diagonalOnly,
							int checkFeasableCount,
							Random random,
							boolean generateStatistics,
							ConvergenceChecker<PointValuePair> checker, int populationSize, double[] sigma) {
		super(checker);
		this.maxIterations = maxIterations;
		this.stopFitness = stopFitness;
		this.isActiveCMA = isActiveCMA;
		this.diagonalOnly = diagonalOnly;
		this.checkFeasableCount = checkFeasableCount;
		this.random = random;
		this.generateStatistics = generateStatistics;
		lambda = populationSize;
		inputSigma = sigma;
	}

	/**
	 * Pushes the current best fitness value in a history queue.
	 *
	 * @param vals History queue.
	 * @param val  Current best fitness value.
	 */
	private static void push(double[] vals, double val) {
		if (vals.length - 1 >= 0) System.arraycopy(vals, 0, vals, 1, vals.length - 1);
		vals[0] = val;
	}

	/**
	 * Sorts fitness values.
	 *
	 * @param x Array of values to be sorted.
	 * @return a sorted array of indices pointing into doubles.
	 */
	private static int[] sortedIndices(final double[] x) {
		final DoubleIndex[] y = IntStream.range(0, x.length).mapToObj(i -> new DoubleIndex(x[i], i)).toArray(DoubleIndex[]::new);
        Arrays.sort(y);
		final int[] j = IntStream.range(0, x.length).map(i -> y[i].index).toArray();
        return j;
	}

	/**
	 * Get range of values.
	 *
	 * @param vpPairs Array of valuePenaltyPairs to get range from.
	 * @return a double equal to maximum value minus minimum value.
	 */
	private static double valueRange(final MyCMAESOptimizer.ValuePenaltyPair[] vpPairs) {
		double max = Double.NEGATIVE_INFINITY;
		double min = Double.POSITIVE_INFINITY;
		for (MyCMAESOptimizer.ValuePenaltyPair vpPair : vpPairs) {
            double vv = vpPair.value;
            min = Math.min(vv, min);
            max = Math.max(vv, max);
		}
		return max - min;
	}

	/**
	 * @param m Input matrix
	 * @return Matrix representing the element-wise logarithm of m.
	 */
	private static RealMatrix log(final RealMatrix m) {
		int R = m.getRowDimension();
		int C = m.getColumnDimension();
		final double[][] d = new double[R][C];
		for (int r = 0; r < R; r++) for (int c = 0; c < C; c++) d[r][c] = Math.log(m.getEntry(r, c));
		return new Array2DRowRealMatrix(d, false);
	}

	/**
	 * @param m Input matrix.
	 * @return Matrix representing the element-wise square root of m.
	 */
	private static RealMatrix sqrt(final RealMatrix m) {
		int R = m.getRowDimension();
		int C = m.getColumnDimension();
		final double[][] d = new double[R][C];
		for (int r = 0; r < R; r++) for (int c = 0; c < C; c++) d[r][c] = Math.sqrt(m.getEntry(r, c));
		return new Array2DRowRealMatrix(d, false);
	}

	/**
	 * @param m Input matrix.
	 * @return Matrix representing the element-wise square of m.
	 */
	private static RealMatrix square(final RealMatrix m) {
		final double[][] d = new double[m.getRowDimension()][m.getColumnDimension()];
		for (int r = 0; r < m.getRowDimension(); r++)
			for (int c = 0; c < m.getColumnDimension(); c++) {
				double e = m.getEntry(r, c);
				d[r][c] = e * e;
			}
		return new Array2DRowRealMatrix(d, false);
	}

	/**
	 * @param m Input matrix 1.
	 * @param n Input matrix 2.
	 * @return the matrix where the elements of m and n are element-wise multiplied.
	 */
	private static RealMatrix times(final RealMatrix m, final RealMatrix n) {
		int R = m.getRowDimension();
		int C = m.getColumnDimension();
		final double[][] d = new double[R][C];
		for (int r = 0; r < R; r++) for (int c = 0; c < C; c++) d[r][c] = m.getEntry(r, c) * n.getEntry(r, c);
		return new Array2DRowRealMatrix(d, false);
	}

	/**
	 * @param m Input matrix 1.
	 * @param n Input matrix 2.
	 * @return Matrix where the elements of m and n are element-wise divided.
	 */
	private static RealMatrix divide(final RealMatrix m, final RealMatrix n) {
		final double[][] d = new double[m.getRowDimension()][m.getColumnDimension()];
		for (int r = 0; r < m.getRowDimension(); r++)
			for (int c = 0; c < m.getColumnDimension(); c++) d[r][c] = m.getEntry(r, c) / n.getEntry(r, c);
		return new Array2DRowRealMatrix(d, false);
	}

	/**
	 * @param m    Input matrix.
	 * @param cols Columns to select.
	 * @return Matrix representing the selected columns.
	 */
	private static RealMatrix selectColumns(final RealMatrix m, final int[] cols) {
		int rowDimension = m.getRowDimension();
		final double[][] d = new double[rowDimension][cols.length];
		for (int r = 0; r < rowDimension; r++) for (int c = 0; c < cols.length; c++) d[r][c] = m.getEntry(r, cols[c]);
		return new Array2DRowRealMatrix(d, false);
	}

	/**
	 * @param m Input matrix.
	 * @param k Diagonal position.
	 * @return Upper triangular part of matrix.
	 */
	private static RealMatrix triu(final RealMatrix m, int k) {
		int R = m.getRowDimension();
		int C = m.getColumnDimension();
		final double[][] d = new double[R][C];
		for (int r = 0; r < R; r++) for (int c = 0; c < C; c++) d[r][c] = r <= c - k ? m.getEntry(r, c) : 0;
		return new Array2DRowRealMatrix(d, false);
	}

	/**
	 * @param m Input matrix.
	 * @return Row matrix representing the sums of the rows.
	 */
	private static RealMatrix sumRows(final RealMatrix m) {
		int C = m.getColumnDimension();
		final double[][] d = new double[1][C];
		int R = m.getRowDimension();
		for (int c = 0; c < C; c++) {
			double sum = 0;
			for (int r = 0; r < R; r++)
				sum += m.getEntry(r, c);
			d[0][c] = sum;
		}
		return new Array2DRowRealMatrix(d, false);
	}

	/**
	 * @param m Input matrix.
	 * @return the diagonal n-by-n matrix if m is a column matrix or the column
	 * matrix representing the diagonal if m is a n-by-n matrix.
	 */
	private static RealMatrix diag(final RealMatrix m) {
		int c = m.getColumnDimension();
		int r = m.getRowDimension();
		if (c == 1) {
			final double[][] d = new double[r][r];
			for (int i = 0; i < r; i++)
				d[i][i] = m.getEntry(i, 0);
			return new Array2DRowRealMatrix(d, false);
		} else {
			final double[][] d = new double[r][1];
			for (int i = 0; i < c; i++)
				d[i][0] = m.getEntry(i, i);
			return new Array2DRowRealMatrix(d, false);
		}
	}

	/**
	 * Copies a column from m1 to m2.
	 *
	 * @param m1   Source matrix.
	 * @param col1 Source column.
	 * @param m2   Target matrix.
	 * @param col2 Target column.
	 */
	private static void copyColumn(final RealMatrix m1, int col1, RealMatrix m2, int col2) {
		int rd = m1.getRowDimension();
		for (int i = 0; i < rd; i++) m2.setEntry(i, col2, m1.getEntry(i, col1));
	}

	/**
	 * @param n Number of rows.
	 * @param m Number of columns.
	 * @return n-by-m matrix filled with 1.
	 */
	private static RealMatrix ones(int n, int m) {
		final double[][] d = new double[n][m];
		for (int r = 0; r < n; r++) Arrays.fill(d[r], 1);
		return new Array2DRowRealMatrix(d, false);
	}

	/**
	 * @param n Number of rows.
	 * @param m Number of columns.
	 * @return n-by-m matrix of 0 values out of diagonal, and 1 values on
	 * the diagonal.
	 */
	private static RealMatrix eye(int n, int m) {
		final double[][] d = new double[n][m];
		for (int r = 0; r < n; r++) if (r < m) d[r][r] = 1;
		return new Array2DRowRealMatrix(d, false);
	}

	/**
	 * @param n Number of rows.
	 * @param m Number of columns.
	 * @return n-by-m matrix of zero values.
	 */
	private static RealMatrix zeros(int n, int m) {
		return new Array2DRowRealMatrix(n, m);
	}

	/**
	 * @param mat Input matrix.
	 * @param n   Number of row replicates.
	 * @param m   Number of column replicates.
	 * @return a matrix which replicates the input matrix in both directions.
	 */
	private static RealMatrix repmat(final RealMatrix mat, int n, int m) {
		final int rd = mat.getRowDimension();
		final int cd = mat.getColumnDimension();
		final double[][] d = new double[n * rd][m * cd];
		for (int r = 0; r < n * rd; r++) {
			double[] dr = d[r];
			int rrd = r % rd;
			for (int c = 0; c < m * cd; c++) dr[c] = mat.getEntry(rrd, c % cd);
		}
		return new Array2DRowRealMatrix(d, false);
	}

	/**
	 * @param start Start value.
	 * @param end   End value.
	 * @param step  Step size.
	 * @return a sequence as column matrix.
	 */
	private static RealMatrix sequence(double start, double end, double step) {
		final int size = (int) ((end - start) / step + 1);
		final double[][] d = new double[size][1];
		double value = start;
		for (int r = 0; r < size; r++) {
			d[r][0] = value;
			value += step;
		}
		return new Array2DRowRealMatrix(d, false);
	}

	/**
	 * @param m Input matrix.
	 * @return the maximum of the matrix element values.
	 */
	private static double max(final RealMatrix m) {
		double max = Double.NEGATIVE_INFINITY;
		int R = m.getRowDimension();
		int C = m.getColumnDimension();

		for (int r = 0; r < R; r++)
			for (int c = 0; c < C; c++) {
				double e = m.getEntry(r, c);
				if (max < e)
					max = e;
			}
		return max;
	}

	/**
	 * @param m Input matrix.
	 * @return the minimum of the matrix element values.
	 */
	private static double min(final RealMatrix m) {
		double min = Double.POSITIVE_INFINITY;
		int R = m.getRowDimension();
		int C = m.getColumnDimension();
		for (int r = 0; r < R; r++)
			for (int c = 0; c < C; c++) {
				double e = m.getEntry(r, c);
				if (min > e)
					min = e;
			}
		return min;
	}

	/**
	 * @param indices Input index array.
	 * @return the inverse of the mapping defined by indices.
	 */
	private static int[] inverse(final int[] indices) {
		final int[] inverse = new int[indices.length];
		for (int i = 0; i < indices.length; i++) inverse[indices[i]] = i;
		return inverse;
	}

	/**
	 * @param indices Input index array.
	 * @return the indices in inverse order (last is first).
	 */
	private static int[] reverse(final int[] indices) {
		final int[] reverse = IntStream.range(0, indices.length).map(i -> indices[indices.length - i - 1]).toArray();
        return reverse;
	}

	/**
	 * @return History of sigma values.
	 */
	public List<Double> getStatisticsSigmaHistory() {
		return statisticsSigmaHistory;
	}

	/**
	 * @return History of mean matrix.
	 */
	public List<RealMatrix> getStatisticsMeanHistory() {
		return statisticsMeanHistory;
	}

	/**
	 * @return History of fitness values.
	 */
	public List<Double> getStatisticsFitnessHistory() {
		return statisticsFitnessHistory;
	}

	/**
	 * @return History of D matrix.
	 */
	public List<RealMatrix> getStatisticsDHistory() {
		return statisticsDHistory;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected PointValuePair doOptimize() {
        iterations = 0;

		final FitEval eval = new FitEval();

		for (iterations = 1; iterations <= maxIterations; iterations++) {
            if (!eval.iterate())
                break;
		}

		return eval.opt;
	}


	public void print(PrintStream out) {
		out.println("sigmaHistory: " + statisticsSigmaHistory);
		out.println("sigmaFitnessHistory: " + statisticsFitnessHistory);
		out.println("meanHistory: " + statisticsMeanHistory);
	}

	/**
	 * Scans the list of (required and optional) optimization data that
	 * characterize the problem.
	 *
	 * @param optData Optimization data. The following data will be looked for:
	 *                <ul>
	 *                 <li>{@link MyCMAESOptimizer.Sigma}</li>
	 *                 <li>{@link MyCMAESOptimizer.PopulationSize}</li>
	 *                </ul>
	 */
	@Override
	protected void parseOptimizationData(OptimizationData... optData) {

		super.parseOptimizationData(optData);


		checkParameters();
	}

	/**
	 * Checks dimensions and values of boundaries and inputSigma if defined.
	 */
	private void checkParameters() {
		final double[] init = getStartPoint();
		final double[] lB = getLowerBound();
		final double[] uB = getUpperBound();

		if (inputSigma != null) {
			if (inputSigma.length != init.length) throw new DimensionMismatchException(inputSigma.length, init.length);
			for (int i = 0; i < init.length; i++)
				if (inputSigma[i] > uB[i] - lB[i]) throw new OutOfRangeException(inputSigma[i], 0, uB[i] - lB[i]);
		}
	}

	/**
	 * Initialization of the dynamic search parameters
	 *
	 * @param guess Initial guess for the arguments of the fitness function.
	 */
	private void initializeCMA(double[] guess) {
		if (lambda <= 0) throw new NotStrictlyPositiveException(lambda);

		final double[][] sigmaArray = new double[guess.length][1];
		for (int i = 0; i < guess.length; i++)
		    sigmaArray[i][0] = inputSigma[i];
		final RealMatrix insigma = new Array2DRowRealMatrix(sigmaArray, false);
		sigma = max(insigma);


		stopTolUpX = oNEtHOUSAND * max(insigma);
		stopTolX = epsilonWTF11 * max(insigma);
		this.stopTolFun = EPSILON_WTF12;
		this.stopTolHistFun = epsilonwtf13;


		mu = lambda / 2;
		/* log(mu + 0.5), stored for efficiency. */
		double logMu2 = Math.log(mu + 0.5);
		weights = log(sequence(1, mu, 1)).scalarMultiply(-1).scalarAdd(logMu2);
		double sumw = 0;
		double sumwq = 0;
		for (int i = 0; i < mu; i++) {
			double w = weights.getEntry(i, 0);
			sumw += w;
			sumwq += w * w;
		}
		weights = weights.scalarMultiply(1 / sumw);
		mueff = sumw * sumw / sumwq;


		cc = (4 + mueff / dimension) /
			(dimension + 4 + 2 * mueff / dimension);
		cs = (mueff + 2) / (dimension + mueff + 3.0);
		damps = (1 + 2 * Math.max(0, Math.sqrt((mueff - 1) /
			(dimension + 1)) - 1)) *
			Math.max(0.3,
				1 - dimension / (epsilon6WTF + maxIterations)) + cs;
		ccov1 = 2 / ((dimension + 1.3) * (dimension + 1.3) + mueff);
		ccovmu = Math.min(1 - ccov1, 2 * (mueff - 2 + 1 / mueff) /
			((dimension + 2) * (dimension + 2) + mueff));
		ccov1Sep = Math.min(1, ccov1 * (dimension + 1.5) / 3);
		ccovmuSep = Math.min(1 - ccov1, ccovmu * (dimension + 1.5) / 3);
		chiN = Math.sqrt(dimension) *
			(1 - 1 / (4.0 * dimension) + 1 / (21.0 * dimension * dimension));

		xmean = MatrixUtils.createColumnRealMatrix(guess);
		diagD = insigma.scalarMultiply(1 / sigma);
		diagC = square(diagD);
		pc = zeros(dimension, 1);
		ps = zeros(dimension, 1);
		normps = ps.getFrobeniusNorm();

		B = eye(dimension, dimension);
		D = ones(dimension, 1);
		BD = times(B, repmat(diagD.transpose(), dimension, 1));
		C = B.multiply(diag(square(D)).multiply(B.transpose()));

		/* Size of history queue of best values. */
		int historySize = 10 + (int) (3 * 10 * dimension / (double) lambda);
		fitnessHistory = new double[historySize];
		Arrays.fill(fitnessHistory, Double.POSITIVE_INFINITY);
	}

	/**
	 * Update of the evolution paths ps and pc.
	 *
	 * @param zmean Weighted row matrix of the gaussian random numbers generating
	 *              the current offspring.
	 * @param xold  xmean matrix of the previous generation.
	 * @return hsig flag indicating a small correction.
	 */
	private boolean updateEvolutionPaths(RealMatrix zmean, RealMatrix xold) {
		ps = ps.scalarMultiply(1 - cs).add(
			B.multiply(zmean).scalarMultiply(
				Math.sqrt(cs * (2 - cs) * mueff)));
		normps = ps.getFrobeniusNorm();
		final boolean hsig = normps /
			Math.sqrt(1 - Math.pow(1 - cs, 2 * iterations)) /
			chiN < 1.4 + 2 / ((double) dimension + 1);
		pc = pc.scalarMultiply(1 - cc);
		if (hsig) pc = pc.add(xmean.subtract(xold).scalarMultiply(Math.sqrt(cc * (2 - cc) * mueff) / sigma));
		return hsig;
	}

	/**
	 * Update of the covariance matrix C for diagonalOnly > 0
	 *
	 * @param hsig    Flag indicating a small correction.
	 * @param bestArz Fitness-sorted matrix of the gaussian random values of the
	 *                current offspring.
	 */
	private void updateCovarianceDiagonalOnly(boolean hsig,
											  final RealMatrix bestArz) {

		double oldFac = hsig ? 0 : ccov1Sep * cc * (2 - cc);
		oldFac += 1 - ccov1Sep - ccovmuSep;
		diagC = diagC.scalarMultiply(oldFac)
			.add(square(pc).scalarMultiply(ccov1Sep))
			.add((times(diagC, square(bestArz).multiply(weights)))
				.scalarMultiply(ccovmuSep));
		diagD = sqrt(diagC);
		if (diagonalOnly > 1 &&
			iterations > diagonalOnly) {

			diagonalOnly = 0;
			B = eye(dimension, dimension);
			BD = diag(diagD);
			C = diag(diagC);
		}
	}

	/**
	 * Update of the covariance matrix C.
	 *
	 * @param hsig    Flag indicating a small correction.
	 * @param bestArx Fitness-sorted matrix of the argument vectors producing the
	 *                current offspring.
	 * @param arz     Unsorted matrix containing the gaussian random values of the
	 *                current offspring.
	 * @param arindex Indices indicating the fitness-order of the current offspring.
	 * @param xold    xmean matrix of the previous generation.
	 */
	private void updateCovariance(boolean hsig, final RealMatrix bestArx,
								  final RealMatrix arz, final int[] arindex,
								  final RealMatrix xold) {
		double negccov = 0;
		if (ccov1 + ccovmu > 0) {
			final RealMatrix arpos = bestArx.subtract(repmat(xold, 1, mu))
				.scalarMultiply(1 / sigma);
			final RealMatrix roneu = pc.multiply(pc.transpose())
				.scalarMultiply(ccov1);

			double oldFac = hsig ? 0 : ccov1 * cc * (2 - cc);
			oldFac += 1 - ccov1 - ccovmu;
			if (isActiveCMA) {

				negccov = (1 - ccovmu) * 0.25 * mueff /
					(Math.pow(dimension + 2, 1.5) + 2 * mueff);


				final double negminresidualvariance = 0.66;

				final double negalphaold = 0.5;

				final int[] arReverseIndex = reverse(arindex);
				RealMatrix arzneg = selectColumns(arz, MathArrays.copyOf(arReverseIndex, mu));
				RealMatrix arnorms = sqrt(sumRows(square(arzneg)));
				final int[] idxnorms = sortedIndices(arnorms.getRow(0));
				final RealMatrix arnormsSorted = selectColumns(arnorms, idxnorms);
				final int[] idxReverse = reverse(idxnorms);
				final RealMatrix arnormsReverse = selectColumns(arnorms, idxReverse);
				arnorms = divide(arnormsReverse, arnormsSorted);
				final int[] idxInv = inverse(idxnorms);
				final RealMatrix arnormsInv = selectColumns(arnorms, idxInv);

				final double negcovMax = (1 - negminresidualvariance) /
					square(arnormsInv).multiply(weights).getEntry(0, 0);
				if (negccov > negcovMax) negccov = negcovMax;
				arzneg = times(arzneg, repmat(arnormsInv, dimension, 1));
				final RealMatrix artmp = BD.multiply(arzneg);
				final RealMatrix Cneg = artmp.multiply(diag(weights)).multiply(artmp.transpose());
				oldFac += negalphaold * negccov;
				C = C.scalarMultiply(oldFac)
					.add(roneu)
					.add(arpos.scalarMultiply(
						ccovmu + (1 - negalphaold) * negccov)
						.multiply(times(repmat(weights, 1, dimension),
							arpos.transpose())))
					.subtract(Cneg.scalarMultiply(negccov));
			} else C = C.scalarMultiply(oldFac)
				.add(roneu)
				.add(arpos.scalarMultiply(ccovmu)
					.multiply(times(repmat(weights, 1, dimension),
						arpos.transpose())));
		}
		updateBD(negccov);
	}

	/**
	 * Update B and D from C.
	 *
	 * @param negccov Negative covariance factor.
	 */
	private void updateBD(double negccov) {

		if (ccov1 + ccovmu + negccov > 0 &&
			(iterations % 1.0 / (ccov1 + ccovmu + negccov) / dimension / dimensionDivisorWTF) < 1) {

			C = triu(C, 0).add(triu(C, 1).transpose());

			final EigenDecomposition eig = new EigenDecomposition(C);
			B = eig.getV();
			D = eig.getD();
			diagD = diag(D);

			if (min(diagD) <= 0) {
				for (int i = 0; i < dimension; i++) if (diagD.getEntry(i, 0) < 0) diagD.setEntry(i, 0, 0);
				final double tfac = max(diagD) / big_magic_number_WTF;
				C = C.add(eye(dimension, dimension).scalarMultiply(tfac));
				diagD = diagD.add(ones(dimension, 1).scalarMultiply(tfac));
			}
			if (max(diagD) > big_magic_number_WTF * min(diagD)) {
				final double tfac = max(diagD) / big_magic_number_WTF - min(diagD);
				C = C.add(eye(dimension, dimension).scalarMultiply(tfac));
				diagD = diagD.add(ones(dimension, 1).scalarMultiply(tfac));
			}
			diagC = diag(C);
			diagD = sqrt(diagD);
			BD = times(B, repmat(diagD.transpose(), dimension, 1));
		}
	}

	/**
	 * @param size Length of random array.
	 * @return an array of Gaussian random numbers.
	 */
	private double[] randn(int size) {
		final double[] randn = IntStream.range(0, size).mapToDouble(i -> random.nextGaussian()).toArray();
        return randn;
	}

	/**
	 * @param size    Number of rows.
	 * @param popSize Population size.
	 * @return a 2-dimensional matrix of Gaussian random numbers.
	 */
	private RealMatrix randn1(int size, int popSize) {
		final double[][] d = new double[size][popSize];
		for (int r = 0; r < size; r++) for (int c = 0; c < popSize; c++) d[r][c] = random.nextGaussian();
		return new Array2DRowRealMatrix(d, false);
	}

	/**
	 * Used to sort fitness values. Sorting is always in lower value first
	 * order.
	 */
	private static final class DoubleIndex implements Comparable<MyCMAESOptimizer.DoubleIndex> {
		/**
		 * Value to compare.
		 */
		private final double value;
		/**
		 * Index into sorted array.
		 */
		private final int index;

		/**
		 * @param value Value to compare.
		 * @param index Index into sorted array.
		 */
		DoubleIndex(double value, int index) {
			this.value = value;
			this.index = index;
		}

		/**
		 * {@inheritDoc}
		 */
		public int compareTo(MyCMAESOptimizer.DoubleIndex o) {
			return this == o ? 0 : Double.compare(value, o.value);
		}

//		/**
//		 * {@inheritDoc}
//		 */
//		@Override
//		public boolean equals(Object other) {

			//if (this == other)
				//return true;

			//if (other instanceof MyCMAESOptimizer.DoubleIndex)
				//return Double.compare(value, ((MyCMAESOptimizer.DoubleIndex) other).value) == 0;

			//return false;
//		}

//		/**
//		 * {@inheritDoc}
//		 */
//		@Override
//		public int hashCode() {
//			long bits = Double.doubleToLongBits(value);
//			return (int) ((1438542 ^ (bits >>> 32) ^ bits) & 0xffffffff);
//		}
	}

	/**
	 * Stores the value and penalty (for repair of out of bounds point).
	 */
	private static class ValuePenaltyPair {
		/**
		 * Objective function value.
		 */
		private final double value;
		/**
		 * Penalty value for repair of out out of bounds points.
		 */
		private final double penalty;

		/**
		 * @param value   Function value.
		 * @param penalty Out-of-bounds penalty.
		 */
		ValuePenaltyPair(final double value, final double penalty) {
			this.value = value;
			this.penalty = penalty;
		}
	}

	/**
	 * Normalizes fitness values to the range [0,1]. Adds a penalty to the
	 * fitness value if out of range.
	 */
	private class FitEval {
		/**
		 * Flag indicating whether the objective variables are forced into their
		 * bounds if defined
		 */
		private final boolean isRepairMode;
        public final double[] lB;
        public final double[] uB;
        public PointValuePair opt,lastResult;
        public double bestValue;
        final double[] fitness = new double[lambda];
        final MyCMAESOptimizer.ValuePenaltyPair[] valuePenaltyPairs = new MyCMAESOptimizer.ValuePenaltyPair[lambda];

        /**
		 * Simple constructor.
		 */
		FitEval() {
            isMinimize = getGoalType().equals(GoalType.MINIMIZE);
			isRepairMode = true;
			lB = getLowerBound();
			uB = getUpperBound();

            final double[] guess = getStartPoint();
            dimension = guess.length;

            initializeCMA(guess);

            MyCMAESOptimizer.ValuePenaltyPair valuePenalty = value(guess);

            bestValue = valuePenalty.value + valuePenalty.penalty;
            push(fitnessHistory, bestValue);

            opt = new PointValuePair(guess, isMinimize ? bestValue : -bestValue);
            lastResult = null;
        }

		/**
		 * @param point Normalized objective variables.
		 * @return the objective value + penalty for violated bounds.
		 */
		MyCMAESOptimizer.ValuePenaltyPair value(double[] point) {
			double penalty;
			if (isRepairMode) {
				double[] repaired = repair(point);
                penalty = penalty(point, repaired);
                point = repaired;
			} else
			    penalty = 0;

            double value = MyCMAESOptimizer.this.computeObjectiveValue(point);
			return new MyCMAESOptimizer.ValuePenaltyPair(isMinimize ? value : -value, isMinimize ? penalty : -penalty);
		}

		/**
		 * @param x Normalized objective variables.
		 * @return {@code true} if in bounds.
		 */
		boolean isFeasible(final double[] x, double[] lB, double[] uB) {

			for (int i = 0; i < x.length; i++) {
				double xi = x[i];
				if (xi < lB[i])
					return false;
				if (xi > uB[i])
					return false;
			}
			return true;
		}

		/**
		 * @param x Normalized objective variables.
		 * @return the repaired (i.e. all in bounds) objective variables.
		 */
		private double[] repair(final double[] x) {
			final double[] lB = this.lB, uB = this.uB;
			final double[] repaired = new double[x.length];
			for (int i = 0; i < x.length; i++) {
				double xi = x[i];
                repaired[i] = xi < lB[i] ? lB[i] : Math.min(xi, uB[i]);
			}
			return repaired;
		}

		/**
		 * @param x        Normalized objective variables.
		 * @param repaired Repaired objective variables.
		 * @return Penalty value according to the violation of the bounds.
		 */
		private double penalty(final double[] x, final double[] repaired) {
			double penalty = IntStream.range(0, x.length).mapToDouble(i -> Math.abs(x[i] - repaired[i])).sum();
            return isMinimize ? penalty : -penalty;
		}

        public boolean iterate() {

            incrementIterationCount();


            final RealMatrix arz = randn1(dimension, lambda);
            final RealMatrix arx = zeros(dimension, lambda);

            double[] lB = this.lB, uB = this.uB;

            for (int k = 0; k < lambda; k++) {
                RealMatrix arzK = arz.getColumnMatrix(k);
                RealMatrix xFactor = times(diagD, arzK).scalarMultiply(sigma);

                RealMatrix arxk = null;
                for (int i = 0; i < checkFeasableCount + 1; i++) {

                    arxk = xmean.add(diagonalOnly <= 0 ? BD.multiply(arzK).scalarMultiply(sigma) : xFactor);

                    if (i >= checkFeasableCount || this.isFeasible(arxk.getColumn(0), lB, uB))
                        break;

                    arz.setColumn(k, randn(dimension));
                }

                copyColumn(arxk, 0, arx, k);

                try {
                    valuePenaltyPairs[k] = this.value(arx.getColumn(k));
                } catch (TooManyEvaluationsException e) {
                    return false;
                }
            }


            double valueRange = valueRange(valuePenaltyPairs);
            for (int iValue = 0; iValue < valuePenaltyPairs.length; iValue++)
                fitness[iValue] = valuePenaltyPairs[iValue].value + valuePenaltyPairs[iValue].penalty * valueRange;

            final int[] arindex = sortedIndices(fitness);

            final RealMatrix xold = xmean;
            int[] arMu = MathArrays.copyOf(arindex, mu);

            final RealMatrix bestArx = selectColumns(arx, arMu);
            xmean = bestArx.multiply(weights);

            final RealMatrix bestArz = selectColumns(arz, arMu);
            final RealMatrix zmean = bestArz.multiply(weights);

            final boolean hsig = updateEvolutionPaths(zmean, xold);

            if (diagonalOnly <= 0) updateCovariance(hsig, bestArx, arz, arindex, xold);
            else updateCovarianceDiagonalOnly(hsig, bestArz);

            sigma *= Math.exp(Math.min(1, (normps / chiN - 1) * cs / damps));
            final double bestFitness = fitness[arindex[0]];
            final double worstFitness = fitness[arindex[arindex.length - 1]];
            ConvergenceChecker<PointValuePair> convergence = getConvergenceChecker();
            if (this.bestValue > bestFitness) {
                this.bestValue = bestFitness;
                this.lastResult = opt;

                this.opt = new PointValuePair(this.repair(bestArx.getColumn(0)), isMinimize ? bestFitness : -bestFitness);

                if (convergence != null && convergence.converged(iterations, opt, this.lastResult))
                    return false;
            }


            if (stopFitness == stopFitness && bestFitness < (isMinimize ? stopFitness : -stopFitness))
                return false;

            final double[] sqrtDiagC = sqrt(diagC).getColumn(0);
            final double[] pcCol = pc.getColumn(0);
            if (IntStream.range(0, dimension).takeWhile(i -> !(sigma * Math.max(Math.abs(pcCol[i]), sqrtDiagC[i]) > stopTolX)).anyMatch(i -> i >= dimension - 1)) {
                return false;
            }
            for (int i = 0; i < dimension; i++)
                if (sigma * sqrtDiagC[i] > stopTolUpX)
                    return false;

            final double historyBest = Util.min(fitnessHistory);
            final double historyWorst = Util.max(fitnessHistory);

            if (iterations > 2 && Math.max(historyWorst, worstFitness) - Math.min(historyBest, bestFitness) < stopTolFun)
                return false;
            if (iterations > fitnessHistory.length && historyWorst - historyBest < stopTolHistFun)
                return false;
            if (max(diagD) / min(diagD) > tENmILLION)
                return false;

            if (convergence != null) {
                final PointValuePair current = new PointValuePair(bestArx.getColumn(0), isMinimize ? bestFitness : -bestFitness);
                if (this.lastResult != null && convergence.converged(iterations, current, this.lastResult))
                    return false;
                this.lastResult = current;
            }

            if (this.bestValue == fitness[arindex[(int) (0.1 + lambda / 4.0)]])
                sigma *= Math.exp(0.2 + cs / damps);
            if (iterations > 2 && Math.max(historyWorst, bestFitness) - Math.min(historyBest, bestFitness) == 0)
                sigma *= Math.exp(0.2 + cs / damps);

            push(fitnessHistory, bestFitness);
            if (generateStatistics) {
                statisticsSigmaHistory.add(sigma);
                statisticsFitnessHistory.add(bestFitness);
                statisticsMeanHistory.add(xmean.transpose());
                statisticsDHistory.add(diagD.transpose().scalarMultiply(hUNDreDtHOUSAND));
            }
            return true;
        }

    }
}


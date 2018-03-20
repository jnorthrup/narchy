package com.jujutsu.tsne;

import com.jujutsu.tsne.barneshut.TSneConfiguration;

public class TSneConfig implements TSneConfiguration {

	@Deprecated private double[][] xin;

	private int outputDims;

	/**
	 * The perplexity can be interpreted as a smooth measure of the effective number of neighbors. The
	 * performance of SNE is fairly robust to changes in the perplexity, and typical values are between 5
	 * and 50.
	 */
	private double perplexity;

	private int max_iter;

	private boolean use_pca;
	private int pca_dims;

	/** used for BHT */
	private double theta;

	private boolean silent;
	private boolean print_error;


	public TSneConfig(double[][] xin, int outputDims, int pca_dims, double perplexity, int max_iter,
					  boolean use_pca, double theta, boolean silent, boolean print_error) {
		this.xin = xin;
		this.outputDims = outputDims;
		this.pca_dims = pca_dims;
		this.perplexity = perplexity;
		this.max_iter = max_iter;
		this.use_pca = use_pca;
		this.theta = theta;
		this.silent = silent;
		this.print_error = print_error;
	}

	/* (non-Javadoc)
	 * @see com.jujutsu.tsne.barneshut.TSneConfiguration#getXin()
	 */
	@Override
	public double[][] getXin() {
		return xin;
	}

	/* (non-Javadoc)
	 * @see com.jujutsu.tsne.barneshut.TSneConfiguration#setXin(double[][])
	 */
	@Override
	public void setXin(double[][] xin) {
		this.xin = xin;
	}

	/* (non-Javadoc)
	 * @see com.jujutsu.tsne.barneshut.TSneConfiguration#getOutputDims()
	 */
	@Override
	public int getOutputDims() {
		return outputDims;
	}

	/* (non-Javadoc)
	 * @see com.jujutsu.tsne.barneshut.TSneConfiguration#setOutputDims(int)
	 */
	@Override
	public void setOutputDims(int n) {
		this.outputDims = n;
	}

	/* (non-Javadoc)
	 * @see com.jujutsu.tsne.barneshut.TSneConfiguration#getInitialDims()
	 */
	@Override
	public int getInitialDims() {
		return pca_dims;
	}

	/* (non-Javadoc)
	 * @see com.jujutsu.tsne.barneshut.TSneConfiguration#setInitialDims(int)
	 */
	@Override
	public void setInitialDims(int initial_dims) {
		this.pca_dims = initial_dims;
	}

	/* (non-Javadoc)
	 * @see com.jujutsu.tsne.barneshut.TSneConfiguration#getPerplexity()
	 */
	@Override
	public double getPerplexity() {
		return perplexity;
	}

	/* (non-Javadoc)
	 * @see com.jujutsu.tsne.barneshut.TSneConfiguration#setPerplexity(double)
	 */
	@Override
	public void setPerplexity(double perplexity) {
		this.perplexity = perplexity;
	}

	/* (non-Javadoc)
	 * @see com.jujutsu.tsne.barneshut.TSneConfiguration#getMaxIter()
	 */
	@Override
	public int getMaxIter() {
		return max_iter;
	}

	/* (non-Javadoc)
	 * @see com.jujutsu.tsne.barneshut.TSneConfiguration#setMaxIter(int)
	 */
	@Override
	public void setMaxIter(int max_iter) {
		this.max_iter = max_iter;
	}

	/* (non-Javadoc)
	 * @see com.jujutsu.tsne.barneshut.TSneConfiguration#usePca()
	 */
	@Override
	public boolean usePca() {
		return use_pca;
	}

	/* (non-Javadoc)
	 * @see com.jujutsu.tsne.barneshut.TSneConfiguration#setUsePca(boolean)
	 */
	@Override
	public void setUsePca(boolean use_pca) {
		this.use_pca = use_pca;
	}

	/* (non-Javadoc)
	 * @see com.jujutsu.tsne.barneshut.TSneConfiguration#getTheta()
	 */
	@Override
	public double getTheta() {
		return theta;
	}

	/* (non-Javadoc)
	 * @see com.jujutsu.tsne.barneshut.TSneConfiguration#setTheta(double)
	 */
	@Override
	public void setTheta(double theta) {
		this.theta = theta;
	}

	/* (non-Javadoc)
	 * @see com.jujutsu.tsne.barneshut.TSneConfiguration#silent()
	 */
	@Override
	public boolean silent() {
		return silent;
	}

	/* (non-Javadoc)
	 * @see com.jujutsu.tsne.barneshut.TSneConfiguration#setSilent(boolean)
	 */
	@Override
	public void setSilent(boolean silent) {
		this.silent = silent;
	}

	/* (non-Javadoc)
	 * @see com.jujutsu.tsne.barneshut.TSneConfiguration#printError()
	 */
	@Override
	public boolean printError() {
		return print_error;
	}

	/* (non-Javadoc)
	 * @see com.jujutsu.tsne.barneshut.TSneConfiguration#setPrintError(boolean)
	 */
	@Override
	public void setPrintError(boolean print_error) {
		this.print_error = print_error;
	}

	@Override
	public int getXStartDim() {
		return xin[0].length;
	}

	@Override
	public int getNrRows() {
		return xin.length;
	}
	
	
}
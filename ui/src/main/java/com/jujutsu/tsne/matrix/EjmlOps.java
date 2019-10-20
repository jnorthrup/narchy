package com.jujutsu.tsne.matrix;


import org.ejml.data.DMatrix;
import org.ejml.data.DMatrixRMaj;

import static org.ejml.dense.row.CommonOps_DDRM.divide;
import static org.ejml.dense.row.CommonOps_DDRM.sumCols;

public class EjmlOps {

	public static void maximize(DMatrix p, double minval) {
		var rows = p.getNumRows();
		var cols = p.getNumCols();
		for (var i = 0; i < rows; i++) {
			for (var j = 0; j < cols; j++) {
				var val = p.get(i, j);
				if(val<minval) p.unsafe_set(j, j, minval);
			}
		}
	}

	/**
	 * Returns a new matrix of booleans where true is set if the value in the matrix is
	 * bigger than value
	 * @param fastTSne TODO
	 * @param matrix
	 * @param value
	 * @return new matrix with booelans with values matrix1[i,j] == matrix2[i,j]
	 */
	public static boolean [][] biggerThan(DMatrixRMaj matrix, double value) {
		var equals = new boolean[matrix.numRows][matrix.numCols];
		for (var i = 0; i < matrix.numRows; i++) {
			for (var j = 0; j < matrix.numCols; j++) {
				equals[i][j] = Double.compare(matrix.get(i,j), value) == 1;
			}
		}
		return equals;
	}

	/**
	 * Sets the diagonal of 'diag' to the values of 'diagElements' as long 
	 * as possible (i.e while there are elements left in diag and the dim of 'diag'
	 * is big enough...
	 * Note: This method ONLY affect the diagonal elements the others are left as
	 * when passed in.
	 * @param fastTSne TODO
	 * @param diag Modified to contain the elements of 'diagElements' on its diagonal
	 * @param diagElems
	 */
	public static void setDiag(DMatrixRMaj diag, double[] diagElems) {
		var idx = 0;
		while(idx<diag.numCols&&idx<diag.numRows&&idx<diagElems.length) {
			diag.set(idx, idx, diagElems[idx++]);
		}
	}

	/**
	 * <p>
	 * Sets the data of<code>target</code> to that of the input matrix with the values and shape defined by the 2D array 'data'.
	 * It is assumed that 'data' has a row-major formatting:<br>
	 *  <br>
	 * data[ row ][ column ]
	 * </p>
	 * @param fastTSne TODO
	 * @param target 2D DenseMatrix. Modified to contain the values in 'data'.
	 * @param data 2D array representation of the matrix. Not modified.
	 */
	public static void setData(DMatrixRMaj target, double[][] data) {
		var numRows = data.length;
		var numCols = data[0].length;

		var targetData = new double[ numRows*numCols ];

		var pos = 0;
        for (var row : data) {
            if (row.length != numCols) {
                throw new IllegalArgumentException("All rows must have the same length");
            }

            System.arraycopy(row, 0, targetData, pos, numCols);
            pos += numCols;
        }
	    
	    target.setData(targetData);
	}

	/** 
	 * Replaces NaN's with repl
	 * @param matrix
	 * @param repl
	 * @return
	 */
	public static void replaceNaN(DMatrixRMaj matrix, double repl) {
		for (var i = 0; i < matrix.numRows; i++) {
			for (var j = 0; j < matrix.numCols; j++) {
				if(Double.isNaN(matrix.get(i,j))) {
					matrix.set(i,j,repl);
				} 
			}
		}
	}

	public static DMatrixRMaj fillWithRow(DMatrixRMaj matrix, int setrow) {
		var rows = matrix.numRows;
		var cols = matrix.numCols;
		var result = new DMatrixRMaj(rows,cols);
		for (var row = 0; row < rows; row++) {
			for (var col = 0; col < cols; col++) {
				result.set(row,col, matrix.get(setrow,col));				
			}
		}
		return result;
	}

	public static DMatrixRMaj tile(DMatrixRMaj matrix, int rowtimes, int coltimes) {
		var result = new DMatrixRMaj(matrix.numRows*rowtimes,matrix.numCols*coltimes);
		for (int i = 0, resultrow = 0; i < rowtimes; i++) {
			for (var j = 0; j < matrix.numRows; j++) {
				for (int k = 0, resultcol = 0; k < coltimes; k++) {
					for (var l = 0; l < matrix.numCols; l++) {
						result.set(resultrow,resultcol++,matrix.get(j,l));
					}
				}
				resultrow++;
			}
		}
		return result;
	}

	/**
	 * All values in matrix that is less than <code>lessthan</code> is assigned
	 * the value <code>assign</code>
	 * @param matrix
	 * @param lessthan
	 * @param assign
	 * @return
	 */
	public static void assignAllLessThan(DMatrixRMaj matrix, double lessthan, double assign) {
		for (var i = 0; i < matrix.numRows; i++) {
			for (var j = 0; j < matrix.numCols; j++) {
				if( matrix.get(i,j) < lessthan) {
					matrix.set(i,j,assign);
				}
			}
		}
	}

	public static DMatrixRMaj colMean(DMatrixRMaj y, int i) {
		var colmean = new DMatrixRMaj(1,y.numCols);
		sumCols(y,colmean);
		divide(colmean, y.numRows);
		return colmean;
	}

	public static void addRowVector(DMatrixRMaj matrix, DMatrix rowvector) {
		for (var i = 0; i < matrix.numRows; i++) {
			for (var j = 0; j < matrix.numCols; j++) {
				matrix.set(i,j,matrix.get(i,j) + rowvector.get(0,j));
			}
		}
	}

	public static void assignAtIndex(DMatrix num, int[] range, int[] range1, double value) {
		for (var j = 0; j < range.length; j++) {
			num.set(range[j], range1[j], value);
		}
	}

	public static double [][] extractDoubleArray(DMatrix p) {
		var rows = p.getNumRows();
		var cols = p.getNumCols();
		var result = new double[rows][cols];
		for (var i = 0; i < rows; i++) {
			for (var j = 0; j < cols; j++) {
				result[i][j] = p.get(i, j);
			}
		}
		return result;
	}

}

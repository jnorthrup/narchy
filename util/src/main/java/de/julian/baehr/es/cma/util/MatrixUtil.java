package de.julian.baehr.es.cma.util;

import jcog.Util;
import jcog.math.tensor.Tensor;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.Arrays;


public class MatrixUtil {
	
	public static RealMatrix  identity(int size){
		return MatrixUtils.createRealIdentityMatrix(size);
	}
	

	public static RealMatrix  fromDiagonal(Tensor diagonal){
        return MatrixUtils.createRealDiagonalMatrix(Util.toDouble(diagonal.get()));
	}
	
	public static RealMatrix  repeatVertically(Tensor vector, int times){
        double[] row = Util.toDouble(vector.get());
		double[][] d = new double[times][];
		Arrays.fill(d, row);
		return MatrixUtils.createRealMatrix(d);
	}
	
	public static RealMatrix  repeatHorizontally(Tensor vector, int times){
		return repeatVertically(vector, times).transpose();
	}
	
	public static RealMatrix  upperTriangle(RealMatrix  matrix){
		return upperTriangle(matrix, 0);
	}

	public static RealMatrix  upperTriangle(RealMatrix  matrix, int start){
		double[][] values = new double[matrix.getColumnDimension()][matrix.getRowDimension()];
		
		for(int y = 0; y < values.length; y++)
			for(int x = start + y; x < values[0].length; x++)
				values[y][x] = matrix.getEntry(y, x);
		
		return new Array2DRowRealMatrix(values);
	}
	
//	public static RealMatrix floatToReal(RealMatrix  fMatrix){
//		double[][] values = new double[matrix.getColumnDimension()][matrix.getRowDimension()];
//
//		for(int row = 0; row < fMatrix.getNumberOfRows(); row++)
//			for(int col = 0; col < fMatrix.getNumberOfColumns(); col++)
//				values[row][col] = fMatrix.get(row, col).doubleValue();
//
//		return new Array2DRowRealMatrix(values);
//	}
	
//	public static RealMatrix  realToFloat(RealMatrix rMatrix){
//		return RealMatrix .valueOf(rMatrix.getData());
//	}
}

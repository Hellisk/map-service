package algorithm.mapinference.lineclustering.pcurves.Utilities;

/*
 * javah -jni algorithm.mapinference.lineclustering.pcurves.Utilities.MyMath
 * mv Utilities_MyMath.h ~/Java/Sources/CRoutines/
 * change <jni.h> to "jni.h"
 */

final public class MyMath {
	public static final double epsilon = 1e-6;
	
	static {
		if (Environment.cRoutines)
			System.loadLibrary("utilities");
	}
	
	public static int integer(double d) {
		if (d > 0)
			return (int) d;
		else
			return (int) d - 1;
	}
	
	public static int maxIndex(int[] array) {
		if (array.length == 0)
			return -1;
		int maxIndex = 0, max = array[0];
		for (int i = 0; i < array.length; i++) {
			if (max < array[i]) {
				max = array[i];
				maxIndex = i;
			}
		}
		return maxIndex;
	}
	
	public static int maxIndex(double[] array) {
		if (array.length == 0)
			return -1;
		int maxIndex = 0;
		double max = array[0];
		for (int i = 0; i < array.length; i++) {
			if (max < array[i]) {
				max = array[i];
				maxIndex = i;
			}
		}
		return maxIndex;
	}
	
	// Sorting in decreasing order
	public static void sortInsertion(int[] array) {
		if (array.length == 0)
			return;
		int maxIndex, temp;
		for (int i = 0; i < array.length; i++) {
			maxIndex = i;
			for (int j = i + 1; j < array.length; j++)
				if (array[maxIndex] < array[j])
					maxIndex = j;
			temp = array[maxIndex];
			array[maxIndex] = array[i];
			array[i] = temp;
		}
	}
	
	private static native void IndexArrayC(double[] array, int[] indices);
	
	public static void IndexArray(double[] array, int[] indices) {
		if (!Environment.cRoutines) {
			if (array.length == 0)
				return;
			int minIndex;
			int temp;
			for (int i = 0; i < array.length; i++)
				indices[i] = i;
			for (int i = 0; i < array.length; i++) {
				minIndex = i;
				for (int j = i + 1; j < array.length; j++)
					if (array[indices[minIndex]] > array[indices[j]])
						minIndex = j;
				temp = indices[minIndex];
				indices[minIndex] = indices[i];
				indices[i] = temp;
			}
		} else
			IndexArrayC(array, indices);
	}
	
	public static void sortInsertion(double[] array) {
		if (array.length == 0)
			return;
		int maxIndex;
		double temp;
		for (int i = 0; i < array.length; i++) {
			maxIndex = i;
			for (int j = i + 1; j < array.length; j++)
				if (array[maxIndex] < array[j])
					maxIndex = j;
			temp = array[maxIndex];
			array[maxIndex] = array[i];
			array[i] = temp;
		}
	}
	
	private static native int equalsC(double d1, double d2, double epsilon);
	
	public static boolean equals(double d1, double d2) {
		if (!Environment.cRoutines) {
			if (d1 == 0.0) {
				if (d2 == 0.0)
					return true;
				else
					return false;
//			} else if (Math.abs(d2 / d1 - 1.0) < epsilon)
			} else if (d2 == d1)
				return true;
			else
				return false;
		} else {
			if (equalsC(d1, d2, epsilon) == 1)
				return true;
			else
				return false;
		}
	}
	
	public static int sign(double d) {
		if (equals(d, 0.0))
			return 0;
		if (d > 0)
			return 1;
		return -1;
	}
	
	public static int sign(int d) {
		if (d == 0)
			return 0;
		if (d > 0)
			return 1;
		return -1;
	}
	
	public static double order(double d) {
		int i = 0;
		if (d > 1) {
			while (d > 1) {
				d /= 10;
				i++;
			}
			i--;
		} else {
			while (d < 1) {
				d *= 10;
				i--;
			}
		}
		return Math.pow(10, i);
	}
	
	public static double TruncateDouble(double d, int numOfDecimals) {
		int i = (int) (d * Math.pow(10, numOfDecimals));
		return i / Math.pow(10, numOfDecimals);
	}
	
	public static double RoundDouble(double d, int numOfDecimals) {
		int i = (int) (d * Math.pow(10, numOfDecimals) + 0.5);
		return i / Math.pow(10, numOfDecimals);
	}
	
	public static double Acos(double d) {
		if (equals(d, -1))
			return Math.PI;
		else
			return Math.acos(d);
	}
	
	private static native int MatrixInvertC(double[] matrix, int size);
	
	public static void MatrixInvert(double[][] matrix) {
		int size = matrix.length;
		if (!Environment.cRoutines) {
			int i, j, k, l, ll;
			int[] ipiv = new int[size];
			int[] indxc = new int[size];
			int[] indxr = new int[size];
			int icol = -1, irow = -1;
			double big, dum, pivinv, temp;
			
			for (j = 0; j < size; j++)
				ipiv[j] = 0;
			for (i = 0; i < size; i++) {
				big = 0.0;
				for (j = 0; j < size; j++) {
					if (ipiv[j] != 1) {
						for (k = 0; k < size; k++) {
							if (ipiv[k] == 0) {
								if (Math.abs(matrix[j][k]) >= big) {
									big = Math.abs(matrix[j][k]);
									irow = j;
									icol = k;
								}
							} else if (ipiv[k] > 1) {
								throw new ArithmeticException("Singular matrix 1 (from java code)");
							}
						}
					}
				}
				ipiv[icol]++;
				if (irow != icol) {
					for (l = 0; l < size; l++) {
						temp = matrix[irow][l];
						matrix[irow][l] = matrix[icol][l];
						matrix[icol][l] = temp;
					}
				}
				indxr[i] = irow;
				indxc[i] = icol;
				if (matrix[icol][icol] == 0.0) {
					throw new ArithmeticException("Singular matrix 2 (from java code)");
				}
				pivinv = 1.0 / matrix[icol][icol];
				matrix[icol][icol] = 1.0;
				for (l = 0; l < size; l++)
					matrix[icol][l] *= pivinv;
				for (ll = 0; ll < size; ll++) {
					if (ll != icol) {
						dum = matrix[ll][icol];
						matrix[ll][icol] = 0.0;
						for (l = 0; l < size; l++)
							matrix[ll][l] -= matrix[icol][l] * dum;
					}
				}
			}
			for (l = size - 1; l >= 0; l--) {
				if (indxr[l] != indxc[l]) {
					for (k = 0; k < size; k++) {
						temp = matrix[k][indxr[l]];
						matrix[k][indxr[l]] = matrix[k][indxc[l]];
						matrix[k][indxc[l]] = temp;
					}
				}
			}
		} else {
			double[] matrixVector = new double[size * size];
			for (int i = 0; i < size; i++)
				for (int j = 0; j < size; j++)
					matrixVector[i * size + j] = matrix[i][j];
			
			int r = MatrixInvertC(matrixVector, size);
			if (r == -1)
				throw new ArithmeticException("Singular matrix 1 (from C code)");
			else if (r == -2)
				throw new ArithmeticException("Singular matrix 2 (from C code)");
			
			for (int i = 0; i < size; i++)
				for (int j = 0; j < size; j++)
					matrix[i][j] = matrixVector[i * size + j];
		}
	}
	
	public static void main(String args[]) {
		for (int size = 1; size < 1000; size++) {
			double[][] elements = new double[size][];
			double[][] elementsI = new double[size][];
			for (int i = 0; i < size; i++) {
				elements[i] = new double[size];
				elementsI[i] = new double[size];
				for (int j = 0; j < size; j++)
					elements[i][j] = elementsI[i][j] = 10 * Math.random();
			}
			MatrixInvert(elementsI);
		}
	}
}

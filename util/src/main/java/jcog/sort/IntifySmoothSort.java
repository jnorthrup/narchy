package jcog.sort;

import java.util.function.ToIntFunction;

/**
 * variation that uses IntFunction<X> instead of comparators, for faster cached comparisons
 */
public enum IntifySmoothSort {
	;


	public static <X> void smoothSort(X[] m, int lo, int hi, ToIntFunction<X> compare) {

        assert(hi < LP[32]): "Maximum length exceeded for smoothsort implementation";

		int head = lo, p = 1, pshift = 1;

		while (head < hi) {
			if ((p & 3) == 3) {
				sift(m, pshift, head, compare);
				p >>>= 2;
				pshift += 2;
			} else {
				if (LP[pshift - 1] >= hi - head)
					trinkle(m, p, pshift, head, false, compare);
				else
					sift(m, pshift, head, compare);

				if (pshift == 1) {
					p <<= 1;
					pshift--;
				} else {
					p <<= (pshift - 1);
					pshift = 1;
				}
			}
			p |= 1;
			head++;

		}

		trinkle(m, p, pshift, head, false, compare);

		while (pshift != 1 || p != 1) {
			if (pshift <= 1) {
				int trail = Integer.numberOfTrailingZeros(p & ~1);
				p >>>= trail;
				pshift += trail;
			} else {
				p <<= 2;
				p ^= 7;
				pshift -= 2;

				trinkle(m, p >>> 1, pshift + 1, head - LP[pshift] - 1, true, compare);
				trinkle(m, p, pshift, head - 1, true, compare);
			}

			head--;
		}
	}

	private static <X> void trinkle(X[] m, int p, int pshift, int head, boolean trusty, ToIntFunction<X> i) {
		final X val = m[head];
		int vval = i.applyAsInt(val);

		while (p != 1) {
			int stepson = head - LP[pshift];
            X mstepson = m[stepson];

			int vstepson = i.applyAsInt(mstepson);
			if (vstepson >= vval)
				break;

			if (!trusty && pshift > 1) {
				int rt = head - 1;
                int lf = head - 1 - LP[pshift - 2];
				if (i.applyAsInt(m[rt]) <= vstepson || i.applyAsInt(m[lf]) <= vstepson)
					break;
			}

			m[head] = mstepson;

			head = stepson;
			int trail = Integer.numberOfTrailingZeros(p & ~1);
			p >>>= trail;
			pshift += trail;
			trusty = false;
		}

		if (!trusty) {
			m[head] = val;
			sift(m, pshift, head, i);
		}
	}

	private static <X> void sift(X[] m, int pshift, int head, ToIntFunction<X> i) {
		final X val = m[head];
		int vval = i.applyAsInt(val);

		while (pshift > 1) {
			int rt = head - 1;
			int lf = head - 1 - LP[pshift - 2];

			X mlf = m[lf], mrt = m[rt];
			int vlf = i.applyAsInt(mlf), vrt = i.applyAsInt(mrt);
			if (vval <= vlf && vval <= vrt)
			    break;

			if (vlf <= vrt) {
				m[head] = mlf;
				head = lf;
				pshift--;
			} else {
				m[head] = mrt;
				head = rt;
				pshift -= 2;
			}
		}
		m[head] = val;
	}


    /** Leonardo numbers */
	private static final int[] LP = {
        1, 1, 3, 5, 9, 15, 25, 41, 67, 109, 177, 287, 465, 753,
        1219, 1973, 3193, 5167, 8361, 13529, 21891, 35421, 57313, 92735,
        150049, 242785, 392835, 635621, 1028457, 1664079, 2692537,
        4356617, 7049155, 11405773, 18454929, 29860703, 48315633, 78176337,
        126491971, 204668309, 331160281, 535828591, 866988873
    };

	//	private static int trailingzeroes(int v) {
//		//return MultiplyDeBruijnBitPosition[(((v & -v) * 0x077CB531) >> 27) & 0x1f];
//		return Integer.numberOfTrailingZeros(v);
//	}
//    // Solution for determining number of trailing zeroes of a number's binary representation.
//    // Taken from http://www.0xe3.com/text/ntz/ComputingTrailingZerosHOWTO.html
//    private static final int[] MultiplyDeBruijnBitPosition = new int[]{
//        0, 1, 28, 2, 29, 14, 24, 3,
//        30, 22, 20, 15, 25, 17, 4, 8,
//        31, 27, 13, 23, 21, 19, 16, 7,
//        26, 12, 18, 6, 11, 5, 10, 9};

}

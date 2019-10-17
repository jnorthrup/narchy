package nars.ca;




import java.util.StringTokenizer;
import java.util.stream.IntStream;

public class RuleMarg {
	public static final int TYPE_MS = 1; 
	public int iClo; 
	public int iTyp; 
	public boolean isHist; 
	public int[] swapArray = new int[16];

	
	public RuleMarg() {
		ResetToDefaults();
	}

	
	
	public void ResetToDefaults() {
		iClo = 2; 
		isHist = true;
		iTyp = TYPE_MS; 
		for (int i = 0; i <= 15; i++)
			swapArray[i] = i;
	}

	
	
	
	public void InitFromString(String sStr) {
		String sTok, sSwaps;
		int i, iNum, iVal;
		
		StringTokenizer st, std;

		ResetToDefaults();

		st = new StringTokenizer(sStr, ",", true);
		while (st.hasMoreTokens()) {
			sTok = st.nextToken();
			if (sTok.length() > 0 && sTok.charAt(0) == 'M') 
															
			{
				iTyp = TYPE_MS; 
			} else if (sTok.length() > 0 && sTok.charAt(0) == 'D') 
			{
				std = new StringTokenizer(sTok.substring(1), ";", false);
				iNum = 0;
				while (std.hasMoreTokens() && (iNum <= 15)) {
					sSwaps = std.nextToken();
					iVal = Integer.valueOf(sSwaps);
					if ((iVal >= 0) && (iVal <= 15))
						swapArray[iNum] = iVal;
					iNum++;
				}
			}
		}

		Validate(); 
	}

	
	
	public void InitFromPrm(int i_Clo, boolean is_Hist, int[] ary) {
		ResetToDefaults();
		iClo = i_Clo; 
		isHist = is_Hist; 
		swapArray = ary;
		Validate(); 
	}

	
	
	
	public String GetAsString() {
		
		Validate();

		
		String sBff = "MS,D";

		for (int i = 0; i <= 14; i++)
			sBff = sBff + swapArray[i] + ';';
		sBff = sBff + swapArray[15];

		return sBff;
	}

	
	
	
	public void Validate() {
		if (iClo < 2)
			iClo = 2;
		else if (iClo > MJBoard.MAX_CLO)
			iClo = MJBoard.MAX_CLO;

		for (int i = 0; i <= 15; i++)
			if ((swapArray[i] < 0) || (swapArray[i] > 15))
				swapArray[i] = i;
	}

	
	
	private void SwapMargCells(int[] mgCells) {
		int iCnt, iNewCnt;

		
		if (IntStream.of(0, 1, 2, 3).allMatch(i -> (mgCells[i] < 2))) {
			iCnt = 0;
			if (mgCells[0] > 0)
				iCnt += 1;
			if (mgCells[1] > 0)
				iCnt += 2;
			if (mgCells[2] > 0)
				iCnt += 4;
			if (mgCells[3] > 0)
				iCnt += 8;
			iNewCnt = swapArray[iCnt];

			mgCells[0] = (1 & iNewCnt) > 0 ? 1 : 0;
			mgCells[1] = (2 & iNewCnt) > 0 ? 1 : 0;
			mgCells[2] = (4 & iNewCnt) > 0 ? 1 : 0;
			mgCells[3] = (8 & iNewCnt) > 0 ? 1 : 0;
		}
	}

	
	
	public int OnePass(int sizX, int sizY, boolean isWrap, int ColoringMethod,
			short[][] crrState, short[][] tmpState, MJBoard mjb) {
		int modCnt = 0;
		int i, j, ic;
		int c1, c2, r1, r2;
		int[] mgCells = new int[4]; 
		int[] mgCellsOld = new int[4]; 
		boolean isOdd; 

		isOdd = ((mjb.Cycle % 2) != 0);
		i = 0;
		if (isOdd)
			i--; 
		while (i < sizX) {
			c1 = i;
			if (c1 < 0)
				c1 = (isWrap) ? sizX - 1 : sizX; 
			c2 = i + 1;
			if (c2 >= sizX)
				c2 = (isWrap) ? 0 : sizX;
			j = 0;
			if (isOdd)
				j--; 
			while (j < sizY) {
				r1 = j;
				if (r1 < 0)
					r1 = (isWrap) ? sizY - 1 : sizY; 
				r2 = j + 1;
				if (r2 >= sizY)
					r2 = (isWrap) ? 0 : sizY;
				mgCellsOld[0] = mgCells[0] = tmpState[c1][r1] = crrState[c1][r1]; 
				mgCellsOld[1] = mgCells[1] = tmpState[c2][r1] = crrState[c2][r1]; 
				mgCellsOld[2] = mgCells[2] = tmpState[c1][r2] = crrState[c1][r2]; 
				mgCellsOld[3] = mgCells[3] = tmpState[c2][r2] = crrState[c2][r2]; 

				if ((IntStream.of(0, 1, 2, 3).map(v -> mgCells[v]).sum() > 0)
						|| (swapArray[0] > 0)) {
					SwapMargCells(mgCells); 

					for (ic = 0; ic <= 3; ic++) 
												
					{
						if (mgCellsOld[ic] != mgCells[ic]) 
						{
							modCnt++; 
							switch (ic) {
								case 0 :
									tmpState[c1][r1] = (short) mgCells[ic];
									break; 
								case 1 :
									tmpState[c2][r1] = (short) mgCells[ic];
									break; 
								case 2 :
									tmpState[c1][r2] = (short) mgCells[ic];
									break; 
								case 3 :
									tmpState[c2][r2] = (short) mgCells[ic];
									break; 
							}
						}
					}
				}
				j += 2;
			}
			i += 2;
		}
		if ((modCnt == 0) && (mjb.Population > 0))
			modCnt = 1;

		return modCnt;
	}
	
}
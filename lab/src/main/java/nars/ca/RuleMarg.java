package nars.ca;




import java.util.StringTokenizer;

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
        int i;

        ResetToDefaults();

        StringTokenizer st = new StringTokenizer(sStr, ",", true);
		while (st.hasMoreTokens()) {
            String sTok = st.nextToken();
            if (sTok.length() > 0 && (int) sTok.charAt(0) == (int) 'M')
															
			{
				iTyp = TYPE_MS; 
			} else if (sTok.length() > 0 && (int) sTok.charAt(0) == (int) 'D')
			{
                StringTokenizer std = new StringTokenizer(sTok.substring(1), ";", false);
                int iNum = 0;
                while (std.hasMoreTokens() && (iNum <= 15)) {
                    String sSwaps = std.nextToken();
                    int iVal = Integer.valueOf(sSwaps);
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
		sBff += swapArray[15];

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


		boolean b = true;
		for (int i : new int[]{0, 1, 2, 3}) {
			if ((mgCells[i] >= 2)) {
				b = false;
				break;
			}
		}
		if (b) {
            int iCnt = 0;
            if (mgCells[0] > 0)
				iCnt += 1;
			if (mgCells[1] > 0)
				iCnt += 2;
			if (mgCells[2] > 0)
				iCnt += 4;
			if (mgCells[3] > 0)
				iCnt += 8;
            int iNewCnt = swapArray[iCnt];

            mgCells[0] = (1 & iNewCnt) > 0 ? 1 : 0;
			mgCells[1] = (2 & iNewCnt) > 0 ? 1 : 0;
			mgCells[2] = (4 & iNewCnt) > 0 ? 1 : 0;
			mgCells[3] = (8 & iNewCnt) > 0 ? 1 : 0;
		}
	}

	
	
	public int OnePass(int sizX, int sizY, boolean isWrap, int ColoringMethod,
			short[][] crrState, short[][] tmpState, MJBoard mjb) {

        boolean isOdd = ((mjb.Cycle % 2) != 0);
        int i = 0;
		if (isOdd)
			i--;
        int[] mgCellsOld = new int[4];
        int[] mgCells = new int[4];
        int modCnt = 0;
        while (i < sizX) {
            int c1 = i;
            if (c1 < 0)
				c1 = (isWrap) ? sizX - 1 : sizX;
            int c2 = i + 1;
            if (c2 >= sizX)
				c2 = (isWrap) ? 0 : sizX;
            int j = 0;
            if (isOdd)
				j--; 
			while (j < sizY) {
                int r1 = j;
                if (r1 < 0)
					r1 = (isWrap) ? sizY - 1 : sizY;
                int r2 = j + 1;
                if (r2 >= sizY)
					r2 = (isWrap) ? 0 : sizY;
				mgCellsOld[0] =(short)( mgCells[0] =  tmpState[c1][r1] = crrState[c1][r1] );
				mgCellsOld[1] =(short)( mgCells[1] =  tmpState[c2][r1] = crrState[c2][r1] );
				mgCellsOld[2] =(short)( mgCells[2] =  tmpState[c1][r2] = crrState[c1][r2] );
				mgCellsOld[3] =(short)( mgCells[3] =  tmpState[c2][r2] = crrState[c2][r2] );

				int sum = 0;
				for (int v : new int[]{0, 1, 2, 3}) {
					int mgCell = mgCells[v];
					sum += mgCell;
				}
				if ((sum > 0)
						|| (swapArray[0] > 0)) {
					SwapMargCells(mgCells); 

					for (int ic = 0; ic <= 3; ic++)
												
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
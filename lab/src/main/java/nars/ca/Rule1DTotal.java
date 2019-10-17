package nars.ca;




import java.util.StringTokenizer;

public class Rule1DTotal {
	public static final int MAX_RANGE = 10;

	public boolean isHist; 
	public boolean isCentr; 
	public int iClo; 
	public int iRng; 
	public boolean[] rulesS = new boolean[MAX_RANGE * 2 + 2]; 
																
	public boolean[] rulesB = new boolean[MAX_RANGE * 2 + 2]; 

	
	public Rule1DTotal() {
		ResetToDefaults();
	}

	
	
	public void ResetToDefaults() {
		int i;
		isHist = false; 
		iClo = 2; 
		isCentr = true; 
		iRng = 2; 
		for (i = 0; i <= MAX_RANGE * 2 + 1; i++) {
			rulesS[i] = false; 
			rulesB[i] = false; 
		}
	}

	
	
	
	public void InitFromString(String sStr) {

        String sTok;
		int iTmp;
		ResetToDefaults();

        StringTokenizer st = new StringTokenizer(sStr, ",", true);
		while (st.hasMoreTokens()) {
			sTok = st.nextToken().toUpperCase();
			
			
			if (sTok.length() > 0 && sTok.charAt(0) == 'R')
				iRng = Integer.valueOf(sTok.substring(1));
			else if (sTok.length() > 0 && sTok.charAt(0) == 'C') {
				iTmp = Integer.valueOf(sTok.substring(1));
				if (iTmp >= 3) {
					isHist = true; 
					iClo = iTmp;
				} else
					isHist = false; 
			} else if (sTok.length() > 0 && sTok.charAt(0) == 'M') 
																	
			{
				isCentr = (Integer.valueOf(sTok.substring(1)) > 0);
			} else if (sTok.length() > 0 && sTok.charAt(0) == 'S') 
																	
			{
				iTmp = Integer.valueOf(sTok.substring(1));
				if ((iTmp >= 0) && (iTmp <= MAX_RANGE * 2 + 1)) {
					rulesS[iTmp] = true;
				}
			} else if (sTok.length() > 0 && sTok.charAt(0) == 'B') 
																	
			{
				iTmp = Integer.valueOf(sTok.substring(1));
				if ((iTmp >= 0) && (iTmp <= MAX_RANGE * 2 + 1)) {
					rulesB[iTmp] = true;
				}
			}
			if (!isHist)
				iClo = 8;
			Validate(); 
		}
	}

	
	
	public void InitFromPrm(int i_Clo, int i_Rng, boolean is_Hist,
			boolean is_Centr, boolean[] rules_S, boolean[] rules_B) {
		isHist = is_Hist; 
		iClo = i_Clo; 
		isCentr = is_Centr; 
		iRng = i_Rng; 
		rulesS = rules_S; 
		rulesB = rules_B; 

		Validate(); 
	}

	
	
	
	public String GetAsString() {
        int i;


        Validate();

        String sBff = 'R' + String.valueOf(iRng);


        int ih = isHist ? iClo : 0;
		sBff = sBff + ",C" + ih;

		
		sBff = sBff + (isCentr ? ",M1" : ",M0");

		
		for (i = 0; i <= MAX_RANGE * 2 + 1; i++)
			if (rulesS[i])
				sBff = sBff + ",S" + i;

		
		for (i = 0; i <= MAX_RANGE * 2 + 1; i++)
			if (rulesB[i])
				sBff = sBff + ",B" + i;

		return sBff;
	}

	
	
	
	public void Validate() {
		int i, iMax;

		if (iClo < 2)
			iClo = 2;
		else if (iClo > MJBoard.MAX_CLO)
			iClo = MJBoard.MAX_CLO;

		if (iRng < 1)
			iRng = 1;
		else if (iRng > MAX_RANGE)
			iRng = MAX_RANGE;
	}

	
	
	public int OnePass(int sizX, int sizY, boolean isWrap, int ColoringMethod,
			short[][] crrState, short[][] tmpState, MJBoard mjb) {
		short bOldVal, bNewVal;
        int i, iCnt;
        int[] xVector = new int[21];
        int ic;

        int ary1DOfs = iRng;
        short[] OneRow = new short[sizX + 1 + 2 * ary1DOfs];

        int i1DNextRow = mjb.i1DLastRow + 1;
		if (i1DNextRow >= sizY)
			i1DNextRow = 0;

		for (ic = 0; ic < sizX; ic++)
			OneRow[ic + ary1DOfs] = crrState[ic][mjb.i1DLastRow]; 
																	
		if (isWrap) {
			for (ic = 1; ic <= ary1DOfs; ic++) {
				OneRow[ary1DOfs - ic] = OneRow[sizX - 1 - ic + 1];
				OneRow[sizX - 1 + ic] = OneRow[ary1DOfs + ic - 1];
			}
		}

		for (ic = 0; ic < sizX; ic++) 
		{
			bOldVal = OneRow[ic + ary1DOfs];
			iCnt = 0; 
			if (isHist) 
			{
				if (bOldVal <= 1) 
				{
					if (isCentr) 
						if (OneRow[ic + ary1DOfs] == 1)
							iCnt++;
					for (i = 1; i <= iRng; i++) 
					{
						if (OneRow[ic - i + ary1DOfs] == 1)
							iCnt++;
						if (OneRow[ic + i + ary1DOfs] == 1)
							iCnt++;
					}

					bNewVal = bOldVal; 

					
					if (bOldVal == 0) 
					{
						if (rulesB[iCnt]) 
							bNewVal = 1; 
					} else 
					{
						if (rulesS[iCnt]) 
						{
							bNewVal = 1;
						} else 
						{
							bNewVal = bOldVal < (iClo - 1)
									? (short) (bOldVal + 1)
									: 0;
						}
					}
				} else 
				{
					bNewVal = bOldVal < (iClo - 1) ? (short) (bOldVal + 1) : 0;
				}
			} else 
			{
				if (isCentr) 
					if (OneRow[ic + ary1DOfs] > 0)
						iCnt++;
				for (i = 1; i <= iRng; i++) 
				{
					if (OneRow[ic - i + ary1DOfs] > 0)
						iCnt++;
					if (OneRow[ic + i + ary1DOfs] > 0)
						iCnt++;
				}

				bNewVal = bOldVal; 

				
				if (bOldVal == 0) 
				{
					if (rulesB[iCnt]) 
						bNewVal = ColoringMethod == 1 ? 1 : (short) (mjb.Cycle
								% (mjb.StatesCount - 1) + 1);
				} else 
				{
					if (rulesS[iCnt]) 
					{
						if (ColoringMethod == 1) 
						{
							bNewVal = (short) (bOldVal < mjb.StatesCount - 1 ? bOldVal + 1 : mjb.StatesCount - 1);
						} else {
							
						}
					} else
						bNewVal = 0; 
				}
			}
			tmpState[ic][i1DNextRow] = bNewVal;
		}

        int modCnt = 1;
		mjb.i1DLastRow = i1DNextRow; 

		return modCnt;
	}
}

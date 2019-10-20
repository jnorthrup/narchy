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
        isHist = false;
		iClo = 2; 
		isCentr = true; 
		iRng = 2; 
		for (int i = 0; i <= MAX_RANGE * 2 + 1; i++) {
			rulesS[i] = false; 
			rulesB[i] = false; 
		}
	}

	
	
	
	public void InitFromString(String sStr) {

        ResetToDefaults();

        StringTokenizer st = new StringTokenizer(sStr, ",", true);
		while (st.hasMoreTokens()) {
            String sTok = st.nextToken().toUpperCase();


            int iTmp;
            if (sTok.length() > 0 && (int) sTok.charAt(0) == (int) 'R')
				iRng = Integer.valueOf(sTok.substring(1));
			else if (sTok.length() > 0 && (int) sTok.charAt(0) == (int) 'C') {
				iTmp = Integer.valueOf(sTok.substring(1));
				if (iTmp >= 3) {
					isHist = true; 
					iClo = iTmp;
				} else
					isHist = false; 
			} else if (sTok.length() > 0 && (int) sTok.charAt(0) == (int) 'M')
																	
			{
				isCentr = (Integer.valueOf(sTok.substring(1)) > 0);
			} else if (sTok.length() > 0 && (int) sTok.charAt(0) == (int) 'S')
																	
			{
				iTmp = Integer.valueOf(sTok.substring(1));
				if ((iTmp >= 0) && (iTmp <= MAX_RANGE * 2 + 1)) {
					rulesS[iTmp] = true;
				}
			} else if (sTok.length() > 0 && (int) sTok.charAt(0) == (int) 'B')
																	
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


        Validate();

        String sBff = 'R' + String.valueOf(iRng);


        int ih = isHist ? iClo : 0;
		sBff = sBff + ",C" + ih;


        sBff += (isCentr ? ",M1" : ",M0");


        int i;
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
        int[] xVector = new int[21];

        int ary1DOfs = iRng;

        int i1DNextRow = mjb.i1DLastRow + 1;
		if (i1DNextRow >= sizY)
			i1DNextRow = 0;

        short[] OneRow = new short[sizX + 1 + 2 * ary1DOfs];
        int ic;
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
            short bOldVal = OneRow[ic + ary1DOfs];
            int iCnt = 0;
            int i;
            short bNewVal;
            if (isHist)
			{
				if ((int) bOldVal <= 1)
				{
					if (isCentr) 
						if ((int) OneRow[ic + ary1DOfs] == 1)
							iCnt++;
					for (i = 1; i <= iRng; i++) 
					{
						if ((int) OneRow[ic - i + ary1DOfs] == 1)
							iCnt++;
						if ((int) OneRow[ic + i + ary1DOfs] == 1)
							iCnt++;
					}

					bNewVal = bOldVal; 

					
					if ((int) bOldVal == 0)
					{
						if (rulesB[iCnt]) 
							bNewVal = (short) 1;
					} else 
					{
						if (rulesS[iCnt]) 
						{
							bNewVal = (short) 1;
						} else 
						{
							bNewVal = (int) bOldVal < (iClo - 1)
									? (short) ((int) bOldVal + 1)
									: (short) 0;
						}
					}
				} else 
				{
					bNewVal = (int) bOldVal < (iClo - 1) ? (short) ((int) bOldVal + 1) : (short) 0;
				}
			} else 
			{
				if (isCentr) 
					if ((int) OneRow[ic + ary1DOfs] > 0)
						iCnt++;
				for (i = 1; i <= iRng; i++) 
				{
					if ((int) OneRow[ic - i + ary1DOfs] > 0)
						iCnt++;
					if ((int) OneRow[ic + i + ary1DOfs] > 0)
						iCnt++;
				}

				bNewVal = bOldVal; 

				
				if ((int) bOldVal == 0)
				{
					if (rulesB[iCnt]) 
						bNewVal = ColoringMethod == 1 ? (short) 1 : (short) (mjb.Cycle
								% (mjb.StatesCount - 1) + 1);
				} else 
				{
					if (rulesS[iCnt]) 
					{
						if (ColoringMethod == 1) 
						{
							bNewVal = (short) ((int) bOldVal < mjb.StatesCount - 1 ? (int) bOldVal + 1 : mjb.StatesCount - 1);
						} else {
							
						}
					} else
						bNewVal = (short) 0;
				}
			}
			tmpState[ic][i1DNextRow] = bNewVal;
		}

        mjb.i1DLastRow = i1DNextRow;

        int modCnt = 1;
        return modCnt;
	}
}

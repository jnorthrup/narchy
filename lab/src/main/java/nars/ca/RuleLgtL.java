package nars.ca;




import java.util.StringTokenizer;

public class RuleLgtL {
	public boolean isHist; 
	public int iClo; 
	public int iRng; 
	public int iNgh; 
	public int iSMin;
    public int iSMax;
	public int iBMin;
    public int iBMax;
	public boolean isCentr; 

	public static final int MAX_RANGE = 10;

	
	public RuleLgtL() {
		ResetToDefaults();
	}

	
	
	public void ResetToDefaults() {
		isHist = false; 
		iClo = 2; 
		iRng = 5; 
		iNgh = MJRules.NGHTYP_MOOR; 
		iSMin = 34;
		iSMax = 58; 
		iBMin = 34;
		iBMax = 45; 
		isCentr = true; 
	}

	
	
	
	public void InitFromString(String sStr) {

		ResetToDefaults();

        StringTokenizer st = new StringTokenizer(sStr, ",", true);
		while (st.hasMoreTokens()) {
            String sTok = st.nextToken().toUpperCase();
			sTok = sTok.trim();


			int iTmp;
			if (sTok.length() > 0 && sTok.charAt(0) == 'R')
			{
				iRng = Integer.valueOf(sTok.substring(1));
			} else if (sTok.length() > 0 && sTok.charAt(0) == 'C') 
																	
			{
				int i = Integer.valueOf(sTok.substring(1));
				if (i >= 3) {
					isHist = true; 
					iClo = i;
				} else
					isHist = false; 
			} else if (sTok.length() > 0 && sTok.charAt(0) == 'M') 
																	
			{
				isCentr = (Integer.valueOf(sTok.substring(1)) > 0);
			} else if (sTok.startsWith("NM")) 
			{
				iNgh = MJRules.NGHTYP_MOOR;
			} else if (sTok.startsWith("NN")) 
			{
				iNgh = MJRules.NGHTYP_NEUM;
			} else if (sTok.length() > 0 && sTok.charAt(0) == 'S') 
																	
			{
				if (sTok.length() >= 4) {
					iTmp = sTok.indexOf("..");
					if (iTmp >= 0) {
                        String sBff = sTok.substring(1, iTmp);
						iSMin = Integer.valueOf(sBff);
						sBff = sTok.substring(iTmp + 2);
						iSMax = Integer.valueOf(sBff);
					}
				}
			} else if (sTok.length() > 0 && sTok.charAt(0) == 'B') 
																	
			{
				if (sTok.length() >= 4) {
					iTmp = sTok.indexOf("..");
					if (iTmp >= 0) {
						iBMin = Integer.valueOf(sTok.substring(1, iTmp));
						iBMax = Integer.valueOf(sTok.substring(iTmp + 2));
					}
				}
			}
		}

		
		Validate(); 
	}

	
	
	public void InitFromPrm(boolean is_Hist, int i_Clo, int i_Rng, int i_Ngh,
			int i_SMin, int i_SMax, int i_BMin, int i_BMax, boolean is_Centr) {
		isHist = is_Hist; 
		iClo = i_Clo; 
		iRng = i_Rng; 
		iNgh = i_Ngh; 
		iSMin = i_SMin;
		iSMax = i_SMax; 
		iBMin = i_BMin;
		iBMax = i_BMax; 
		isCentr = is_Centr; 

		Validate(); 
	}

	
	
	
	public String GetAsString() {


        Validate();


        String sBff = 'R' + String.valueOf(iRng);


        int ih = isHist ? iClo : 0;
		sBff = sBff + ",C" + ih;


        sBff += (isCentr ? ",M1" : ",M0");

		
		sBff = sBff + ",S" + iSMin + ".." + iSMax;

		
		sBff = sBff + ",B" + iBMin + ".." + iBMax;


        sBff += (iNgh == MJRules.NGHTYP_NEUM ? ",NN" : ",NM");

		return sBff;
	}

	
	
	public void Validate() {

		if (iClo < 2)
			iClo = 2;
		else if (iClo > MJBoard.MAX_CLO)
			iClo = MJBoard.MAX_CLO;

		if (iRng < 1)
			iRng = 1;
		else if (iRng > MAX_RANGE)
			iRng = MAX_RANGE;

		if (iNgh != MJRules.NGHTYP_NEUM)
			iNgh = MJRules.NGHTYP_MOOR;

        int iMax = isCentr ? 1 : 0;
		for (int i = 1; i <= iRng; i++)

            iMax += i * 8;

		iSMin = BoundInt(1, iSMin, iMax);
		iSMax = BoundInt(1, iSMax, iMax);
		iBMin = BoundInt(1, iBMin, iMax);
		iBMax = BoundInt(1, iBMax, iMax);
	}

	
	private static int BoundInt(int iMin, int iVal, int iMax) {
		if (iVal < iMin)
			return iMin;
		if (iVal > iMax)
			return iMax;
		return iVal;
	}

	
	
	public int OnePass(int sizX, int sizY, boolean isWrap, int ColoringMethod,
			short[][] crrState, short[][] tmpState, MJBoard mjb) {
        int modCnt = 0;
        int[] lurd = new int[4];
        int[] xVector = new int[21];
        int[] yVector = new int[21];
		int iTmpC, iTmpR, iTmpBlobC, iTmpBlobR;
		int ctrCol, ctrRow;
        boolean fMoore = (iNgh == MJRules.NGHTYP_MOOR);
														

		for (int i = 0; i < sizX; i++) {
			for (int j = 0; j < sizY; j++) {
				
				
				xVector[10] = i;
				yVector[10] = j;
				for (int iTmp = 1; iTmp <= iRng; iTmp++) {
                    int colL = i - iTmp;
					xVector[10 - iTmp] = colL >= 0 ? colL : sizX + colL;

                    int colR = i + iTmp;
					xVector[10 + iTmp] = colR < sizX ? colR : colR - sizX;

                    int rowT = j - iTmp;
					yVector[10 - iTmp] = rowT >= 0 ? rowT : sizY + rowT;

                    int rowB = j + iTmp;
					yVector[10 + iTmp] = rowB < sizY ? rowB : rowB - sizY;
				}
                short bOldVal = crrState[i][j];
                short bNewVal = bOldVal;
				if (bNewVal >= iClo)
					bNewVal = (short) (iClo - 1);

                int iCnt = 0;
				int ir;
				int ic;
				if (isHist) {
					if (bOldVal <= 1) 
					{
						for (ic = 10 - iRng; ic <= 10 + iRng; ic++) {
							for (ir = 10 - iRng; ir <= 10 + iRng; ir++) {
								if ((isCentr) || (ic != i) || (ir != j)) {
									if ((fMoore)
											|| ((Math.abs(ic - 10) + Math
													.abs(ir - 10)) <= iRng)) {
										if (crrState[xVector[ic]][yVector[ir]] == 1) {
											iCnt++;
										}
									}
								}
							}
						}
						
						if (bOldVal == 0) 
						{
							if ((iCnt >= iBMin) && (iCnt <= iBMax)) 
																	
								bNewVal = 1; 
						} else 
						{
							if ((iCnt >= iSMin) && (iCnt <= iSMax)) 
																	
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
						bNewVal = bOldVal < (iClo - 1)
								? (short) (bOldVal + 1)
								: 0;
					}
				} else 
				{
					for (ic = 10 - iRng; ic <= 10 + iRng; ic++) {
						for (ir = 10 - iRng; ir <= 10 + iRng; ir++) {
							if ((isCentr) || (ic != i) || (ir != j)) {
								if ((fMoore)
										|| ((Math.abs(ic - 10) + Math
												.abs(ir - 10)) <= iRng)) {
									if (crrState[xVector[ic]][yVector[ir]] != 0) {
										iCnt++;
									}
								}
							}
						}
					}
					
					if (bOldVal == 0) 
					{
						if ((iCnt >= iBMin) && (iCnt <= iBMax)) 
																
							bNewVal = ColoringMethod == 1
									? 1
									: (short) (mjb.Cycle
											% (mjb.StatesCount - 1) + 1);
					} else 
					{
						if ((iCnt >= iSMin) && (iCnt <= iSMax)) 
																
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
				tmpState[i][j] = bNewVal;
				if (bNewVal != bOldVal) 
				{
					modCnt++; 
				}
			} 
		} 

		return modCnt;
	}
	
}
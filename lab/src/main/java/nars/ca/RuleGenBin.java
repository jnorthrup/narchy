package nars.ca;




import java.util.StringTokenizer;

public class RuleGenBin {
	public int iClo; 
	public boolean isHist; 
	public int iNgh; 
	public boolean[] rulesS = new boolean[256];
	public boolean[] rulesB = new boolean[256];

	
	public RuleGenBin() {
		ResetToDefaults();
	}

	
	
	public void ResetToDefaults() {
		iClo = 2; 
		isHist = false;
		iNgh = MJRules.NGHTYP_MOOR; 
		for (int i = 0; i <= 255; i++) {
			rulesS[i] = rulesB[i] = false;
		}
	}

	
	
	private static String ExpandIt(String sStr) {

        sStr = sStr.trim();
        int iNum = 0;
        String sRetString = "";
        for (int i = 0; i < sStr.length(); i++) {
            char cChar = sStr.charAt(i);
            if (Character.isDigit(cChar)) {
                int iCharVal = (int) cChar - (int) '0';
                iNum = iNum * 10 + iCharVal;
			} else {
				if (iNum == 0)
					iNum = 1;
				if (((int) sStr.charAt(i) == (int) 'a') || ((int) sStr.charAt(i) == (int) 'b')) {
					for (int j = 0; j < iNum; j++) {
                        sRetString += (((int) sStr.charAt(i) == (int) 'a') ? "0" : "1");
					}
					iNum = 0;
				}
			}
		}
		return sRetString;
	}

	
	
	
	public void InitFromString(String sStr) {
		int iTmp;

        ResetToDefaults();

        StringTokenizer st = new StringTokenizer(sStr, ",", true);
		while (st.hasMoreTokens()) {
            String sTok = st.nextToken();

            int i;
            if (sTok.length() > 0 && (int) sTok.charAt(0) == (int) 'S')
			{
				sTok = ExpandIt(sTok.substring(1));
				for (i = 0; (i < sTok.length()) && (i < 256); i++)
					rulesS[i] = ((int) sTok.charAt(i) == (int) '1');
			} else if (sTok.length() > 0 && (int) sTok.charAt(0) == (int) 'B')
			{
				sTok = ExpandIt(sTok.substring(1));
				for (i = 0; (i < sTok.length()) && (i < 256); i++)
					rulesB[i] = ((int) sTok.charAt(i) == (int) '1');
			} else if (sTok.length() > 0 && (int) sTok.charAt(0) == (int) 'C') {
				i = Integer.valueOf(sTok.substring(1));
				if (i >= 3) {
					isHist = true; 
					iClo = i;
				} else
					isHist = false; 
			} else if (sTok.startsWith("NM")) 
			{
				iNgh = MJRules.NGHTYP_MOOR;
			} else if (sTok.startsWith("NN")) 
			{
				iNgh = MJRules.NGHTYP_NEUM;
			}
		}
		Validate(); 
	}

	
	
	public void InitFromPrm(int i_Clo, boolean is_Hist, int i_Ngh,
			boolean[] rules_S, boolean[] rules_B) {
		iClo = i_Clo; 
		iNgh = i_Ngh; 
		isHist = is_Hist; 
		rulesS = rules_S;
		rulesB = rules_B;

		Validate(); 
	}

	
	private static String OneToken(int iVal, int iCnt) {

        String sRetStr = "";
		if (iCnt > 0) {
            String sChr = iVal == 0 ? "a" : "b";

            switch (iCnt) {
                case 1:
                    sRetStr = sChr;
                    break;
                case 2:
                    sRetStr = sChr + sChr;
                    break;
                default:
                    sRetStr = iCnt + sChr;
                    break;
            }
		}
		return sRetStr;
	}

	
	private static String CompactIt(String sStr) {
        String sResult = "";

        int iLast = -1;
        int iCnt = 0;
		for (int i = 0; i < sStr.length(); i++) {
            int iThis = Integer.valueOf(sStr.substring(i, i + 1));
            if ((iThis != 0) && (iThis != 1))
				iThis = 0;
			if (iThis != iLast) {
                sResult += OneToken(iLast, iCnt);
				iLast = iThis;
				iCnt = 1;
			} else
				iCnt++;
		}
		return sResult + OneToken(iLast, iCnt);
	}

	
	
	
	public String GetAsString() {


        Validate();


        int ih = isHist ? iClo : 0;
        String sBff = 'C' + String.valueOf(ih);


        int maxIdx;
        if (iNgh == MJRules.NGHTYP_NEUM)
		{
            sBff += ",NN";
			maxIdx = 15;
		} else 
		{
            sBff += ",NM";
			maxIdx = 255;
		}


        String sTmp = "";
        int i;
        for (i = 0; i < maxIdx; i++) {
            sTmp += (rulesS[i] ? '1' : '0');
		}
		sBff = sBff + ",S" + CompactIt(sTmp);

		
		sTmp = "";
		for (i = 0; i < maxIdx; i++) {
            sTmp += (rulesB[i] ? '1' : '0');
		}
		sBff = sBff + ",B" + CompactIt(sTmp);

		return sBff;
	}

	
	
	
	public void Validate() {
		if (iClo < 2)
			iClo = 2;
		else if (iClo > MJBoard.MAX_CLO)
			iClo = MJBoard.MAX_CLO;

		if ((iNgh != MJRules.NGHTYP_MOOR) && (iNgh != MJRules.NGHTYP_NEUM))
			iNgh = MJRules.NGHTYP_MOOR; 
	}

	
	
	public int OnePass(int sizX, int sizY, boolean isWrap, int ColoringMethod,
			short[][] crrState, short[][] tmpState, MJBoard mjb) {
        int modCnt = 0;
        int[] lurd = new int[4];

		for (int i = 0; i < sizX; ++i) {
			
			lurd[0] = (i > 0) ? i - 1 : (isWrap) ? sizX - 1 : sizX;
			lurd[2] = (i < sizX - 1) ? i + 1 : (isWrap) ? 0 : sizX;
			for (int j = 0; j < sizY; ++j) {
				
				lurd[1] = (j > 0) ? j - 1 : (isWrap) ? sizY - 1 : sizY;
				lurd[3] = (j < sizY - 1) ? j + 1 : (isWrap) ? 0 : sizY;
                short bOldVal = crrState[i][j];
                short bNewVal = bOldVal;

                int iCnt = 0;
                if (isHist)
				{
					if ((int) bOldVal <= 1)
					{
						if (iNgh == MJRules.NGHTYP_MOOR) {
							if ((int) crrState[i][lurd[1]] == 1)
								iCnt += 1;
							if ((int) crrState[lurd[2]][lurd[1]] == 1)
								iCnt += 2;
							if ((int) crrState[lurd[2]][j] == 1)
								iCnt += 4;
							if ((int) crrState[lurd[2]][lurd[3]] == 1)
								iCnt += 8;
							if ((int) crrState[i][lurd[3]] == 1)
								iCnt += 16;
							if ((int) crrState[lurd[0]][lurd[3]] == 1)
								iCnt += 32;
							if ((int) crrState[lurd[0]][j] == 1)
								iCnt += 64;
							if ((int) crrState[lurd[0]][lurd[1]] == 1)
								iCnt += 128;
						} else {
							if ((int) crrState[i][lurd[1]] == 1)
								iCnt += 1;
							if ((int) crrState[lurd[2]][j] == 1)
								iCnt += 2;
							if ((int) crrState[i][lurd[3]] == 1)
								iCnt += 4;
							if ((int) crrState[lurd[0]][j] == 1)
								iCnt += 8;
						}

						
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
						bNewVal = (int) bOldVal < (iClo - 1)
								? (short) ((int) bOldVal + 1)
								: (short) 0;
					}
				} else 
				{
					if (iNgh == MJRules.NGHTYP_MOOR) {
						if ((int) crrState[i][lurd[1]] != 0)
							iCnt += 1;
						if ((int) crrState[lurd[2]][lurd[1]] != 0)
							iCnt += 2;
						if ((int) crrState[lurd[2]][j] != 0)
							iCnt += 4;
						if ((int) crrState[lurd[2]][lurd[3]] != 0)
							iCnt += 8;
						if ((int) crrState[i][lurd[3]] != 0)
							iCnt += 16;
						if ((int) crrState[lurd[0]][lurd[3]] != 0)
							iCnt += 32;
						if ((int) crrState[lurd[0]][j] != 0)
							iCnt += 64;
						if ((int) crrState[lurd[0]][lurd[1]] != 0)
							iCnt += 128;
					} else {
						if ((int) crrState[i][lurd[1]] != 0)
							iCnt += 1;
						if ((int) crrState[lurd[2]][j] != 0)
							iCnt += 2;
						if ((int) crrState[i][lurd[3]] != 0)
							iCnt += 4;
						if ((int) crrState[lurd[0]][j] != 0)
							iCnt += 8;
					}

					
					if ((int) bOldVal == 0)
					{
						if (rulesB[iCnt]) 
							bNewVal = ColoringMethod == 1
									? (short) 1
									: (short) (mjb.Cycle
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

				tmpState[i][j] = bNewVal;
				if ((int) bNewVal != (int) bOldVal) {
					modCnt++; 
				}
			}
			
		}
		

		return modCnt;
	}
	
}
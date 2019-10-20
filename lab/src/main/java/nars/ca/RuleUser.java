package nars.ca;




import java.util.StringTokenizer;

public class RuleUser {
	public boolean isHist; 
	public int iClo; 

	private static final int RIDX_RUG = 1; 
	private static final int RIDX_DIB = 2; 
	private static final int RIDX_HOD = 3; 
	private static final int RIDX_GRH = 4; 

	private int RuleIdx;
	private int Increment; 

	
	public RuleUser() {
		ResetToDefaults();
	}

	
	
	public void ResetToDefaults() {
		isHist = true; 
		iClo = 16; 
		RuleIdx = RIDX_HOD; 
		Increment = 3; 
	}

	
	
	
	public void InitFromString(String sStr) {

        int i;
		ResetToDefaults();
		if (sStr.length() < 3)
			return;

		
		if (sStr.compareTo("Rug") == 0)
			sStr = "RUG,C64,I1";
		else if (sStr.compareTo("Digital_Inkblots") == 0)
			sStr = "DIB,C256,I3";
		else if (sStr.compareTo("Hodge") == 0)
			sStr = "HOD,C32,I5";
		else if (sStr.compareTo("GreenHast") == 0)
			sStr = "GRH";

		
		if (sStr.startsWith("RUG"))
			RuleIdx = RIDX_RUG;
		else if (sStr.startsWith("DIB"))
			RuleIdx = RIDX_DIB;
		else if (sStr.startsWith("HOD"))
			RuleIdx = RIDX_HOD;
		else if (sStr.startsWith("GRH"))
			RuleIdx = RIDX_GRH;

        StringTokenizer st = new StringTokenizer(sStr, " ,", true);
		while (st.hasMoreTokens()) {
            String sTok = st.nextToken().toUpperCase();
            if (sTok.length() > 0 && (int) sTok.charAt(0) == (int) 'I')
				Increment = Integer.valueOf(sTok.substring(1));
			else if (sTok.length() > 0 && (int) sTok.charAt(0) == (int) 'C')
				iClo = Integer.valueOf(sTok.substring(1));
		}

		Validate(); 
	}

	
	
	public String GetAsString() {


        Validate();

        String sBff = "";
        switch (RuleIdx) {
			case RIDX_RUG : 
				sBff = "RUG,C" + iClo;
				sBff = sBff + ",I" + Increment;
				break;
			case RIDX_DIB : 
				sBff = "DIB,C" + iClo;
				sBff = sBff + ",I" + Increment;
				break;
			case RIDX_HOD : 
				sBff = "HOD,C" + iClo;
				sBff = sBff + ",I" + Increment;
				break;
			case RIDX_GRH : 
				sBff = "GRH";
				break;
		}

		return sBff;
	}

	
	
	public void Validate() {
		if (iClo < 2)
			iClo = 2;
		else if (iClo > MJBoard.MAX_CLO)
			iClo = MJBoard.MAX_CLO;
	}

	
	
	public int OnePass(int sizX, int sizY, boolean isWrap, int ColoringMethod,
			short[][] crrState, short[][] tmpState) {
        int modCnt = 0;
        int[] lurd = new int[4];

		for (int i = 0; i < sizX; ++i) {
			
			lurd[0] = (i > 0) ? i - 1 : (isWrap) ? sizX - 1 : sizX;
			lurd[2] = (i < sizX - 1) ? i + 1 : (isWrap) ? 0 : sizX;
			for (int j = 0; j < sizY; ++j) {
				
				lurd[1] = j > 0 ? j - 1 : (isWrap) ? sizY - 1 : sizY;
				lurd[3] = (j < sizY - 1) ? j + 1 : (isWrap) ? 0 : sizY;
                short bOldVal = crrState[i][j];

                short bNewVal = bOldVal;
                int iCnt;
                switch (RuleIdx) {
					case RIDX_RUG : 
						
						iCnt = (int) crrState[lurd[0]][lurd[1]]
								+ (int) crrState[lurd[0]][j]
								+ (int) crrState[lurd[0]][lurd[3]]
								+ (int) crrState[i][lurd[1]] + (int) crrState[i][lurd[3]]
								+ (int) crrState[lurd[2]][lurd[1]]
								+ (int) crrState[lurd[2]][j]
								+ (int) crrState[lurd[2]][lurd[3]];
						bNewVal = (short) (((iCnt / 8) + Increment) % iClo); 
																				
																				
						break;

					case RIDX_DIB : 
						
						iCnt = (int) crrState[lurd[0]][lurd[1]]
								+ (int) crrState[lurd[0]][j]
								+ (int) crrState[lurd[0]][lurd[3]]
								+ (int) crrState[i][lurd[1]] + (int) crrState[i][j]
								+ (int) crrState[i][lurd[3]]
								+ (int) crrState[lurd[2]][lurd[1]]
								+ (int) crrState[lurd[2]][j]
								+ (int) crrState[lurd[2]][lurd[3]];
						bNewVal = (short) (((iCnt / 9) + Increment) % iClo); 
																				
																				
						break;

					case RIDX_HOD :
                        int sum8 = (int) crrState[lurd[0]][lurd[1]]
								+ (int) crrState[lurd[0]][j]
								+ (int) crrState[lurd[0]][lurd[3]]
								+ (int) crrState[i][lurd[1]] + (int) crrState[i][lurd[3]]
								+ (int) crrState[lurd[2]][lurd[1]]
								+ (int) crrState[lurd[2]][j]
								+ (int) crrState[lurd[2]][lurd[3]];
						bNewVal = (short) 0;

						
						if ((int) bOldVal == 0) {
							if (sum8 < Increment) {
								bNewVal = (short) 0;
							} else
								bNewVal = (short) (sum8 < 100 ? 2 : 3);
						} else if (((int) bOldVal > 0) && ((int) bOldVal < (iClo - 1))) {
							bNewVal = (short) (((sum8 >> 3) + Increment) & 255);
						}

						if ((int) bNewVal > (iClo - 1)) {
							bNewVal = (short) (iClo - 1);
						}

						if ((int) bOldVal == (iClo - 1)) {
							bNewVal = (short) 0;
						}
						break;

					case RIDX_GRH :

                        int prevState = ((int) bOldVal >> 2) & 3;

                        bOldVal = (short) ((int) bOldVal & 3);


                        int d = 0;
                        int r = 0;
                        switch (bOldVal) {
							case 0 :
                                int i4Sum = ((((int) crrState[lurd[0]][j] & 3) == 1)
                                        ? 1
                                        : 0)
                                        + ((((int) crrState[i][lurd[1]] & 3) == 1)
                                        ? 1
                                        : 0)
                                        + ((((int) crrState[i][lurd[3]] & 3) == 1)
                                        ? 1
                                        : 0)
                                        + ((((int) crrState[lurd[2]][j] & 3) == 1)
                                        ? 1
                                        : 0);
								r = 0;
								d = (i4Sum > 0) ? 1 : 0;
								break;
							case 1 : 
								r = 2;
								d = 0;
								break;
							case 2 : 
								r = 0;
								d = 0;
								break;
						}
						bNewVal = (short) ((r + d - prevState + 3) % 3 + ((int) bOldVal << 2));
																							
																							
																							
																							
						break;
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
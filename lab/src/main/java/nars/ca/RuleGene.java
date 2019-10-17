package nars.ca;




import java.util.StringTokenizer;

public class RuleGene {
	public int iClo; 
	private final boolean[] RulesS = new boolean[9]; 
	private final boolean[] RulesB = new boolean[9]; 

	
	public RuleGene() {
		ResetToDefaults();
	}

	
	
	public void ResetToDefaults() {
		iClo = 3; 
		for (int i = 0; i <= 8; i++) {
			RulesS[i] = false;
			RulesB[i] = false;
		}
	}

	
	
	
	@SuppressWarnings("HardcodedFileSeparator")
	public void InitFromString(String sStr) {

        String sTok;
		int i, iNum = 1;
		char cChar;
		int iCharVal;
		ResetToDefaults();

        StringTokenizer st = new StringTokenizer(sStr, ",/", true);
		while (st.hasMoreTokens()) {
			sTok = st.nextToken();
			if ((sTok.compareTo("/") == 0) || (sTok.compareTo(",") == 0)) {
				iNum++;
				continue;
			}
			switch (iNum) {
			case 1: 
			case 2: 
				for (i = 0; i < sTok.length(); i++) {
					cChar = sTok.charAt(i);
					if (Character.isDigit(cChar)) {
						iCharVal = cChar - '0';
						if ((iCharVal >= 0) && (iCharVal <= 8)) {
							if (iNum == 1)
								RulesS[iCharVal] = true;
							else
								RulesB[iCharVal] = true;
						}
					}
				}
				break;
			case 3: 
				iClo = Integer.valueOf(sTok);
				break;
			}
		}

		Validate(); 
	}

	
	
	public void InitFromPrm(int i_Clo, boolean[] rulS, boolean[] rulB) {
		iClo = i_Clo;
		for (int i = 0; i <= 8; i++) {
			RulesS[i] = rulS[i];
			RulesB[i] = rulB[i];
		}
		Validate(); 
	}

	
	
	
	@SuppressWarnings("HardcodedFileSeparator")
	public String GetAsString() {
		String sBff = "";
		int i;

		
		Validate();

		
		for (i = 0; i <= 8; i++)
			
			if (RulesS[i])
				sBff = sBff + i;
		sBff = sBff + '/';

		for (i = 0; i <= 8; i++)
			
			if (RulesB[i])
				sBff = sBff + i;
		sBff = sBff + '/';

		sBff = sBff + iClo;
		return sBff;
	}

	
	
	
	public void Validate() {
		if (iClo < 2)
			iClo = 2;
		else if (iClo > MJBoard.MAX_CLO)
			iClo = MJBoard.MAX_CLO;
	}

	
	
	public int OnePass(int sizX, int sizY, boolean isWrap, int ColoringMethod,
					   short[][] crrState, short[][] tmpState, MJBoard mjb) {
		short bOldVal, bNewVal;
		int modCnt = 0;
		int i, j, iCnt;
		int[] lurd = new int[4]; 

		for (i = 0; i < sizX; ++i) {
			
			lurd[0] = (i > 0) ? i - 1 : (isWrap) ? sizX - 1 : sizX;
			lurd[2] = (i < sizX - 1) ? i + 1 : (isWrap) ? 0 : sizX;
			for (j = 0; j < sizY; ++j) {
				
				lurd[1] = (j > 0) ? j - 1 : (isWrap) ? sizY - 1 : sizY;
				lurd[3] = (j < sizY - 1) ? j + 1 : (isWrap) ? 0 : sizY;
				bOldVal = crrState[i][j];
				bNewVal = bOldVal;
				if (bOldVal > 1) 
				{
					bNewVal = bOldVal < iClo - 1 ? (short) (bOldVal + 1) : 0;
				} else 
				{
					iCnt = 0;
					if (crrState[lurd[0]][lurd[1]] == 1)
						++iCnt;
					if (crrState[i][lurd[1]] == 1)
						++iCnt;
					if (crrState[lurd[2]][lurd[1]] == 1)
						++iCnt;
					if (crrState[lurd[0]][j] == 1)
						++iCnt;
					if (crrState[lurd[2]][j] == 1)
						++iCnt;
					if (crrState[lurd[0]][lurd[3]] == 1)
						++iCnt;
					if (crrState[i][lurd[3]] == 1)
						++iCnt;
					if (crrState[lurd[2]][lurd[3]] == 1)
						++iCnt;
					if (bOldVal != 0) {
						if (RulesS[iCnt])
							bNewVal = 1;
						else if (bOldVal < iClo - 1)
							bNewVal = (short) (bOldVal + 1);
						else
							bNewVal = 0;
					} else 
					{
						if (RulesB[iCnt])
							bNewVal = 1;
					}
				}
				tmpState[i][j] = bNewVal;
				if (bNewVal != bOldVal) {
					modCnt++; 
				}
			} 
		} 
		return modCnt;
	}
	
}
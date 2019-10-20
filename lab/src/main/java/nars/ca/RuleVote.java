package nars.ca;




import java.util.StringTokenizer;

public class RuleVote {
	private final boolean[] RulesSB = new boolean[10]; 

	
	public RuleVote() {
		ResetToDefaults();
	}

	
	
	public void ResetToDefaults() {
		for (int i = 0; i <= 9; i++) {
			RulesSB[i] = false;
		}
	}

	
	
	
	@SuppressWarnings("HardcodedFileSeparator")
	public void InitFromString(String sStr) {

        ResetToDefaults();


        StringTokenizer st = new StringTokenizer(sStr, ",/", true);
        int iNum = 1;
        while (st.hasMoreTokens()) {
            String sTok = st.nextToken();
            if ((sTok.compareTo("/") == 0) || (sTok.compareTo(",") == 0)) {
				iNum++;
				continue;
			}

			for (int i = 0; i < sTok.length(); i++) {
                char cChar = sTok.charAt(i);
                if (Character.isDigit(cChar)) {
                    int iCharVal = (int) cChar - (int) '0';
                    if ((iCharVal >= 0) && (iCharVal <= 9)) {
						RulesSB[iCharVal] = true;
					}
				}
			}
		}

		Validate(); 
	}

	
	
	public void InitFromPrm(boolean[] rulSB) {
        System.arraycopy(rulSB, 0, RulesSB, 0, 10);
		Validate(); 
	}

	
	
	
	public String GetAsString() {


        Validate();


        String sBff = "";
        for (int i = 0; i <= 9; i++)
			
			if (RulesSB[i])
                sBff += i;

		return sBff;
	}

	
	
	
	public void Validate() {
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
                int iCnt = 0;
                if ((int) crrState[lurd[0]][lurd[1]] != 0)
					++iCnt;
				if ((int) crrState[i][lurd[1]] != 0)
					++iCnt;
				if ((int) crrState[lurd[2]][lurd[1]] != 0)
					++iCnt;
				if ((int) crrState[lurd[0]][j] != 0)
					++iCnt;
				if ((int) crrState[i][j] != 0)
					++iCnt;
				if ((int) crrState[lurd[2]][j] != 0)
					++iCnt;
				if ((int) crrState[lurd[0]][lurd[3]] != 0)
					++iCnt;
				if ((int) crrState[i][lurd[3]] != 0)
					++iCnt;
				if ((int) crrState[lurd[2]][lurd[3]] != 0)
					++iCnt;


                short bNewVal = bOldVal;
                if ((int) bOldVal == 0)
				{
					if (RulesSB[iCnt]) 
						bNewVal = ColoringMethod == 1 ? (short) 1 : (short) (mjb.Cycle
								% (mjb.StatesCount - 1) + 1);
				} else 
				{
					if (RulesSB[iCnt]) 
					{
						if (ColoringMethod == 1) 
						{
							bNewVal = (short) ((int) bOldVal < mjb.StatesCount - 1 ? (int) bOldVal + 1 : mjb.StatesCount - 1);
						} else {
							
						}
					} else
						bNewVal = (short) 0;
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
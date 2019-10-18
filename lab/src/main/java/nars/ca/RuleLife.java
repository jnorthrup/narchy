package nars.ca;




import java.util.StringTokenizer;

public class RuleLife {
	private final boolean[] RulesS = new boolean[9]; 
	private final boolean[] RulesB = new boolean[9]; 

	
	public RuleLife() {
		ResetToDefaults();
	}

	
	
	public void ResetToDefaults() {
		for (int i = 0; i <= 8; i++) {
			RulesS[i] = false;
			RulesB[i] = false;
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
                    int iCharVal = cChar - '0';
                    if ((iCharVal >= 0) && (iCharVal <= 8)) {
						
						if ((sTok.charAt(0) == 'S') || (sTok.charAt(0) == 's'))
							RulesS[iCharVal] = true;
						else if ((sTok.charAt(0) == 'B')
								|| (sTok.charAt(0) == 'b'))
							RulesB[iCharVal] = true;
						else if (iNum == 1)
							RulesS[iCharVal] = true;
						else
							RulesB[iCharVal] = true;
					}
				}
			}
		}

		Validate(); 
	}

	
	
	public void InitFromPrm(boolean[] rulS, boolean[] rulB) {
		for (int i = 0; i <= 8; i++) {
			RulesS[i] = rulS[i];
			RulesB[i] = rulB[i];
		}
		Validate(); 
	}

	
	
	
	@SuppressWarnings("HardcodedFileSeparator")
	public String GetAsString() {


        Validate();


        int i;
        String sBff = "";
        for (i = 0; i <= 8; i++)
			
			if (RulesS[i])
                sBff += i;
        sBff += '/';

		for (i = 0; i <= 8; i++)
			
			if (RulesB[i])
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
                if (crrState[lurd[0]][lurd[1]] != 0)
					++iCnt;
				if (crrState[i][lurd[1]] != 0)
					++iCnt;
				if (crrState[lurd[2]][lurd[1]] != 0)
					++iCnt;
				if (crrState[lurd[0]][j] != 0)
					++iCnt;
				if (crrState[lurd[2]][j] != 0)
					++iCnt;
				if (crrState[lurd[0]][lurd[3]] != 0)
					++iCnt;
				if (crrState[i][lurd[3]] != 0)
					++iCnt;
				if (crrState[lurd[2]][lurd[3]] != 0)
					++iCnt;


                short bNewVal = bOldVal;
                if (bOldVal == 0)
				{
					if (RulesB[iCnt]) 
						bNewVal = ColoringMethod == 1 ? 1 : (short) (mjb.Cycle
								% (mjb.StatesCount - 1) + 1);
				} else 
				{
					if (RulesS[iCnt]) 
					{
						if (ColoringMethod == 1) 
						{
							bNewVal = (short) (bOldVal < mjb.StatesCount - 1 ? bOldVal + 1 : mjb.StatesCount - 1);
						} else {
							
						}
					} else
						bNewVal = 0; 
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
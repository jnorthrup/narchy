package nars.ca;




import java.util.StringTokenizer;

public class RuleVote {
	private final boolean[] RulesSB = new boolean[10]; 

	
	public RuleVote() {
		ResetToDefaults();
	}

	
	
	public void ResetToDefaults() {
		for (var i = 0; i <= 9; i++) {
			RulesSB[i] = false;
		}
	}

	
	
	
	@SuppressWarnings("HardcodedFileSeparator")
	public void InitFromString(String sStr) {

        ResetToDefaults();


		var st = new StringTokenizer(sStr, ",/", true);
		var iNum = 1;
        while (st.hasMoreTokens()) {
			var sTok = st.nextToken();
            if ((sTok.compareTo("/") == 0) || (sTok.compareTo(",") == 0)) {
				iNum++;
				continue;
			}

			for (var i = 0; i < sTok.length(); i++) {
				var cChar = sTok.charAt(i);
                if (Character.isDigit(cChar)) {
					var iCharVal = cChar - '0';
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


		var sBff = "";
        for (var i = 0; i <= 9; i++)
			
			if (RulesSB[i])
                sBff += i;

		return sBff;
	}

	
	
	
	public void Validate() {
    }

	
	
	public int OnePass(int sizX, int sizY, boolean isWrap, int ColoringMethod,
					   short[][] crrState, short[][] tmpState, MJBoard mjb) {
		var modCnt = 0;
		var lurd = new int[4];

		for (var i = 0; i < sizX; ++i) {
			
			lurd[0] = (i > 0) ? i - 1 : (isWrap) ? sizX - 1 : sizX;
			lurd[2] = (i < sizX - 1) ? i + 1 : (isWrap) ? 0 : sizX;
			for (var j = 0; j < sizY; ++j) {
				
				lurd[1] = (j > 0) ? j - 1 : (isWrap) ? sizY - 1 : sizY;
				lurd[3] = (j < sizY - 1) ? j + 1 : (isWrap) ? 0 : sizY;
				var bOldVal = crrState[i][j];
				var iCnt = 0;
                if (crrState[lurd[0]][lurd[1]] != 0)
					++iCnt;
				if (crrState[i][lurd[1]] != 0)
					++iCnt;
				if (crrState[lurd[2]][lurd[1]] != 0)
					++iCnt;
				if (crrState[lurd[0]][j] != 0)
					++iCnt;
				if (crrState[i][j] != 0)
					++iCnt;
				if (crrState[lurd[2]][j] != 0)
					++iCnt;
				if (crrState[lurd[0]][lurd[3]] != 0)
					++iCnt;
				if (crrState[i][lurd[3]] != 0)
					++iCnt;
				if (crrState[lurd[2]][lurd[3]] != 0)
					++iCnt;


				var bNewVal = bOldVal;
                if (bOldVal == 0)
				{
					if (RulesSB[iCnt]) 
						bNewVal = ColoringMethod == 1 ? 1 : (short) (mjb.Cycle
								% (mjb.StatesCount - 1) + 1);
				} else 
				{
					if (RulesSB[iCnt]) 
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
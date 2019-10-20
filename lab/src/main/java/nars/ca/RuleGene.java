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
		for (var i = 0; i <= 8; i++) {
			RulesS[i] = false;
			RulesB[i] = false;
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
			switch (iNum) {
			case 1: 
			case 2: 
				for (var i = 0; i < sTok.length(); i++) {
					var cChar = sTok.charAt(i);
                    if (Character.isDigit(cChar)) {
						var iCharVal = cChar - '0';
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
		for (var i = 0; i <= 8; i++) {
			RulesS[i] = rulS[i];
			RulesB[i] = rulB[i];
		}
		Validate(); 
	}

	
	
	
	@SuppressWarnings("HardcodedFileSeparator")
	public String GetAsString() {


        Validate();


        int i;
		var sBff = "";
        for (i = 0; i <= 8; i++)
			
			if (RulesS[i])
                sBff += i;
        sBff += '/';

		for (i = 0; i <= 8; i++)
			
			if (RulesB[i])
                sBff += i;
        sBff += '/';

        sBff += iClo;
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
		var modCnt = 0;
		var lurd = new int[4];

		for (var i = 0; i < sizX; ++i) {
			
			lurd[0] = (i > 0) ? i - 1 : (isWrap) ? sizX - 1 : sizX;
			lurd[2] = (i < sizX - 1) ? i + 1 : (isWrap) ? 0 : sizX;
			for (var j = 0; j < sizY; ++j) {
				
				lurd[1] = (j > 0) ? j - 1 : (isWrap) ? sizY - 1 : sizY;
				lurd[3] = (j < sizY - 1) ? j + 1 : (isWrap) ? 0 : sizY;
				var bOldVal = crrState[i][j];
				var bNewVal = bOldVal;
                if (bOldVal > 1)
				{
					bNewVal = bOldVal < iClo - 1 ? (short) (bOldVal + 1) : 0;
				} else 
				{
					var iCnt = 0;
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
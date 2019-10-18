package nars.ca;




import java.util.StringTokenizer;

public class Rule1DBin {
	public static final int MAX_RANGE = 4;

	public final byte[] iAry = new byte[512]; 
	public String sHex; 
	public int iRng; 

	
	public Rule1DBin() {
		ResetToDefaults();
		SetArray(); 
	}

	
	
	public void ResetToDefaults() {
		iRng = 1;
		sHex = "6E";
	}

	
	
	
	public void InitFromString(String sStr) {

        String sTok;
		int iTmp;
		ResetToDefaults();

        StringTokenizer st = new StringTokenizer(sStr, ",", true);
		while (st.hasMoreTokens()) {
			sTok = st.nextToken().toUpperCase();
			
			if (sTok.length() > 0 && sTok.charAt(0) == 'R') 
			{
				iRng = Integer.valueOf(sTok.substring(1));
			} else if (sTok.length() > 0 && sTok.charAt(0) == 'W') 
																	
			{
				sHex = sTok.substring(1);
			}
		}
		
		SetArray(); 
	}

	
	
	public void InitFromPrm(int i_Rng, String sBinStr) {
		iRng = i_Rng;
		sHex = CvtBinStr2HexStr(sBinStr);
		SetArray(); 
	}

	
	
	
	public String GetAsString() {
		String sBff;
		int i, ih;

		
		Validate();
		
		sBff = 'R' + String.valueOf(iRng);

		
		sBff = sBff + ",R" + sHex;

		return sBff;
	}

	
	
	
	public void Validate() {
		if (iRng < 1)
			iRng = 1;
		else if (iRng > MAX_RANGE)
			iRng = MAX_RANGE;

		sHex.toUpperCase();
		if ((!sHex.isEmpty()) && (sHex.charAt(0) == 'W'))
			sHex = sHex.substring(1); 
	}

	
	
	private void SetArray() {
        int i;

        Validate();

        String sBinStr = CvtHexStr2BinStr(sHex);
        int iCnt = 1;

		
		for (i = 1; i <= 2 * iRng + 1; i++)
            iCnt *= 2;
		sBinStr = LPad(sBinStr, iCnt, '0');

		
		for (i = 0; i < iCnt; i++)
			iAry[iCnt - i - 1] = (byte) (sBinStr.charAt(i) == '1' ? 1 : 0);
	}

	
	
	private String LPad(String sStr, int num, char chPad) {
		int i;
        int iLen = sStr.length();
		if (iLen < num) {
			for (i = 1; i <= num - iLen; i++)
				sStr = chPad + sStr;
		}
		return sStr;
	}

	
	
	private String CvtBinStr2HexStr(String sBin) {
		int iVal;
		String sTok;

        int i = sBin.length();
		if ((i % 4) != 0)
			LPad(sBin, 4 - (i % 4), '0');

        String sHexStr = "";
		for (i = 1; i <= (sBin.length() / 4); i++) {
			sTok = sBin.substring(sBin.length() - i * 4, sBin.length() - i * 4
					+ 3);
			iVal = 0;
			if (sTok.charAt(1) == '1')
				iVal += 8;
			if (sTok.charAt(2) == '1')
				iVal += 4;
			if (sTok.charAt(3) == '1')
				iVal += 2;
			if (sTok.charAt(4) == '1')
				iVal += 1;
			sHexStr = Integer.toHexString(iVal) + sHexStr;
		}
		sHexStr = DelLedChr(sHexStr, '0');

		return sHexStr;
	}

	
	
	private String CvtHexStr2BinStr(String sHex) {
        int i;

        String sBinBff = "";
		sHex.toUpperCase();
		for (i = 0; i < sHex.length(); i++) {
			switch (sHex.charAt(i)) {
				case '0' :
                    sBinBff += "0000";
					break;
				case '1' :
                    sBinBff += "0001";
					break;
				case '2' :
                    sBinBff += "0010";
					break;
				case '3' :
                    sBinBff += "0011";
					break;
				case '4' :
                    sBinBff += "0100";
					break;
				case '5' :
                    sBinBff += "0101";
					break;
				case '6' :
                    sBinBff += "0110";
					break;
				case '7' :
                    sBinBff += "0111";
					break;
				case '8' :
                    sBinBff += "1000";
					break;
				case '9' :
                    sBinBff += "1001";
					break;
				case 'A' :
                    sBinBff += "1010";
					break;
				case 'B' :
                    sBinBff += "1011";
					break;
				case 'C' :
                    sBinBff += "1100";
					break;
				case 'D' :
                    sBinBff += "1101";
					break;
				case 'E' :
                    sBinBff += "1110";
					break;
				case 'F' :
                    sBinBff += "1111";
					break;
			}
		}
		sBinBff = DelLedChr(sBinBff, '0');
		return sBinBff;
	}

	
	
	private String DelLedChr(String sStr, char cChar) {
		while ((!sStr.isEmpty()) && (sStr.charAt(0) == cChar))
			sStr = sStr.substring(1);

		return sStr;
	}

	
	
	public int OnePass(int sizX, int sizY, boolean isWrap, int ColoringMethod,
			short[][] crrState, short[][] tmpState, MJBoard mjb) {
		short bOldVal, bNewVal;
        int i;
        int ic, iPow, iIdx;
		int iClo = mjb.StatesCount;

        int ary1DOfs = iRng;
        short[] OneRow = new short[sizX + 1 + 2 * ary1DOfs];
        int[] xVector = new int[21];

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

			iPow = 1;
			iIdx = 0;
			for (i = iRng; i >= -iRng; i--) 
			{
				if (OneRow[ic + i + ary1DOfs] > 0)
                    iIdx += iPow;
                iPow *= 2;
			}

			
			bNewVal = iAry[iIdx]; 
			if (bNewVal > 0)
				if (ColoringMethod == 2) 
					bNewVal = (short) (mjb.Cycle % (iClo - 1) + 1); 

			tmpState[ic][i1DNextRow] = bNewVal;
		}

        int modCnt = 1;
		mjb.i1DLastRow = i1DNextRow; 

		return modCnt;
	}
}

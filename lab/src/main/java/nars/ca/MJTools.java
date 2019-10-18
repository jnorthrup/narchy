package nars.ca;




import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

public class MJTools {
	
	public MJTools() {
	}

	
	
	public static boolean LoadTextFile(String sPath, Vector vLines) {
        URL theUrl;
        try {
			theUrl = new URL(sPath);
		} catch (MalformedURLException mue) {
			System.out.println("Malformed URL: " + sPath);
			return false;
		} catch (SecurityException se) {
			System.out.println("Security exception: " + sPath);
			return false;
		}

        boolean fRetVal = false;
        try {
			vLines.removeAllElements();

            DataInputStream theFile = new DataInputStream(theUrl.openStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(
					theFile));


            String sBff;
            while ((sBff = br.readLine()) != null) {
				if (!sBff.isEmpty()) {
					vLines.addElement(sBff.trim());
				}
			}
			br.close();
			fRetVal = true;
		} catch (IOException e) {
			System.out.println("IOException:" + e);
		}

		return fRetVal;
	}

	
	
	
	public static boolean LoadResTextFile(String sPath, Vector vLines) {
		boolean fRetVal = false;

        try {
			InputStream in = MJCell.class.getResourceAsStream(sPath);
			if (in != null) {
				BufferedReader br = new BufferedReader(
						new InputStreamReader(in));


                String sBff;
                while ((sBff = br.readLine()) != null) {
					if (!sBff.isEmpty()) {
						vLines.addElement(sBff.trim());
					}
				}
				br.close();
				fRetVal = true;
			}
		} catch (IOException e) {
			System.out.println("IOException:" + e);
		}

		return fRetVal;
	}
	
	
	
}
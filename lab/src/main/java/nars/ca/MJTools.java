package nars.ca;




import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

public class MJTools {
	
	public MJTools() {
	}

	
	
	public static boolean LoadTextFile(String sPath, Vector vLines) {
		boolean fRetVal = false;
		URL theUrl;
		DataInputStream theFile;
		String sBff;
		try {
			theUrl = new URL(sPath);
		} catch (MalformedURLException mue) {
			System.out.println("Malformed URL: " + sPath);
			return false;
		} catch (SecurityException se) {
			System.out.println("Security exception: " + sPath);
			return false;
		}

		try {
			vLines.removeAllElements();

			theFile = new DataInputStream(theUrl.openStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(
					theFile));

			
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
		String sBff;

		try {
			InputStream in = MJCell.class.getResourceAsStream(sPath);
			if (in != null) {
				BufferedReader br = new BufferedReader(
						new InputStreamReader(in));

				
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
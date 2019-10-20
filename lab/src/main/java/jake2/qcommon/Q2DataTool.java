/*
 * Q2DataDialog.java
 * Copyright (C)  2003
 */
package jake2.qcommon;

import jake2.Jake2;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Q2DataTool {
    static final String home = System.getProperty("user.home");
    static final String sep = System.getProperty("file.separator");
    static final String dataDir = home + sep + "Jake2";
    static final String baseq2Dir = dataDir + sep + "baseq2";

    private final Vector<String> mirrorNames = new Vector<>();
    private final Vector<String> mirrorLinks = new Vector<>();
    private final byte[] buf = new byte[64*1024];

    public void testQ2Data() {
        initMirrors();
        for(var i = 0; !isAvail() && i<mirrorNames.size(); i++) {
            try {
                install(i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void dispose() {
    }
    
    static void setStatus(String text) {
        System.err.println(text);
        System.err.println();
    }

    static boolean isAvail() {
        Cvar.Set("cddir", baseq2Dir);
        FS.setCDDir();
        return null != FS.LoadFile("pics/colormap.pcx");        
    }

    void initMirrors() {
        var in = Jake2.class.getResourceAsStream("mirrors");
        var r = new BufferedReader(new InputStreamReader(in));
        try {
            while (true) {
                var name = r.readLine();
                var value = r.readLine();
                if (name == null || value == null) break;
                mirrorNames.add(name);
                mirrorLinks.add(value);
            }
        } catch (Exception e) {} 
        finally {
            try {
                r.close();
            } catch (Exception e1) {}
            try {
                in.close();
            } catch (Exception e1) {}
        }
    }

    void install(int mirrorIdx) {
        var mirrorName = mirrorNames.get(mirrorIdx);
        var mirror = mirrorLinks.get(mirrorIdx);

        setStatus("downloading from "+mirrorName+": <"+mirror+ '>');

        File dir = null;
        try {
            dir = new File(dataDir);
            dir.mkdirs();
        }
        catch (Exception e) {}
        try {
            if (!dir.isDirectory() || !dir.canWrite()) {
                setStatus("can't write to " + dataDir);
                return;
            } 
        }
        catch (Exception e) {
            setStatus(e.getMessage());
            return;
        }

        File outFile = null;
        OutputStream out = null;
        InputStream in = null;
        try {
            var url = new URL(mirror);
            var conn = url.openConnection();
            

            in = conn.getInputStream();

            outFile = File.createTempFile("Jake2Data", ".zip");
            outFile.deleteOnExit();
            out = new FileOutputStream(outFile);

            copyStream(in, out);
        } catch (Exception e) {
            setStatus(e.getMessage());
            return;
        } finally {
            try {
                in.close();
            } catch (Exception e) {}
            try {
                out.close();
            } catch (Exception e) {}                
        }

        try {
            installData(outFile.getCanonicalPath());
        } catch (Exception e) {
            setStatus(e.getMessage());
            return;
        }


        try {
            if (outFile != null) outFile.delete();
        } catch (Exception e) {}

        setStatus("installation successful from "+mirrorName+": <"+mirror+ '>');
    }


    void installData(String filename) throws Exception {
        InputStream in = null;
        OutputStream out = null;
        try {
            var f = new ZipFile(filename);
            var e = f.entries();
            while (e.hasMoreElements()) {
                var entry = e.nextElement();
                var name = entry.getName();
                int i;
                if ((i = name.indexOf("/baseq2")) > -1 && !name.contains(".dll")) {
                    name = dataDir + name.substring(i);
                    var outFile = new File(name);
                    if (entry.isDirectory()) {
                        outFile.mkdirs();
                    } else {
                        setStatus("installing " + outFile.getName());
                        outFile.getParentFile().mkdirs();
                        out = new FileOutputStream(outFile);
                        in = f.getInputStream(entry);
                        copyStream(in, out);
                    }
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            try {in.close();} catch (Exception e1) {}
            try {out.close();} catch (Exception e1) {}                              
        }
    }

    void copyStream(InputStream in, OutputStream out) throws Exception {
        try {
            
            int l;
            while ((l = in.read(buf)) > 0) {
                out.write(buf, 0, l);
                
            }
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                in.close();
            } catch (Exception e) {}
            try {
                out.close();
            } catch (Exception e) {}
        }                       
    }

}

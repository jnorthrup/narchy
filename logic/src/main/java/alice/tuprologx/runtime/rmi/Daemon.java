package alice.tuprologx.runtime.rmi;

import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.registry.LocateRegistry;
import java.util.Arrays;

public class Daemon {

    public static void main(String... args){
        String engineName="prolog";
        int port=1099;
        String portString="1099";
        if (args.length >0){
            if ("-?".equals(args[0])){
                System.out.println("\nargs: {-N<engine name>} {-P<rmi server port>} \nex: -Nprolog -P1099\n");
                System.exit(-1);
            }
            String name=getOpt(args,"-N");
            if (name!=null)
                engineName=name;
            String portSt=getOpt(args,"-P");
            if (portSt!=null){
                try {
                    port=Integer.parseInt(portSt);
                    portString=portSt;
                } catch (Exception ex){
                    System.err.println("Invalid port specification - "+portSt+" - setting default.");
                }
            }
        }
        System.setSecurityManager(new RMISecurityManager());
        try {
            LocateRegistry.createRegistry(port);
        } catch (Exception ex){
        }
        try {
            PrologImpl engine=new PrologImpl();

            String hostName=InetAddress.getLocalHost().toString();
            int index=hostName.indexOf('/');
            if (index>=0)
                hostName=hostName.substring(0,index);
            Naming.rebind("//"+hostName+ ':' +portString+ '/' +engineName,engine);
            System.out.println("RMI server at "+portString+": "+engineName+" engine ready.");
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    static String getOpt(String[] args,String prefix){
        return Arrays.stream(args).filter(arg -> arg.startsWith(prefix)).findFirst().map(arg -> arg.substring(prefix.length())).orElse(null);
    }
}


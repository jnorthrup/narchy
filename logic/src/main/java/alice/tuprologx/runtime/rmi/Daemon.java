package alice.tuprologx.runtime.rmi;

import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.registry.LocateRegistry;
import java.util.Optional;

public class Daemon {

    public static void main(String... args){
        var engineName="prolog";
        var port=1099;
        var portString="1099";
        if (args.length >0){
            if ("-?".equals(args[0])){
                System.out.println("\nargs: {-N<engine name>} {-P<rmi server port>} \nex: -Nprolog -P1099\n");
                System.exit(-1);
            }
            var name=getOpt(args,"-N");
            if (name!=null)
                engineName=name;
            var portSt=getOpt(args,"-P");
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
            var engine=new PrologImpl();

            var hostName=InetAddress.getLocalHost().toString();
            var index=hostName.indexOf('/');
            if (index>=0)
                hostName=hostName.substring(0,index);
            Naming.rebind("//"+hostName+ ':' +portString+ '/' +engineName,engine);
            System.out.println("RMI server at "+portString+": "+engineName+" engine ready.");
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    static String getOpt(String[] args,String prefix){
        for (var s : args) {
            if (s.startsWith(prefix)) {
                return Optional.of(s).map(arg -> arg.substring(prefix.length())).orElse(null);
            }
        }
        return Optional.<String>empty().map(arg -> arg.substring(prefix.length())).orElse(null);
    }
}


import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;

public class Server{
    static String aFile;
    private static int PORT_NUMBER = 9999;

    public static void main(String[] args) throws Exception {
        if(args.length == 1){
            File f = new File(args[0]);
            if(f.exists()){
                aFile = args[0];
                System.out.println("Authorisation file found at input path.");
            }
            else{
                aFile = null;
                System.out.println("No authorisation file found.");
                return;
            }
        }
        else{
            System.out.println("Error. 1 argument required for authorisation file.");
            return;
        }

        ServerSocket wSocket = new ServerSocket(PORT_NUMBER);
        System.out.println("Welcome socket open");

        while(true){
            Socket s = wSocket.accept();
            new ServerThreadInstance(s, aFile).start();
        }


    }
}
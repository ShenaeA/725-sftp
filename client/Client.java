
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class Client {
    static ArrayList<String> validCommands = new ArrayList<String>();
    static String[] sftpCommands;
    static String HOSTNAME = "localhost";
    static int PORT_NUMBER = 9999;
    static Socket socket;
    static boolean active = true;

    static DataOutputStream aOutToServer;
    static BufferedReader aInFromServer;
    static DataOutputStream bInToServer;
    static DataInputStream bInFromServer;

    public static void main(String[] args) throws Exception {
        new File(System.getProperty("user.dir")+"\\ft").mkdir(); // creats file transfer (ft) folder if doesn't already exist

        sftpCommands = new String[]{"USER","ACCT","PASS","TYPE","LIST","CDIR","KILL","NAME","TOBE","DONE","RETR","SEND","STOP","STOR"};
        
        try{
            socket = new Socket(HOSTNAME, PORT_NUMBER);
            aOutToServer = new DataOutputStream(socket.getOutputStream());
            aInFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bInToServer = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            bInFromServer = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            System.out.println("Connected to " + HOSTNAME + " via port number " + PORT_NUMBER);
            System.out.println(readFromServer()); // receiving greeting

            while(active){
                System.out.print("> ");
                String[] command = getClientCommand();
                if(command != null){
                    executeClientCommand();
                }
            }
        }
        catch {

        }
    }

    // TO DO: readFromServer

    // TO DO: getClientCommand

    // TO DO: executeClientCommand
}

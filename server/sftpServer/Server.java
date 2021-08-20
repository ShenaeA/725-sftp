package sftpServer;

import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;

public class Server{
    static String aFile;
    private static int PORT_NUMBER = 9999;
    static boolean folderExists;
    static File ftFolder;
    static boolean seeSysOutput = true;
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

        ftFolder = new File(System.getProperty("user.dir")+"\\sft");

        if(ftFolder.exists()){
            System.out.println("File transfer folder already exists");
        }
        else { // create file transfer folder
            try{
                ftFolder.mkdir();
                System.out.println("Created file transfer folder");
            }
            catch(Exception e){}
        }

        ServerSocket wSocket = new ServerSocket(PORT_NUMBER);
        System.out.println("Welcome socket open");

        while(true){
            Socket s = wSocket.accept();
            new ServerThreadInstance(s, aFile).start();
        }
    }
}
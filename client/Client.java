
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;


public class Client {
    static String[] validCommands;
    private static File clientFolderFile = new File(System.getProperty("user.dir"));
	private static final String rootDir = (clientFolderFile.getParentFile()).getAbsolutePath();
	String activeDir = rootDir;
    static String HOSTNAME = "localhost";
    static int PORT_NUMBER = 9999;
    static Socket socket;
    static boolean active = true;
    static String activeCommand;
    static String sendType = "b";
    static boolean retrFlag = false;
    static String retrFileName = "";
    static int retrFileSize;

    static DataOutputStream aOutToServer;
    static BufferedReader aInFromServer;
    static DataOutputStream bOutToServer;
    static DataInputStream bInFromServer;

    public static void main(String[] args) throws Exception {
        validCommands = new String[]{"USER","ACCT","PASS","TYPE","LIST","CDIR","KILL","NAME","TOBE","DONE","RETR","SEND","STOP","STOR"};
        
        try{
            socket = new Socket(HOSTNAME, PORT_NUMBER);
            aOutToServer = new DataOutputStream(socket.getOutputStream());
            aInFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bOutToServer = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            bInFromServer = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            System.out.println("Connected to " + HOSTNAME + " via port number " + PORT_NUMBER);
            System.out.println(responseFromServer()); // receiving greeting

            while(active){
                System.out.print("> ");
                String[] command = readSystemInput();
                if(command != null){
                    executeClientCommand(command);
                }
            }
        }
        catch (ConnectException c) {
            System.out.println("Connection not made. Server may be inactive.");
        }
        catch (Exception e){
            System.out.println("Some other error occurred.");
        }
    }

    /*
     * Reads an input string (from the Client)
     * If valid command type entry, returns a String array where the first element is the command and the remainder are the argument(s)
     */
    public static String[] readSystemInput() throws Exception {
        BufferedReader serverOutputReader = new BufferedReader(new InputStreamReader(System.in));
        String inStr = serverOutputReader.readLine();
        String[] input = inStr.split(" ");

        for(String c : validCommands){
            if(input[0].equals(c)){
                activeCommand = c;
                return input;
            }
        }

        System.out.println("ERROR: Input command not recognised");
        System.out.println("Valid commands: \"USER\", \"ACCT\", \"PASS\", \"TYPE\", \"LIST\", \"CDIR\", \"KILL\", \"NAME\", \"DONE\", \"RETR\", AND \"STOR\".");
        return null;
    }


    // TO DO: executeClientCommand
    public static void executeClientCommand(String[] args) throws Exception {
        if(!(activeCommand == null)){
            switch (activeCommand){
                case "USER":
                    validate("USER",args);
                    break;
                case "ACCT":
                    validate("ACCT",args);
                    break;
                case "PASS":
                    validate("PASS",args);
                    break;
                case "DONE":
                    try{
                        sendToServer("DONE");
                        System.out.println(responseFromServer());
                        socket.close();
                    }
                    catch (Exception e){
                        System.out.println("ERROR: Either connection was already lost or was unable to close connection now");
                    }
                    active = false;
                    break;
                case "TYPE":
                    type(args);
                    break;
                case "LIST":
                    list(args);
                    break;
                case "CDIR":
                    cdir(args);
                    break;
                case "KILL":
                    kill(args);
                    break;
                case "NAME":
                    name(args);
                    break;
                case "TOBE":
                    tobe(args);
                    break;
                case "RETR":
                    retr(args);
                    break;
                case "SEND":
                    send(args);
                    break;
            }
        }
    }
    
    /*
     * ONLY FOR INITIAL AUTHENTICATION COMMANDS (USER, ACCT, PASS)
     * validate(command, String[command, argument])
     * Checks for whether the input from the client system is the correct format, i.e. <command> [<SPACE> <args>] <NULL>
     * If it is, then the command will be sent to the server as a consolidated string, otherwise and error is thrown
     */
    public static void validate(String cmd, String[] args){
        if(args.length != 2){ // i.e. its should be <command> [<SPACE> <args>] <NULL>
            String s = null;
            switch (activeCommand) {
                case "USER":
                    s = " user-id";
                    break;

                case "ACCT":
                    s = " account";
                    break;
                case "PASS":
                    s = " password";
                    break;
            }
            
            System.out.println("ERROR: quantity of input arguments is " + (args.length-1) + ", where the format should be " + s + " for \"" + activeCommand + "\" command.");
            System.out.println("Arguments should be formatted as: <command> [<SPACE> <args>] <NULL>.");
        }
        else{ // correct argument amount
            sendToServer(activeCommand + " " + args[1]);
            System.out.println(responseFromServer());
        }
    }

    /*
     * TYPE CMD
     * type(String[command, argument])
     * This function checks whether the input string is correct for command TYPE
     * Arguments can either be "A", "B", or "C", representing, ASCII, binary and continuous respectively.
     * Type selected dictates how the transferred file will be mapped when stored.
     * Binary is the default type
     */
    public static void type(String[] args){
        if(args.length == 2 && args[0].equals("TYPE")){ // i.e. received both command and argument
            sendToServer(args[0]+" "+args[1].toLowerCase()); // TYPE + <A/B/C>
            String response = responseFromServer();

            if(response.substring(0, 1).equals("+")){
                sendType = args[1].toLowerCase();
            }
            System.out.println(response);
        }
        else{
            System.out.println("ERROR: Invalid argument input. Arguments for TYPE are: \"A\", \"B\", and \"C\".");
        }
    }

    /*
     * LIST CMD
     * list(String[command, argument, possible other arguments])
     * This function checks whether the input string is correct for command LIST
     * Arguments can either be "F" OR "V", for formatted or verbose directory respectively
     */
    public static void list(String[] args){
        if((args.length >= 2 && args[0].equals("LIST")) && (args[1].equals("F") || args[1].equals("V"))){ // i.e. must receive "LIST" command and specification, optionally a directory path
            if(args.length == 2){ // no path given
                sendToServer(args[0]+" "+args[1]);
            }
            else{ // directory given, if there was spaces in the directory path, previous processing would've split 
                String dir = "";
                for(int i = 2; i<args.length; i++){
                    if(i == (args.length-1)){
                        dir += args[i];
                    }
                    else{
                        dir += args[i] + " ";
                    }
                }
                sendToServer(args[0] + " " + args[1] + " " + dir); // LIST FORMAT DIRECTORY-PATH
            }
        }
        else{
            System.out.println("ERROR: Invalid argument input. Arguments for LIST are: \"F\", and \"V\".");
        }
        System.out.println(responseFromServer());
    }


    /*
     * CDIR CMD
     * cdir(String[command, argument, possible other arguments])
     * This function checks whether the input string is correct for command CDIR
     * It also fixes the path-directory input, by reconnecting it with spaces (as originally intended)
     */
    public static void cdir(String[] args){
        if(args.length >= 2){ // i.e. CDIR directory path
            String dir = "";
            for(int i = 1; i < args.length; i++){
                if(i == (args.length-1)){
                    dir += args[i];
                }
                else{
                    dir += args[i] + " ";
                }
            }
            sendToServer(args[0] + " " + dir);
            System.out.println(responseFromServer());
        }
        else{
            System.out.println("ERROR: Invalid argument input. 1 argument allowed for a new directory");
        }
    }

    /*
     * KILL CMD
     * kill(String[command, argument, possible other arguments])
     * This function checks whether the input string is correct for command KILL
     * Similar to circumstances of the CDIR command, 
     * the file-spec argument may have been previously split because of spaces in the file name
     * The function rejoins the argument with spaces, as originally input
     */
    public static void kill(String[] args){
        if(args.length >= 2){ // i.e. KILL file-spec, has a minimum of 2 arguments
            String spec = "";
            for(int i = 1; i < args.length; i++){
                if(i == (args.length-1)){
                    spec += args[i];
                }
                else{
                    spec += args[i] + " ";
                }
            }
            sendToServer(args[0] + " " + spec);
            System.out.println(responseFromServer());
        }
        else{
            System.out.println("ERROR: Invalid argument input. 1 argument allowed for file spec");
        }
    }

    /*
     * NAME CMD
     * name(String[command, argument, possible other arguments])
     * This function checks whether the input string is correct for command NAME
     * Similar to circumstances of the KILL command, the old-file-spec argument may have been previously split 
     * because of spaces in the file name. The function rejoins the argument with spaces, as originally input
     */
    public static void name(String[] args){
        if(args.length >= 2){ // i.e. NAME old-file-spec, has a minimum of 2 arguments
            String spec = "";
            for(int i = 1; i < args.length; i++){
                if(i == (args.length-1)){
                    spec += args[i];
                }
                else{
                    spec += args[i] + " ";
                }
            }
            sendToServer(args[0] + " " + spec);
            System.out.println(responseFromServer());
        }
        else{
            System.out.println("ERROR: Invalid argument input. 1 argument allowed for old file spec");
        }
    }

    public static void tobe(String[] args){
        if(args.length >= 2){ // i.e. TOBE new-file-spec, has a minimum of 2 arguments
            String spec = "";
            for(int i = 1; i < args.length; i++){
                if(i == (args.length-1)){
                    spec += args[i];
                }
                else{
                    spec += args[i] + " ";
                }
            }
            sendToServer(args[0] + " " + spec);
            System.out.println(responseFromServer());
        }
        else{
            System.out.println("ERROR: Invalid argument input. 1 argument allowed for new file spec");
        }
    }

    public static void retr(String[] args){
        if(args.length >= 2){ // i.e. RETR new-file-spec, has a minimum of 2 arguments
            String spec = "";
            for(int i = 1; i < args.length; i++){
                if(i == (args.length-1)){
                    spec += args[i];
                }
                else{
                    spec += args[i] + " ";
                }
            }
            sendToServer(args[0] + " " + spec);
            String response  = responseFromServer();
            System.out.println(response);
            if(!("-".equals(response.substring(0,1)))){
                retrFileSize = Integer.parseInt(response);
                retrFlag = true; 
                retrFileName = spec;
                System.out.println("Input either a SEND to receive file or STOP command to stop receiving process");
            }
        }
        else{
            System.out.println("ERROR: Invalid argument input. 1 argument allowed for file spec");
            retrFlag = false;
        }
    }

    public static void send(String[] args){
        if(args.length == 1 && args[0].equals("SEND")){ // i.e. RETR new-file-spec, has a minimum of 2 arguments
            sendToServer("SEND "); // needs space

            try{
                File fileSaveLocation = new File(clientFolderFile + "\\" + retrFileName);
                Long abortTime = new Date().getTime() + retrFileSize*1000;
                if(sendType.equals("a")){ // ASCII
                    BufferedOutputStream fileReceive = new BufferedOutputStream(new FileOutputStream(fileSaveLocation, false));
                    for(int i = 0; i < retrFileSize; i++){
                        if(new Date().getTime() > abortTime){
                            System.out.println("Time limit for file transfer reached. Timed out after " + retrFileSize*1000 + " seconds.");
                            return;
                        }
                        fileReceive.write(aInFromServer.read());
                    }
                    fileReceive.flush();
                    fileReceive.close();
                    retrFlag = false;
                    System.out.println("File saved at " + fileSaveLocation.getAbsolutePath());
                }
                else{ // BINARY
                    FileOutputStream fileReceive = new FileOutputStream(fileSaveLocation, false);
                    byte[] fileInBytes = new byte[(int) retrFileSize];
                    int idx = 0;
                    int s;
                    while(idx < retrFileSize){
                        s = bInFromServer.read(fileInBytes);
                        if(new Date().getTime() > abortTime){
                            System.out.println("Time limit for file transfer reached. Timed out after " + retrFileSize*1000 + " seconds.");
                            fileReceive.flush();
                            fileReceive.close();
                            return;
                        }
                        fileReceive.write(fileInBytes, 0, s);
                        idx+=s;
                    }
                    fileReceive.flush();
                    fileReceive.close();
                    retrFlag = false;
                    System.out.println("File saved at " + fileSaveLocation.getAbsolutePath());
                }
            } 
            catch (FileNotFoundException f){
                System.out.println("ERROR: client folder (saving location) doesn't exist");
            }
            catch(SocketException g){
                System.out.println("Server connection closed prematurely, file did not finish transferring.");
            }
            catch(Exception h){
                if(Server.seeSysOutput) h.printStackTrace();
            }
        }
        else{
            System.out.println("ERROR: Invalid argument input. No arguments permitted for SEND command");
        }
    }

    // // TO DO: STOR
    // public static void stor(String[] args){
    //     String spec = "";
    //     if(args.length >= 3){ // i.e. STOR file-spec, has a minimum of 3 arguments
    //         for(int i = 1; i < args.length; i++){
    //             if(i == (args.length-1)){
    //                 spec += args[i];
    //             }
    //             else{
    //                 spec += args[i] + " ";
    //             }
    //         }
    //     }

    //     File fileToSend = new File(activeDir.getPath()+ "/" + spec); // location for file
    //     if(!fileToSend.isFile()){ 
    //         System.out.println("File " + spec + " does not exist in pathing: " + activeDir.getPath().toString());
    //         return;
    //     }

    //     // // Compare sending type and existing file format
    //     // if("a".equals(sendType)){
    //     //     // if file not in ascii format
    //     // }
    //     // else if("b".equals(sendType) || "c".equals(sendType)){
    //     //     //if file not in binary formatting
    //     // }

    //     sendToServer(args[0] + args[1] + spec);
    //     String response = responseFromServer();
    //     System.out.println(response);

    //     if("+".equals(response.substring(0,1))){ 
    //         // Begin SIZE cmd
    //         System.out.println("File size sending: " + fileToSend.length() + " ... "); // file length in bytes/characters
    //         sendToServer("SIZE " + fileToSend.length());
    //         response = responseFromServer();
    //         System.out.println(response);

    //         if("+".equals(response.substring(0,1))){
    //             System.out.println("Sending...");

    //             try{
    //                 if("a".equals(sendType)){
    //                     // send ascii
    //                     aOutToServer.flush();
    //                     int b = 0;
                        
    //                 }
    //                 else{
    //                     // send binary or continuous
    //                     bOutToServer.flush();


    //                 }
    //             }

    //         }
    //     }
    //     else{
    //         System.out.println("ERROR: Invalid argument input. Must have 3 arguments, STOR { NEW | OLD | APP } file-spec");
    //     }


    // }

    /* 
     * sendToServer(command)
     * Sends command as ASCII to server
     * Error occurs if the client has already closed its connection
     */
    public static void sendToServer(String command){
        try{
			aOutToServer.writeBytes(command + '\0');
		}
		catch(IOException e){
			try{ // Connection is already closed, so close socket
				socket.close();
				active = false;
                System.out.println("ERROR: Cannot send command. Connection to server closed.");
			}
			catch (IOException f){}
		}
    }

    /*
	 * responseFromServer()
	 * Reads response string sent from server in ASCII format character by character
	 * Returns a string of completed response 
     * Essentially, callin this function causes the client to busy wait until it gets a response from the server
	 */	
    public static String responseFromServer(){
        String response = "";
		int character = 0;
		
		while(true){
			try{
				character = aInFromServer.read();
			}
			catch (IOException e){
				try { // when server is closed, close thread
					socket.close();
					active = false;
					break;
				}
				catch (IOException f) {
					System.out.println("Socket could not be closed");
				}
			}
			if((char) character == '\0' && response.length() > 0) break; // if null, stop reading
			if((char) character != '\0') response += (char) character; // otherwise add to string
		}
		
		// System.out.println("Input: " + command);
		return response;
	}
}

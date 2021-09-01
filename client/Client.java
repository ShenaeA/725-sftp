
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;


public class Client {
    static String[] validCommands;
    private static File clientFolderFile = new File(System.getProperty("user.dir"));
	private static final String rootDir = (clientFolderFile.getParentFile()).getAbsolutePath();
	static String activeDir = "\\";
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
                case "STOR":
                    stor(args);
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
        if((args.length != 2 && !(cmd.equals("USER"))) || ((args.length < 1 || args.length > 2) && (cmd.equals("USER")))){ // i.e. its should be <command> [<SPACE> <args>] <NULL>
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
            String response = responseFromServer();
            if("!".equals(response.substring(0,1))){
                if(rootDir.substring(rootDir.length() - 1).equals("\\") || rootDir.substring(rootDir.length() - 1).equals("/")){
                    activeDir = rootDir + dir;
                }
                else{
                    activeDir = rootDir + "\\" + dir;
                }
            }
            else if("E".equals(response.substring(0,1))){ // an error occurred, server also changes the active directory back to root
                activeDir = rootDir;
            }
            System.out.println(response);
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

    /*
     * TOBE CMD
     * tobe(String[command, argument, possible other arguments])
     * This function is the second half of the NAME command/renaming process
     */
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

    /*
     * RETR CMD
     * retr(String[command, argument, possible other arguments])
     * For requesting files of the remote system, saving to root/client i.e. 727-sftp/client
     * This function checks whether the input string is correct for command RETR
     * The old-file-spec argument may have been previously split because of spaces in the file name. 
     * The function rejoins the argument with spaces, as originally input
     */
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

            File f;
            if(("\\").equals(spec.substring(0,1)) || ("/").equals(spec.substring(0,1))){
                f = new File(rootDir + activeDir + spec);
            }
            else{
                f = new File(rootDir + activeDir + "\\" + spec);
            }
            
            try{
                boolean fileTypeIsBinary = isBinaryFile(f.getAbsolutePath());
                if((!fileTypeIsBinary && (sendType.equals("b") || sendType.equals("c"))) || (fileTypeIsBinary && (sendType.equals("a")))){
                    System.out.println("ERROR: conflicting send type and input file type. Send type is " + sendType.toUpperCase() + (fileTypeIsBinary ? " and file type is b/c" : " and file type is a"));
                    return;
                }
            }
            catch (IOException e){
                System.out.println("ERROR: IO Exception occurred");
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
            else{
                retrFlag = false;
            }
        }
        else{
            System.out.println("ERROR: Invalid argument input. 1 argument allowed for file spec");
            retrFlag = false;
        }
    }
    
    /*
     * SEND CMD
     * send(String[command, argument, possible other arguments], only looking for the command though
     * This function checks whether the input string is correct for command SEND
     * A second half of the RETR function/process to request a file be sent to root/client
     */
    public static void send(String[] args){
        if(!retrFlag){
            System.out.println("ERROR: need valid RETR command before SEND can be used");
        }
        else if(args.length == 1 && args[0].equals("SEND") && retrFlag){ // i.e. RETR new-file-spec, has a minimum of 2 arguments
            sendToServer("SEND "); // needs space

            try{
                File fileSaveLocation = new File(clientFolderFile + "\\" + retrFileName);
                Long abortTime = new Date().getTime() + retrFileSize; // 1 millisecond per byte timeout
                if(sendType.equals("a")){ // ASCII
                    BufferedOutputStream fileReceive = new BufferedOutputStream(new FileOutputStream(fileSaveLocation, false));
                    for(int i = 0; i < retrFileSize; i++){
                        if(new Date().getTime() > abortTime){
                            System.out.println("Time limit for file transfer reached. Timed out after " + retrFileSize + " milliseconds.");
                            fileReceive.close();
                            return;
                        }
                        fileReceive.write(aInFromServer.read());
                    }
                    fileReceive.flush();
                    fileReceive.close();
                    retrFlag = false;
                    // System.out.println("+File saved at " + fileSaveLocation.getAbsolutePath());
                }
                else{ // BINARY
                    FileOutputStream fileReceive = new FileOutputStream(fileSaveLocation, false);
                    byte[] fileInBytes = new byte[(int) retrFileSize];
                    int idx = 0;
                    int s;
                    while(idx < retrFileSize){
                        s = bInFromServer.read(fileInBytes);
                        if(new Date().getTime() > abortTime){
                            System.out.println("Time limit for file transfer reached. Timed out after " + retrFileSize + " milliseconds.");
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
                System.out.println("ERROR: Server connection closed prematurely, file did not finish transferring.");
            }
            catch(Exception h){
                System.out.println("ERROR: Something went wrong. Check file still exists");;
            }
        }
        else{
            System.out.println("ERROR: Invalid argument input. No arguments permitted for SEND command");
        }
    }

    /*
     * STOP CMD
     * stop(String[command, argument, possible other arguments], only looking for the command though
     * This function stops the RETR process
     */
    public static void stop(String[] args){
        if(args.length == 1 && args[0].equals("STOP")){ // i.e. RETR new-file-spec, has a minimum of 2 arguments
            sendToServer("STOP "); // needs space
            System.out.println(responseFromServer());
        }
        else{
            System.out.println("ERROR: Invalid argument input. No arguments permitted for STOP command");
        }

    }



    /*
     * STOR CMD
     * stor(String[command, argument, possible other arguments]
     * The function is the first step for receiving files from the host system
     * Sends SIZE command automatically
     */
    public static void stor(String[] args){
        String spec = "";
        if(args.length >= 3){ // i.e. STOR file-spec, has a minimum of 3 arguments
            for(int i = 2; i < args.length; i++){
                if(i == (args.length-1)){
                    spec += args[i];
                }
                else{
                    spec += args[i] + " ";
                }
            }
        }
        else{
            System.out.println("ERROR: Invalid argument input. Must have 3 arguments, STOR { NEW | OLD | APP } file-spec");
        }

        // validating file, case sensitive
        boolean exists = false;
        File f = new File(activeDir);
        for(File file : f.listFiles()){
            if(spec.equals(file.getName()) && file.isFile()){
                exists = true;
            }
        }

        if(!exists){ 
            System.out.println("ERROR: File " + spec + " does not exist in pathing: " + activeDir);
            return;
        }
        
        // // Compare sending type and existing file format
        // if("a".equals(sendType)){
        //     // if file not in ascii format
        // }
        // else if("b".equals(sendType) || "c".equals(sendType)){
        //     //if file not in binary formatting
        // }

        sendToServer(args[0] + " " + args[1] + " " + spec);
        String response = responseFromServer();
        System.out.println(response);

        File fileToSend = new File(activeDir + "/" + spec); // location for file
        String fileSize = Long.toString(fileToSend.length());

        if("+".equals(response.substring(0,1))){ 
            // Begin SIZE cmd
            System.out.println("Validating that there is space for file. File to be sent size is " + fileToSend.length() + " bytes"); // file length in bytes/characters
            sendToServer("SIZE " + fileSize);
            response = responseFromServer();
            System.out.println(response);

            if("+".equals(response.substring(0,1))){
                System.out.println("Sending...");

                try{
                    byte[] fileInBytes = new byte[(int) fileToSend.length()];
                    if(sendType.equals("a")){ // ASCII
                        BufferedInputStream readInFile = new BufferedInputStream(new FileInputStream(fileToSend));
                        aOutToServer.flush();
    
                        int b = 0;
                        while((b =readInFile.read(fileInBytes)) >= 0){
                            aOutToServer.write(fileInBytes, 0, b);
                        }
    
                        readInFile.close();
                    }
                    else{ // BINARY
                        FileInputStream readInFile = new FileInputStream(fileToSend);
                        bOutToServer.flush();
                        int b = 0;
                        while((b = readInFile.read()) >= 0){
                            bOutToServer.write(b);
                        }
                        readInFile.close();
                        bOutToServer.flush();
                    }

                    response = responseFromServer();
                    System.out.println(response);
                }
                catch(IOException e){
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    active = false;
                    System.out.println("IOException occurred. Sending unsuccessful");
                }
                catch(Exception g){
                    System.err.println(g);
                }
            }
        }
        
    }

    /*
     * Helper function to detrrmine if an input file is binary or not 
     */
    public static boolean isBinaryFile(String s) throws IOException {
        File f = new File(s);
        String type = Files.probeContentType(Paths.get(s));
        if (type == null) {
            //type couldn't be determined, assume binary
            return true;
        } else if (type.startsWith("text")) {
            return false;
        } else {
            //type isn't text
            return true;
        }
    }

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

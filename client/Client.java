
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;


public class Client {
    static String[] validCommands;
    static String HOSTNAME = "localhost";
    static int PORT_NUMBER = 9999;
    static Socket socket;
    static boolean active = true;
    static String activeCommand;
    static String sendType = "b";

    static DataOutputStream aOutToServer;
    static BufferedReader aInFromServer;
    static DataOutputStream bInToServer;
    static DataInputStream bInFromServer;

    public static void main(String[] args) throws Exception {
        new File(System.getProperty("user.dir")+"\\ft").mkdir(); // creats file transfer (ft) folder if doesn't already exist

        validCommands = new String[]{"USER","ACCT","PASS","TYPE","LIST","CDIR","KILL","NAME","TOBE","DONE","RETR","SEND","STOP","STOR"};
        
        try{
            socket = new Socket(HOSTNAME, PORT_NUMBER);
            aOutToServer = new DataOutputStream(socket.getOutputStream());
            aInFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bInToServer = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
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
                case "TYPE":
                    type(args);
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
            System.out.println("ERROR: Invalid argument input. Arguments for type are: \"A\", \"B\", and \"C\".");
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

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.stream.*;

/*
 * ServerInstance 
 * A thread which handles a new socket connection
 */
 
public class ServerThreadInstance extends Thread{
	protected Socket socket;

	boolean active = true;
	String posGreeting = "+SFTP RFC913 Server Activated :)\0";
	String negGreeting = "-Connection to SFTP RFC913 Server Failed :(\0";

	String state; 

	// Data streams between client and server
	BufferedReader aInFromClient;
	DataOutputStream aOutToClient;
	DataInputStream bInFromClient;
	DataOutputStream bOutToClient;
	
	// User Authentication
	protected static Authoriser authoriser;
	
	// Flags
	boolean name = false;
	boolean retr = false;
	
	String sendType = "b"; // Default send type is binary
	String storType = ""; 	// Store type (NEW | OLD | APP)
	long fileLength;		// Length of file to store
	private static final String workingDir = System.getProperty("user.dir")+"ft";
	String activeDir = "";
	
	ServerThreadInstance(Socket s, String authFile){
		this.socket = s;
		ServerThreadInstance.authoriser = new Authoriser(authFile);
	}
	
	@Override
	public void run(){
		try{
			socket.setReuseAddress(true);
			bOutToClient = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			bInFromClient = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			aInFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			aOutToClient = new DataOutputStream(socket.getOutputStream());
			sendToClient(posGreeting);
		}
		catch (Exception e){
			if(Server.seeSysOutput) System.out.println("Something went wrong. Connection not made.");
			sendToClient(negGreeting); 
		}
		
		while(active){
			try {
				String[] commandFromClient = commandFromClient().split(" ");
				if("DONE".equals(commandFromClient[0])){
					sendToClient("+Finishing command received. Closing connection...");
					socket.close();
					active = false;
					break;
				}
				else{
					state(commandFromClient);
				}
			} catch (Exception e){}
		}
		if(Server.seeSysOutput) System.out.println("Closed thread");		
	}
	
	/*
	 * state(command)
	 * Takes the input cmd and decides which is the correct function
	 * Possible args are: "USER", "ACCT", "PASS", "TYPE", "LIST", "CDIR', "KILL', "NAME", "DONE", "RETR", AND "STOR"
	 */
	public void state(String[] command) throws Exception { 
		switch (command[0]){
			case "USER":
				sendToClient(authoriser.user(command[1]));
			break;
			
			case "ACCT":
				sendToClient(authoriser.acct(command[1]));
			break;
	
			case "PASS":
				sendToClient(authoriser.pass(command[1]));
			break;
			
			case "TYPE":
				if(authoriser.loggedIn()){
					sendToClient(type(command[1]));
				}
				else{
					sendToClient("-Command not available, please log in first.");
				}
				break;
			
			case "LIST":
				if(authoriser.loggedIn()){
					sendToClient(list(command));
				}
				else{
					sendToClient("-Command not available, please log in first.");
				}
				break;
			
			// case "CDIR":
			// 	if(authoriser.loggedIn()){
			// 		cdir(command[1]);
			// 	}
			// 	else{
			// 		sendToClient("-Command not available, please log in first.");
			// 	}
			// 	break;
			
			// case "KILL":
			// 	if(authoriser.loggedIn()){
			// 		kill(command[1]);
			// 	}
			// 	else{
			// 		sendToClient("-Command not available, please log in first.");
			// 	}
			// 	break;
			
			// case "NAME":
			// 	if(authoriser.loggedIn()){
			// 		name(command[1]);
			// 	}
			// 	else{
			// 		sendToClient("-Command not available, please log in first.");
			// 	}
			// 	break;
			
			// case "RETR":
			// 	if(authoriser.loggedIn()){
			// 		retr(command[1]);
			// 	}
			// 	else{
			// 		sendToClient("-Command not available, please log in first.");
			// 	}
			// 	break;
			
			// case "STOR":
			// 	if(authoriser.loggedIn()){
			// 		stor(command[1]);
			// 	}
			// 	else{
			// 		sendToClient("-Command not available, please log in first.");
			// 	}
			// 	break;
		}
			
	}

	/*
	 * TYPE CMD
	 * There are three methods of sending, ASCII, BINARY, and CONTINUOUS (a, b, c).
	 * If an invalid type is given, the current type will remain the same.
	 */
	public String type(String inStr){
		String response = null;
		if(inStr != null){
			switch(inStr.toLowerCase()){
				case "a":
					sendType = "a";
					response = "+Using Ascii mode";
					break;
				case "b":
					sendType = "b";
					response = "+Using Binary mode";
					break;
				case "c":
					sendType = "c";
					response = "+Using Continuous mode";
					break;
				default:
					response = "-Type not valid";
					break;
			}
		}
		else {
			response = "-Type not valid";
		}
		return response;
	}

	/*
	 * LIST CMD
	 * There are three methods of sending, ASCII, BINARY, and CONTINUOUS (a, b, c).
	 * If an invalid type is given, the current type will remain the same.
	 */
	public String list(String[] args){
		String response = null;
		// readding white space
		String dir = "";
		if(args.length > 3){
			for(int i = 2; i < args.length; i++){
                if(i == (args.length-1)){
                    dir += args[i];
                }
                else{
                    dir += args[i] + " ";
                }
            }
		}
		else{
			if(args.length == 3){ // no white space path
				dir = args[2];
			}
			else if(args.length == 2){
				dir = "./";
			}
			else{
				if(Server.seeSysOutput) System.out.println("Wrong args amount received. Got " + args.length);
				return "-Wrong args ammount";
			}
			
		}
		if(!"./".equals(dir)){ // i.e. either want a directory path (e.g. Documents/ft/) or an empty string
			if(!dir.substring(0,1).equals("/")){
				dir = "/" + dir;
			}
		}
		

		if(args[1] != null){
			switch(args[1]){
				case "F":
					List<String> inSetOfFiles = Stream.of(new File(dir).listFiles()).filter(file -> !file.isDirectory()).map(File::getName).collect(Collectors.toList());
					String files = "";
					for(String s : inSetOfFiles){
						files += s + "\r\n";
					}
					response = "+" + dir + "\r\n" + files;
					break;
				case "V":
					response = "V";
					break;
				default:
					response = "-Type not valid";
					break;
			}
		}
		else {
			response = "-Type not valid";
		}
		return response;
	}


	/*
	 * STOR CMD (inc SIZE)
	 * 
	 * 
	 */
	private String stor(String[] args){
		String response = null;
		// refilling any white space in file-sped
		String fileName = "";
		if(args.length > 3){
			for(int i = 2; i < args.length; i++){
                if(i == (args.length-1)){
                    fileName += args[i];
                }
                else{
                    fileName += args[i] + " ";
                }
            }
		}

		if(args[1] != null){
			switch(args[1]){
				case "NEW":
					File file = new File(fileName);
					if(file.isFile()){
						sendToClient("+File exists, will create new generation of file");
					}
					else{
						sendToClient("+File does not exist, will create new file");
					}

					


					break;

				case "OLD":


					break;

				case "APP":


					break;
			}
		}
		return response;
	}



	/* 
     * sendToClient (command)
     * Sends command as ASCII to server
     * Error occurs if the client has already closed its connection
     */
	private void sendToClient(String s){
		try{
			aOutToClient.writeBytes(s + '\0');
		}
		catch(IOException e){
			try{ // Client has closed connection, so close socket
				socket.close();
				active = false;
			}
			catch (IOException f){}
		}
	}
		

	/*
	 * commandFromClient()
	 * Reads command string sent from client in ASCII format character by character
	 * Returns a string of completed command 
	 */	
	private String commandFromClient(){
		String command = "";
		int character = 0;
		
		while(true){
			try{
				character = aInFromClient.read();
			}
			catch (IOException e){
				try { // when server is closed, close thread
					socket.close();
					active = false;
					break;
				}
				catch (IOException f) {
					if(Server.seeSysOutput) System.out.println("Socket could not be closed");
				}
			}
			if((char) character == '\0' && command.length() > 0) break; // if null, stop reading
			if((char) character != '\0') command += (char) character; // otherwise add to string
		}
		
		if(Server.seeSysOutput) System.out.println("Input: " + command);
		return command;
	}
		
}
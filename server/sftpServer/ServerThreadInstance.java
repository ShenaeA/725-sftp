import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

import javax.imageio.IIOException;

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
	
	String storType = ""; 	// Store type (NEW | OLD | APP)
	long fileLength;		// Length of file to store
	
	long netIO = 0;		// IO transferred counter
	
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
			System.out.println("Something went wrong. Connection not made.");
			sendToClient(negGreeting); 
		}
		
		while(active){
			try {
				String[] commandFromClient = commandFromClient().split(" ");
				if(commandFromClient[0] == "DONE"){
					sendToClient("+Closing connection. total transferred is " + netIO/1000 + "kBs.");
					socket.close();
					active = false;
					break;
				}
				else{
					state(commandFromClient);
				}
			} catch (Exception e){}
		}
		System.out.println("Closed thread");		
	}
	
	// state takes the input cmd and decides which is the correct function
	// possible args are: "USER", "ACCT", "PASS", "TYPE", "LIST", "CDIR', "KILL', "NAME", "DONE", "RETR", AND "STOR"
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
			
			break;
			
			case "LIST":
			
			break;
			
			case "CDIR":
			
			break;
			
			case "KILL":
			
			break;
			
			case "NAME":
			
			break;
			
			case "RETR":
			
			break;
			
			case "STOR":
			
			break;
			
			case "TOBE":
			
			break;
			
			case "SEND":
			
			break;
			
			case "STOP":
			
			break;
		}
			
	}

	// TO DO: sendToClient();
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
					System.out.println("Socket could not be closed");
				}
			}
			if((char) character == '\0' && command.length() > 0) break; // if null, stop reading
			if((char) character != '\0') command += (char) character; // otherwise add to string
		}
		
		// System.out.println("Input: " + command);
		return command;
	}
		
}
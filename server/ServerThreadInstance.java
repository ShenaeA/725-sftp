import java.io.*;
import java.net.Socket;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.*;

import javax.xml.namespace.QName;

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
	
	// Flags + GV
	boolean nameFlag = false;
	boolean retrFlag = false;
	long retrFileLength;
	String retrFileDirAndName = "";
	String oldFileSpec = "";
	String oldFileDir = "";
	String sendType = "b"; // Default send type is binary
	String storType = ""; 	// Store type (NEW | OLD | APP
	
	private static File serverFolderFile = new File(System.getProperty("user.dir"));
	private static final String rootDir = (serverFolderFile.getParentFile()).getAbsolutePath();
	String activeDir = rootDir;
	 
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
				if(command.length != 2){
					sendToClient("-ERROR: incorrect argument quantity. USER command only requires one argument <USER username>");
				}
				else {
					sendToClient(authoriser.user(command[1]));
				}
			break;
			
			case "ACCT":
				if(command.length != 2){
					sendToClient("-ERROR: incorrect argument quantity. ACCT command only requires one argument <ACCT accountName>");
				}
				else {
					sendToClient(authoriser.acct(command[1]));
				}
			break;
	
			case "PASS":
				if(command.length != 2){
					sendToClient("-ERROR: incorrect argument quantity. PASS command only requires one argument <PASS password>");
				}
				else {
					sendToClient(authoriser.pass(command[1]));
				}
			break;
			
			case "TYPE":
				if(authoriser.loggedIn()){
					if(command.length != 2){
						sendToClient("-ERROR: incorrect argument quantity. TYPE command only requires one argument <TYPE A|B|C>");
					}
					else {
						sendToClient(type(command[1]));
					}
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
			
			case "CDIR":
				if(authoriser.loggedIn()){
					sendToClient(cdir(command));
				}
				else{
					sendToClient("-Command not available, please log in first.");
				}
				break;
			
			case "KILL":
				if(authoriser.loggedIn()){
					sendToClient(kill(command));
				}
				else{
					sendToClient("-Command not available, please log in first.");
				}
				break;
			
			case "NAME":
				if(authoriser.loggedIn()){
					sendToClient(name(command));
				}
				else{
					sendToClient("-Command not available, please log in first.");
				}
				break;

			case "TOBE":
				if(authoriser.loggedIn()){
					sendToClient(tobe(command));
				}
				else{
					sendToClient("-Command not available, please log in first.");
				}
				break;
			
			case "RETR":
				if(authoriser.loggedIn()){
					sendToClient(retr(command));
				}
				else{
					sendToClient("-Command not available, please log in first.");
				}
				break;

			case "STOP":
				if(authoriser.loggedIn()){
					sendToClient(stop(command));
				}
				else{
					sendToClient("-Command not available, please log in first.");
				}
				break;

			case "SEND":
				if(authoriser.loggedIn()){
					sendToClient(send(command));
				}
				else{
					sendToClient("-Command not available, please log in first.");
				}
				break;
			
			// case "STOR":
			// 	if(authoriser.loggedIn()){
			// 		sendToClient(stor(command[1]));
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
		String dir = activeDir;
		String printDir = "\\";
		if(args.length > 3){
			for(int i = 2; i < args.length; i++){
                if(i == (args.length-1)){
                    printDir += args[i];
                }
                else{
                    printDir += args[i] + " ";
                }
            }
		}
		else{
			if(args.length == 3){ // no white space path
				printDir += args[2];
			}
			else if(args.length < 2) {
				if(Server.seeSysOutput) System.out.println("Wrong args amount received. Got " + args.length);
				return "-Wrong args ammount";
			}
			
		}
		dir = rootDir + dir + printDir;

		if(!(new File(dir).isDirectory())){
			return "-Invalid directory";
		}
		
		if(args[1] != null){
			switch(args[1]){
				case "F":
					try {
						List<String> inSetOfFiles = Stream.of(new File(dir).listFiles()).map(File::getName).collect(Collectors.toList());
						String files = "+" + printDir + "\r\n";
						if(inSetOfFiles!= null){
							for(String s : inSetOfFiles){
								files += s + "\r\n";
							}
						}
						response = files;
					}
					catch (Exception e){
						if (Server.seeSysOutput){
							System.err.println(e);
							activeDir = "";
							return "-"+ e.toString(); // return error to client
						}
					}
					
					break;
				case "V":
					try {
						StringBuilder build = new StringBuilder();
						build.append("+").append(printDir).append("\r\n");
						build.append(String.format("%-64s%-10s%-4s%-27s%-1s", "|Name", "|Size (kB)", "|R/W", "|Last Modified", "|")).append("\r\n");
						
						// Get file info
						ArrayList<FileInfo> filesInfo = new ArrayList<FileInfo>();
						File directory = new File(dir);
						if(directory!=null){
							for(File file : directory.listFiles()){
								filesInfo.add(new FileInfo(file.getName(), 
									DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG).format(new Date(file.lastModified())),
									file.length(),
									file.canRead(),
									file.canWrite()));
																
							}
						}

						// Format display
						for(FileInfo f : filesInfo){
							String temp = "";
							temp += String.format("%-64s", "|"+ f.getName());
							temp += String.format("%-10s", "|" + f.getSize()/1000);
							temp += String.format("%-4s", "|" + f.getReadWrite());
							temp += String.format("%-27s", "|" + f.getModified());
							temp += String.format("%-1s", "|");
							temp += "\r\n";
							build.append(temp);
						}

						response = build.toString();

					}
					catch (DirectoryIteratorException | InvalidPathException f) {
						if(Server.seeSysOutput) System.err.print(f);
						response = "-" + f.toString();
					}
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
	 * KILL CMD
	 * Deletes <file-spec> file from server system
	 * Permission is decided based on the active user/acct. 
	 * NOTE: not based on password as password would've been inout to login for that user
	 */
	private String kill(String[] args){
		try{
			String response = null;
			String fileSpec = "";
			File delete;
			if(args.length >= 2) {
				fileSpec = whitespace(args, 2);
			}
			else{
				return "-Wrong argument amount";
			}

			// Checking that potential file being accessed isn't in a restricted folder that the current user cannot access
			if(fileSpec.contains("\\") || fileSpec.contains("/")){
				int idx1 = fileSpec.lastIndexOf("\\") + 1;
				int idx2 = fileSpec.lastIndexOf("/") + 1;
				String dir = fileSpec.substring(0, Math.max(idx1, idx2)); // gets the directory

				if(!(dir.substring(0)).equals("\\") || (dir.substring(0)).equals("/")){ // checks formatting as activeDir doesn't have a slash afterwards
					dir = "\\" + dir;
				}

				//[0 = no restriction], [1 = restricted but required USER/ACCT/PW is currently active],
				//[2 = restriction, need ACCT/PW of current user (i.e. need to change acounts)], 
				//[3 = restricted, current user has no access], [4 = an error with the .restricted file], [5 = another error occurred]
				int result = restricted(rootDir + activeDir + dir);
				if(!(result == 0 || result == 1)){
					return "-Not deleted because current user does not have permission to access to that directory. File existence unknown";
				}
			}

			if(activeDir.substring(activeDir.length() - 1).equals("\\")){
				delete = new File(rootDir + activeDir + fileSpec);
			}
			else{
				delete = new File(rootDir + activeDir + "\\" + fileSpec);
			}
			
			boolean exists = false;
			File f = new File(rootDir + activeDir);

			for(File file : f.listFiles()){
				if(fileSpec.equals(file.getName()) && file.isFile()){
					exists = true;
				}
			}
			

			if(!exists){
				return "-Not deleted because file does not exist. Note: case sensitive";
			}

			if(delete.delete()){
				response = "+" + fileSpec + " deleted";
			}
			else{
				response = "-Not deleted because: reason unknown";
			}
			return response;

		}
		catch (SecurityException e){
			return "-Not deleted because a security exception occured: " + e.toString();
		}
		
	}

	/*
	* CDIR CMD
	* For changing the active directory
	* Permission is decided based on the active user/acct. 
	* NOTE: not based on password as password would've been inout to login for that user
	*/
	private String cdir(String[] args){
		String response = "";
		String dir = "";
		try{
			if(args.length >=2){
				dir = whitespace(args, 2);
			}
			else{
				return "-Wrong argument amount";
			}
			if(!"/".equals(dir.substring(0,1))){
				dir = "/" + dir;
			}

			File f = new File(rootDir + dir);
			if(!f.isDirectory()){
				return "-Can't connect to directory because: input is not a valid directory";
			}

			//[0 = no restriction], [1 = restricted but required USER/ACCT/PW is currently active],
	 		//[2 = restriction, need ACCT/PW of current user (i.e. need to change acounts)], 
	 		//[3 = restricted, current user has no access], [4 = an error with the .restricted file], [5 = another error occurred]
			switch(restricted(rootDir + dir)){
				case 0:
					activeDir = dir;
					response = "!Changed working directory to " + dir;
					break;
				case 1:
					activeDir = dir;
					response = "!Changed working directory to " + dir;
					break;
				case 2:
					sendToClient("+Directory ok, send account"); 
					String[] responseFromClient = commandFromClient().split(" "); 
					
					while(!responseFromClient[0].equals("ACCT") || responseFromClient.length != 2){
						sendToClient("-Incorrect input, please send account, <ACCT accountName>");
						responseFromClient = commandFromClient().split(" "); 
					}
					if(authoriser.acctCDIR(responseFromClient[1])){ // account was changed
						if(restricted(dir) == 1){
							response = "!Changed working directory to " + dir;
							break;
						}
					}
					response = "-Account exists but does not have permissions to enter this directory. Account has been changed to " + Authoriser.account + " and current directory is " + ((new File(rootDir + activeDir)).getAbsolutePath());
					break;
				case 3:
					response = "-Cannot connect to " + dir + " because: current user does not have permission to access";
					break;
				case 4:
					response = "-Cannot connect to " + dir + " because: error occurred with restricted file";
					break;
				case 5:
					response = "-Cannot connect to " + dir + " because: unknown error occurred";
					break;
			}
			return response;
		}
		catch (Exception e){
			activeDir = rootDir;
			return "ERROR: unknown error occurred, current directory changed to root";
		}
	}


	/* 
	 * NAME CMD
	 * First part of the renaming process
	 * Permission is decided based on the active user/acct. 
	 * NOTE: not based on password as password would've been inout to login for that user
	 */
	private String name(String[] args){
		String response = "";
		String filename = "";

		if(args.length >= 2){
			filename = whitespace(args, 2);
		}
		else {
			nameFlag = false;
			return "-ERROR: wrong argument amount, 1 argument rquired for NAME cmd <NAME old-file-spec>";
		}
		
		// Checking that file being accessed isn't in a restricted folder that the current user cannot access
		if(filename.contains("\\") || filename.contains("/")){
        	int idx1 = filename.lastIndexOf("\\") + 1;
			int idx2 = filename.lastIndexOf("/") + 1;
        	String dir = filename.substring(0, Math.max(idx1, idx2)); // gets the directory

			if(!(dir.substring(0)).equals("\\") || (dir.substring(0)).equals("/")){ // checks formatting
				dir = "\\" + dir;
			}

			//[0 = no restriction], [1 = restricted but required USER/ACCT/PW is currently active],
	 		//[2 = restriction, need ACCT/PW of current user (i.e. need to change acounts)], 
	 		//[3 = restricted, current user has no access], [4 = an error with the .restricted file], [5 = another error occurred]
			if(!(restricted(rootDir + activeDir + dir) == 0 || restricted(rootDir + activeDir + dir) == 1)){
				nameFlag = false;
				return "-Not deleted because current user does not have permission to access to that directory. File existence unknown";
			}
		}

		boolean exists = false;
		File dir = new File(rootDir + activeDir);

		for(File file : dir.listFiles()){
			if(filename.equals(file.getName()) && file.isFile()){
				exists = true;
			}
		}

		if(exists){
			if(!(filename.equals(oldFileSpec)) || !nameFlag){ // i.e. NAME CMD hasn't been sent previously
				oldFileSpec = filename;
				if(activeDir.substring(activeDir.length() - 1).equals("\\")){ // ensures oldFileSpec string always ends in a slash
					oldFileDir = rootDir + activeDir;
				}
				else{
					oldFileDir = rootDir + activeDir + "\\";
				}
				nameFlag = true;
				response = "+File exists, send TOBE command with file's new name";
			}
			else{
				response = "-Already send name, please send TOBE cmd";
			}
		}
		else{
			response = "-Invalid file name. Is not a file in this directory";
			nameFlag = false;
		}

		return response;
	}


	/*
	 * TOBE CMD
	 * Used for the second part of the renaming process, where the new file name is received and the file is renamed
	 * A valid NAME command must be received beofre this function can be successful
	 * Assumes valid permissions base on previous verification with initial NAME command
	 */
	private String tobe(String[] args){
		String response = "";
		if(nameFlag){
			String newFileName = "";
			if(args.length >= 2){
				newFileName = whitespace(args, 2);
			}
			else{
				nameFlag = false;
				return "-File wasn't renamed because wrong argument amount, 1 argument required for TOBE cmd <TOBE new-file-spec>";
			}

			String dir = "";
			if(activeDir.substring(activeDir.length() - 1).equals("\\")){
				dir = rootDir + activeDir;
			}
			else{
				dir = rootDir + activeDir + "\\";
			}
			// Check that haven't changed directories
			if(!(oldFileDir.equals(dir))){
				nameFlag = false;
				File f = new File(dir);
				return "-File wasn't renamed because there has been a change in current directory since last NAME command. Current directory is " + f.getAbsolutePath() + ". Please restart renaming process.";
			}
			
			File oldname = new File(oldFileDir + oldFileSpec);
			File newName = new File(oldFileDir + newFileName);
			boolean successFlag = oldname.renameTo(newName);

			if(successFlag){
				response = "+" + oldFileSpec + " renamed to " + newFileName;
			}
			else{
				response = "-File wasn't renamed for an unknown reason, please restart renaming process";
			}

		}
		else {
			response = "-ERROR: send NAME cmd first";
		}

		nameFlag = false;
		return response;
	}

	/*
	 * RETR CMD
	 * When client is requesting the server send a file
	 * 
	 */
	private String retr(String[] args){
		String fileSpec = "";
		if(args.length >= 2){
			fileSpec = whitespace(args, 2);
		}
		else {
			return "-ERROR: wrong argument amount, 1 argument required for RETR cmd <RETR file-spec>";
		}
		
		// Checking that file being accessed isn't in a restricted folder that the current user cannot access
		if(fileSpec.contains("\\") || fileSpec.contains("/")){
        	int idx1 = fileSpec.lastIndexOf("\\") + 1;
			int idx2 = fileSpec.lastIndexOf("/") + 1;
        	String dir = fileSpec.substring(0, Math.max(idx1, idx2)); // gets the directory

			if(!(dir.substring(0)).equals("\\") || (dir.substring(0)).equals("/")){ // checks formatting
				dir = "\\" + dir;
			}

			//[0 = no restriction], [1 = restricted but required USER/ACCT/PW is currently active],
	 		//[2 = restriction, need ACCT/PW of current user (i.e. need to change acounts)], 
	 		//[3 = restricted, current user has no access], [4 = an error with the .restricted file], [5 = another error occurred]
			if(!(restricted(rootDir + activeDir + dir) == 0 || restricted(rootDir + activeDir + dir) == 1)){
				return "-ERROR: cannot make requests for file(s) in that directory, current user does not have permission to access to that directory";
			}
		}

		File dir = new File(rootDir + activeDir);

		for(File file : dir.listFiles()){
			if(fileSpec.equals(file.getName()) && file.isFile()){
				retrFlag = true;
				retrFileLength = file.length();
				retrFileDirAndName = rootDir + activeDir + "\\" + file.getName();
				return Long.toString(retrFileLength);
			}
		}

		return "-File does not exist";
	}

	/*
	 * SEND CMD
	 * Valid calls only occur after a valid RETR CMD call
	 * 
	 */
	private String send(String[] args){
		if(retrFlag){
			try{
				byte[] fileInBytes = new byte[(int) retrFileLength];
				if(Server.seeSysOutput){
					System.out.println("File length: " + Long.toString(retrFileLength) + "bytes " + ". \r\nSend type: \"" + sendType + "\".");
				} 
				File file = new File(retrFileDirAndName);
				if(sendType.equals("a")){ // ASCII
					BufferedInputStream readInFile = new BufferedInputStream(new FileInputStream(file));
					aOutToClient.flush();

					int b = 0;
					while((b =readInFile.read(fileInBytes)) >= 0){
						aOutToClient.write(fileInBytes, 0, b);
					}

					readInFile.close();
				}
				else{ // BINARY
					FileInputStream readInFile = new FileInputStream(file);
					bOutToClient.flush();

					int b = 0;

					while((b = readInFile.read()) >= 0){
						bOutToClient.write(b);
					}

					readInFile.close();
				}
			}
			catch(IOException e){
				try {
					socket.close();
				} catch (IOException f) {
					f.printStackTrace();
				}
				active = false;
				return "-IOException occurred. Sending unsuccessful";
			}
			catch(Exception g){
				if(Server.seeSysOutput) System.err.println(g);
				return "-Sending unsuccessful";
			}
		}
		else{
			return "-Not file chosen. Need valid RETR command first.";
		}
		retrFlag = false;
		return "+Sending successful";
	}

	/*
	 * STOP CMD
	 * Receiving a STOP after a RETR command means that the client doesn't want to receive the file anymore, 
	 * and to cancel the retrieval process
	 */
	private String stop(String[] args){
		if(args.length != 1 || !(args[0].equals("STOP"))){
			return "-ERROR: incorrect input, 1 argument required for RETR cmd <RETR file-spec>";
		}

		if(retrFlag){
			retrFlag = false;
			return "+Ok, RETR aborted";
		}
		else{
			return "-RETR was never started, so nothing to stop";
		}
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
					if(file.isFile()){ // not case sensitive
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
	 * whitespace - helper function
	 * Send/receive involves converting a string into a string array, splitting at every whitespace
	 * This function combines the third argument back together as originally intended
	 * e.g. Input string from client: LIST F directoryName/folder 1/ 
	 * received as 4 arguments: [LIST, F, directoryName/folder, 1/] 
	 * Converted back to 3 arguments: [LIST, F, directoryName/folder 1/]
	 */
	private String whitespace(String args[], int reqArgs){
		String dir = "";
		if(args.length >= reqArgs){
			for(int i = reqArgs-1; i < args.length; i++){
                if(i == (args.length-1)){
                    dir += args[i];
                }
                else{
                    dir += args[i] + " ";
                }
            }
		}
		return dir;
	}

	/*
	 * restricted
	 * Helper function for checking if a folder/directory is resticted. Restricted directories have .restrict files within, 
	 * containing requirements for a USER + ACCT/PW. This function checks the Authoriser to see if that ACCT/PW is currently in use
	 * If not, a request for an ACCT/PW is made. 
	 * 
	 * This helper function assumes that if a valid user isn't logged in already, then the client doesn't have permission to enter the directory
	 * 
	 * File format must be the same as Authorisation.txt, having '#' between each field, and use ',' to separate ACCTs
	 * e.g. USER#ACCT,ACCT2#PASSWORD or USER##PASSWORD or USER#ACCT#
	 * Account and password info for a given user must match the contents of the Authorisation file, i.e. if a user X has password Y
	 * in Authorisation.txt, then if a password is specified in .restrict, it must be Y. Same principle for any specified account info.
	 * 
	 * Returns an int signifying access status: [0 = no restriction], [1 = restricted but required USER/ACCT/PW is currently active],
	 * [2 = restriction, need ACCT/PW of current user (i.e. need to change acounts)], 
	 * [3 = restricted, current user has no access], [4 = an error with the .restricted file], [5 = another error occurred]
	 */
	private int restricted(String dir){
		File r = new File(dir + "\\.restrict");
		if(r.exists()){
			String line;
			String[] info = null;
			boolean validUser = false;
			boolean validAcct = false;
			boolean validPass = false;
			try {
				BufferedReader br = new BufferedReader(new FileReader(r));
				while((line = br.readLine()) != null){
					info = line.split("#", -1);
					if(info.length == 3){ // i.e. {[USER], [ACCT(s))], [PW]}
						if(info[0].equals(Authoriser.user)){
							validUser = true;
							// check account
							if("".equals(info[1])){ // no acct required for this user
								validAcct = true;
							}
							else{
								for(String accts : info[1].split(",", -1)){
									if(accts.equals(Authoriser.account)){ // i.e. current account in use
										validAcct = true;
									}
								}
							}

							// check password
							if("".equals(info[2])){
								validPass = true;
							}
							else{
								if(info[2].equals(Authoriser.pass)){
									validPass = true;
								}
							}						
						}
						
					}
					else{
						br.close();
						return 4;
					}
				}
				br.close();
				return ((!validUser) ? 3 : ((validAcct && validPass) ? 1 : 2)); // check user: if valid, check either current account or password is wrong, if so then request, if not then all valid.
			} catch (IOException e) {
				if(Server.seeSysOutput) System.err.println(e);
				return 5;
			}
		}
		else{
			return 0; 
		}
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
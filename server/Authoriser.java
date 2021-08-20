package sftpServer;

import java.io.*;

/*
 * Authoriser validating users
 * This class handles commands for the intitial authentication 
 * i.e. USER, ACCT, and PASS
 */
 
/*
 * Valid syntax for user info in Authorisation.txt
 * Note: case sensitive
 *
 * **** IMPORTANT ****
 * IF Authorisation.txt IS LEFT EMPTY, SYSTEM IS ASSUMED TO HAVE NO USERS, 
 * SO ANY USER INPUT FROM CLIENT WILL BE VALID
 * **** IMPORTANT ****
 *
 * Can have more than one account, but not more than one password. 
 * This is because the password is only ascociated with the user, not the individual accounts.
 * There can be multiple accounts for billing purposes
 *
 * Scenario 1: user without account or password required. There must be two # after the username
 * USERNAME##
 *
 * Scenario 2: user only requires an account name. There must be a # after the last account name
 * USERNAME#ACCOUNTNAME,ACCOUNTNAME2#
 *
 * Scenario 3: user requires account and password. 
 * USERNAME#ACCOUNTNAME#PASSWORD
 *
 * SCENARIO 4: user only needs a password, need two spaces for 'split' function
 * USERNAME##PASSWORD
 */
 
public class Authoriser {
	protected static String aFile;
	protected static Boolean validUser = false;
	protected static Boolean validAcct = false;
	protected static Boolean validPass = false;
	
	protected static String user;
	protected static String acct;
	protected static String[] accts;
	protected static String pass;

	public Authoriser(String aFile){
		Authoriser.aFile = aFile;
	}
		
	// USER cmd
	public String user(String inString) throws Exception{
		File f = new File("Authorisation.txt");
		BufferedReader reader = null;
		String txt;
		String response = null;
		validUser = false;
		validAcct = false;
		validPass = false;
		
		// IN: reads in file containing authorised users 
		try{
			reader = new BufferedReader(new FileReader(f));
			
			if(f.length() == 0){ // if Authentication.txt is empty
				validAcct = true;
				validPass = true;
				response = "+";
				return response;
			}
			else {
				while((txt = reader.readLine()) != null){
					String temp = txt;
					String[] userInfo = temp.split("#", -1);
					user = userInfo[0];
					accts = userInfo[1].split(",");
					pass = userInfo[2];
					
					// if the input user if found in the file then break searching loop
					if(user.equals(inString)){
						validUser = true;
						break;
					}
				}
			}
		}
		catch (FileNotFoundException e){
			System.out.println("@USER file not found in authoriser class");
		}
		catch (IOException e) {
			System.out.println("@USER IO exception in authoriser class");
		}
		finally{
			try {
				if(reader != null){
					reader.close();
				}
			}
			catch (IOException e){
				System.out.println("@USER, IO exception on reader close");
			}
		}
		
		// OUT: user response + processing for if USER requires an account / password. 
		// Note: a password is associated with a user, so only one is allowed
		// First check if a valid username has been received, if there has been, then check if an account/password is required for that user
		if(!validUser){
			return "-Invalid user-id, try again";
		}
		else {
			if((accts[0].equals("") && accts.length <= 1) || accts.length <= 0){ // no account found
				validAcct = true;
			}
			if(pass.length() == 0){
				validPass = true;
			}
			
			//response
			if(validAcct && validPass){ // when neither password or account is required
				response = "!"+user+" logged in";
			}
			else if(!validAcct && !validPass){ // if an account, password, or both are required
				response = "+"+user+" valid, send account and password";
			}
			else if(!validAcct && validPass){
				response = "+"+user+" valid, send account";
			}
			else if(validAcct && !validPass){
				response = "+"+user+" valid, send password";
			}
		}
		return response;
	}
	
	/* 
	 * ACCT CMD
	 * assumes a current valid user command active in remote system 
	 */
	public String acct(String inString) throws Exception {
		String response = null;
		if(loggedIn()){
			response = "-Already logged in";
		}
		else if(!validUser){
			response = "-Cannot validate account as no USER given";
		}
		else{
			// if there's no account required for the user
			if(validAcct && !validPass){
				response = "+"+"Account not required, send password";
			}
			else if(validAcct && validPass){
				response = "! Already logged in";
			}
			else{
				for(String acctname: accts){
					if(inString.equals(acctname)){
						validAcct = true;
						if(validPass){
							response = "! Account valid, logged-in";
						}
						else {
							response = "+Account valid, send password";
						}
						break;
					}
				}
				if(!validAcct){
					response = "-Invalid account, try again";
				}				
			}
		}
		return response;
	}
	
	/* 
	 * PASS CMD
	 * assumes a current valid user command active in remote system 
	 */
	public String pass(String inString) throws Exception {
		String response = null;
		if(loggedIn()){
			response = "-Already logged in";
		}
		else if(!validUser){
			response = "-Cannot validate password as no USER given";
		}
		else{
			if(!validAcct && validPass){
				response = "+"+"Password not required, send account";
			}
			else if(validAcct && validPass){
				response = "! Already logged in";
			}
			else{
				if(inString.equals(pass)){
					validPass = true;
					if(validAcct){
						response = "! Logged in";
					}
					else {
						response = "+Send Account";
					}
				}
				else {
					response = "-Wrong password, try again";
				}
			}
		}
		return response;
	}
	
	// Helper function intended for Authoriser and ServerInstance
	public boolean loggedIn(){
		return validUser && validAcct && validPass;
	}
}

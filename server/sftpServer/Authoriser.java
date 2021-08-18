import java.io.*;

/*
 * Authoriser validating users
 * This class handles commands for the intitial authentication 
 * i.e. USER, ACCT, and PASS
 */
 
/*
 * Valid syntax for user info in Authorisation.txt
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
 *
 */
 
public class Authoriser {
	protected static String aFile;
	protected static Boolean validUser = false;
	protected static Boolean validAcct = false;
	protected static Boolean validPass = false;
	
	protected static String user;
	protected static String acct = "";
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
					accts = userInfo[1].split(",", -1);
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
			if(validAcct && validAcct){ // when neither password or account is required
				response = "!"+user+" logged in";
			}
			else { // if an account, password, or both are required
				response = "+"+user+" valid, send account and password";
			}
		}
		return response;
	}
	
	public String acct(String inString) throws Exception {
		
		
		
	}
	
	
	public String pass(String inString) throws Exception {
		
		
		
	}
	
}
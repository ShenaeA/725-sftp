import java.io.*;

/*
 * Authoriser validating users
 * This class handles commands for the intitial authentication 
 * i.e. USER, ACCT, and PASS
 */
 
/*
 * Valid syntax for user infor in Authorisation.txt
 * Scenario 1: user without account or password required
 * USERNAME
 *
 * Scenario 2: user only requires an account name
 * USERNAME ACCOUNTNAME|ACCOUNTNAME2
 *
 * Scenario 3: user requires account and password
 * USERNAME ACCOUNTNAME 
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
		
		
		// reads in file containing authorised users 
		try{
			reader = new BufferedReader(new FileReader(f));
			
			while((txt = reader.readLine()) != null){
				String temp = txt;
				String[] userInfo = temp.split(" ", -1);
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
		catch (FileNotFoundException e){
			if(Server.DEBUG) System.out.println("@USER file not found in authoriser class");
		}
		catch (IOException e) {
			if(Server.DEBUG) System.out.println("@USER IO exception in authoriser class");
		}
		finally{
			try {
				if(reader != null){
					reader.close();
				}
			}
			catch (IOException e){
				if(Server.DEBUG) System.out.println("@USER, IO exception on reader close");
			}
		}
		
	
	}
	
	public String acct(String inString) throws Exception {
		
		
		
	}
	
	
	public String pass(String inString) throws Exception {
		
		
		
	}
	
}
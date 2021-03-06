# README CS725 Assignment One
https://github.com/ShenaeA/725-sftp
## Table of Contents
1. [Introduction](#introduction)
2. [Components and Set Ups](#components-and-set-ups)
   1. [SFTPServer](#sftpserver)
   2. [SFTPClient](#sftpclient)
   3. [Authorisation File](#authorisation-file)
   4. [Restricted folders](restricted-folders)
3. [Command Guide and Example Test or Use Cases](#command-guide-and-example-test-or-use-cases)
   1. [USER, ACCT and PASS Commands](#user-acct-and-pass-commands)
   2. [TYPE Command](#type-command)
   3. [LIST Command](#list-command)
   4. [CDIR Command](#cdir-command)
   5. [KILL Command](#kill-command)
   6. [NAME Command](#name-command)
   7. [TOBE Command](#tobe-command)
   8. [DONE Command](#done-command)
   9. [RETR Command, and SEND or STOP](#retr-command-and-send-or-stop)
   10. [STOR Command](#stor-command)

# Introduction
This is a java implementation of the RFC 913 simple file transfer protocol. Additional features have been added, like restricted folder capabilities.

# Components and Set Ups
The folder structure and naming of 725-sftp, client and server must not be renamed, as well as ftFolder after it generates.
## Server
Preface: make sure the authorisation file is set-up first. The default file will work, however, if you desire different users/accounts/passwords, then you'll need to edit as per the "[Authorisation.txt](#authorisation-file)" file instructions.

To run the Server (Command Prompt or PowerShell):
1. Open command prompt and navigate to ../725-sftp/server and compile using "javac *.java" 
2. Run with "java Server \[authorisation file name]"
   a. the default name of the authorisation file is Authorisation.txt
   b. if you've changed the file name then use that name instead
   c. the authorisation file must be in the '725-sftp/server/' folder

## Client
Preface: this is the client used to gain access to the remote host/SFTP server. Currently the default port the client is connected to is port 9999, using "localhost' as the IP. If the connection isn't made there will be a response "Connection not made. Server may be inactive.", so make sure the server is running first. The client must be stored in the /client folder, as that is also where the storing of file transfers to client goes.  

To run the Client:
1. Open command prompt and navigate to ../725-sftp/client and compile using "javac *.java"
2. Run with "java Client"

## Authorisation File
Valid syntax for user info in Authorisation.txt
Note: case sensitive and spaces are not permitted. If file is left blank, it is assumed any USER command is valid for login

IF Authorisation.txt IS LEFT EMPTY, SYSTEM IS ASSUMED TO HAVE NO USERS, SO ANY USER INPUT FROM CLIENT WILL BE VALID

Can have more than one account, but not more than one password. 
This is because the password is only associated with the user, not the individual accounts.
There can be multiple accounts for billing purposes.

Scenario 1: user without account or password required. There must be two # after the username 
USERNAME##

Scenario 2: user only requires an account name. There must be a # after the last account name USERNAME#ACCOUNTNAME,ACCOUNTNAME2#

Scenario 3: user requires account and password. 
USERNAME#ACCOUNTNAME#PASSWORD

Scenario 4: user only needs a password, need two spaces for 'split' function
USERNAME##PASSWORD

## Restricted folders
Restricted folders exist when a ```.restrict``` file exists within the folder.
File format must be the same as Authorisation.txt, having '#' between each field, and use ',' to separate ACCTs, e.g. USER#ACCT,ACCT2#PASSWORD or USER##PASSWORD or USER#ACCT#
Account and password info for a given user within the ```.restrict``` must match the contents of the Authorisation file, i.e. if a user X has password Y in Authorisation.txt, then if a password is specified in .restrict, it must be Y. Same principle for any specified account info. Cannot have users in ```.restrict``` that do not exists in the authoriser file. 

# Command Guide and Example Test or Use Cases
The client receive responses from the server beginning with either a ! + or -.
The '>' indicates where the client has made an input. 

## USER, ACCT and PASS Commands
After starting up the client, the first thing that is required is logging in. 
Authorisation/permissions are set using the authorisation file.
Depending on the contents the user (client) may need to invoke a USER/ACCT/PASS command and input information. When the authorisation file is empty, the user still needs to enter a user-id. It can be anything as long as there is no spaces or hashtags.

USER command can be reused over and over, and typing user again will cause the client to log out.

Format: USER user-id
Format: ACCT account
Format: PASS password

### Empty authorisation file
hen the authorisation file is empty, the user still needs to enter a user-id. It can be anything as long as there is no spaces or hashtags.
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER myname
+
```

The examples for USER, ACCT, and PASS use the following Authorisation.txt:
```
JUSTUSER##
JUSTACCT#ACCTNAME#
TWOACCT#ACCT1,ACCT2#
JUSTPASS##password1
ALL#ACCTNAME1,ACCTNAME2,ACCTNAME3#password2
```

### JUSTUSER
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTUSER
!JUSTUSER logged in
```
### TWOACCT
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER TWOACCT 
+TWOACCT valid, send account
> ACCT ACCT2
! Account valid, logged-in
```
### JUSTPASS
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTPASS
+JUSTPASS valid, send password
> PASS password1
! Logged in
```
### ALL
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER ALL
+ALL valid, send account and password
> ACCT ACCTNAME2
+Account valid, send password
> PASS password2
! Logged in
```
### Changing User
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTUSER
!JUSTUSER logged in
> TYPE A
+Using Ascii mode
> USER TWOACCT
+TWOACCT valid, send account
> TYPE B
-Command not available, please log in first.
> ACCT ACCT1
! Account valid, logged-in
> TYPE B
+Using Binary mode
```
### Error Cases
#### Login
``` 
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER SOMETEXT
-Invalid user-id, try again
> TYPE A
-Command not available, please log in first.
> USER ALL    
+ALL valid, send account and password
> TYPE A   
-Command not available, please log in first.
> ACCT ACCTNAME1
+Account valid, send password
> TYPE A
-Command not available, please log in first.
> PASS password2
! Logged in
> TYPE A
+Using Ascii mode
```
#### Incorrect Account/Password 1
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER ALL
+ALL valid, send account and password
> ACCT SOMEACCOUNT
-Invalid account, try again
> ACCT ACCTNAME1
+Account valid, send password
> PASS PASSSSSS
-Wrong password, try again
> PASS password2
! Logged in
```
#### Incorrect Account/Password 2
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER ALL
+ALL valid, send account and password
> PASS PASSSSS
-Wrong password, try again
> PASS password2
+Send Account
> ACCT ACCCCCTTTT
-Invalid account, try again
> PASS PASSSS
+Password not required, send account
> ACCT ACCTNAME1
! Account valid, logged-in
> PASS PASSSS
-Already logged in
```

## TYPE Command
The TYPE command changes the expected mapping type for files being sent and received. 
The default is 'B' or binary. Options are "A" for ASCII, "B' for binary, or "C" for continuous (also a binary formatting).
Current implementation does not check input file type before send/receive, so will break if the incorrect type is selected before using commands like RETR or STOR.

Format: TYPE { A | B | C }

```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER ALL
+ALL valid, send account and password
> ACCT ACCTNAME2
+Account valid, send password
> PASS password2
! Logged in
> TYPE A
+Using Ascii mode
> TYPE B
+Using Binary mode
> TYPE C
+Using Continuous mode
```

## LIST Command
The LIST command outputs the current working directory in one of two formats, "F" being standard formatting, and "V" being a verbose directory listing.
Format: LIST { F | V } directory-path

### General Use Case
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTUSER
!JUSTUSER logged in
> CDIR /client
!Changed working directory to /client
> LIST F
+\
Capture.PNG
Capture1.PNG
Capture2.PNG
Client.java
txt1.txt

> LIST V
+\
|Name                                                           |Size (kB)|R/W|Last Modified             |
|Capture.PNG                                                    |99       |R/W|31/08/21, 1:07:39 AM NZST |
|Capture1.PNG                                                   |0        |R/W|1/09/21, 3:01:47 AM NZST  |
|Capture2.PNG                                                   |7        |R/W|1/09/21, 3:49:58 AM NZST  |
|Client.java                                                    |22       |R/W|1/09/21, 4:17:44 AM NZST  |
|txt1.txt                                                       |0        |R/W|1/09/21, 12:35:13 AM NZST |

```

### Permissions Error Case
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTACCT
+JUSTACCT valid, send account
> ACCT ACCTNAME
! Account valid, logged-in
> CDIR server/sft/new folder/folder
-Cannot connect to /server/sft/new folder/folder because: current user does not have permission to access
> CDIR server/sft/new folder
!Changed working directory to /server/sft/new folder
> LIST F /folder
-Cannot give a listing for a directory the current user does not have permission to access
> LIST F        
+
folder

> LIST F /
+/
folder

> LIST V /folder 
-Cannot give a listing for a directory the current user does not have permission to access
```

## CDIR Command
The CDIR function will change the current working directory on the remote host to the argument passed.
To get to the root folder use ```CDIR /```.
Format: CDIR new-directory

### Example
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTACCT
+JUSTACCT valid, send account
> ACCT ACCTNAME
! Account valid, logged-in
> CDIR server/sft/new folder/folder
-Cannot connect to /server/sft/new folder/folder because: current user does not have permission to access
> CDIR server/sft/new folder
!Changed working directory to /server/sft/new folder
> CDIR /directory doesnt exist
-Can't connect to directory because: input is not a valid directory
```

### Permissions Example
The "+directory ok, send account/password" response will only occur if the directory being accessed is restricted.

For the example .restrict file contents are:
```
TWOACCT#ACCT2#
JUSTUSER##
```

```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER TWOACCT
+TWOACCT valid, send account
> ACCT ACCT1
! Account valid, logged-in
> CDIR server/sft/new folder/folder
+Directory ok, send account
> ACCT ACCT1
-Account exists but does not have permissions to enter this directory. Aced to ACCT1 and current directory is C:\root\725-sftp
> CDIR server/sft/new folder/folder
+Directory ok, send account
> ACCT ACCT2
+Account valid, account changed to ACCT2
!Changed working directory to /server/sft/new folder/folder
```
## KILL Command
The KILL command deletes a file from the current working directory.
Format: KILL file-spec

### General Use Case Example 
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTACCT
+JUSTACCT valid, send account
> ACCT ACCTNAME
! Account valid, logged-in
> CDIR /
!Changed working directory to /
> LIST F
+
.git
.gitignore
.vscode
client
example-cases
README.md
server
txt.txt
TXT1.txt

> KILL txt.txt
+txt.txt deleted
> LIST F
+
.git
.gitignore
.vscode
client
example-cases
README.md
server
TXT1.txt

>
```

### Exists Example
Continuing off the previous example:
```
> LIST F
+
.git
.gitignore
.vscode
client
example-cases
README.md
server
TXT1.txt

>
> KILL txt.txt
-Not deleted because file does not exist. Note: case sensitive
```

### Permissions Example
The user 'JUSTACCT' doesn't have permission to enter ```/server/sft/new folder/folder```, whereas 'JUSTUSER' does
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTACCT 
+JUSTACCT valid, send account
> ACCT ACCTNAME
! Account valid, logged-in
> CDIR server/sft/new folder/folder
-Cannot connect to /server/sft/new folder/folder because: current user does not have permission to access
> CDIR server/sft/new folder/      
!Changed working directory to /server/sft/new folder/
> KILL folder/TXT.txt
-Not deleted because current user does not have permission to access to that directory. File existence unknown
> USER JUSTUSER
!JUSTUSER logged in
> CDIR server/sft/new folder/folder
!Changed working directory to /server/sft/new folder/folder
> LIST F
+
.restrict
TXT.txt

>
```

### Case Sensitivity + General Usage Example
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTACCT 
+JUSTACCT valid, send account
> ACCT ACCTNAME
! Account valid, logged-in
> CDIR server/sft/new folder/      
!Changed working directory to /server/sft/new folder/
> LIST F
+\
folder
New Text Document.txt

> KILL new text document.txt
-Not deleted because file does not exist. Note: case sensitive
> KILL New Text Document.txt 
+New Text Document.txt deleted
> LIST F
+
folder

>
> KILL New Text Document.txt
-Not deleted because file does not exist
```

## NAME Command
The NAME command renames a file. This function includes renaming the extention, so caution is advised.
Format: NAME old-file-spec

### General Use Case
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTACCT
+JUSTACCT valid, send account
> ACCT ACCTNAME
! Account valid, logged-in
> LIST F
+
.git
.gitignore
.vscode
client
example-cases
README.md
server
txt.txt
TXT1.txt

> NAME txt.txt
+File exists, send TOBE command with file's new name
> TOBE changeExtention.doc
+txt.txt renamed to changeExtention.doc
> LIST F
+
.git
.gitignore
.vscode
changeExtention.doc
client
example-cases
README.md
server
TXT1.txt

>
```
### Permissions example
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTUSER
!JUSTUSER logged in
> CDIR server/sft/new folder/folder
!Changed working directory to /server/sft/new folder/folder
> LIST F
+
.restrict
TXT.txt

> CDIR /
!Changed working directory to /
> USER JUSTACCT
+JUSTACCT valid, send account
> ACCT ACCTNAME
! Account valid, logged-in
> CDIR server/sft/new folder/folder
-Cannot connect to /server/sft/new folder/folder because: current user does not have permission to access
> CDIR server/sft/new folder
!Changed working directory to /server/sft/new folder
> NAME folder/TXT.txt
-Cannot rename because current user does not have permission to access to that directory. File existence unknown
> NAME \folder\TXT.txt
-Cannot rename because current user does not have permission to access to that directory. File existence unknown
> TOBE folder\TXT.txt
-ERROR: send NAME cmd first
```
## TOBE Command
The TOBE command follows the NAME command to rename a file
Format: TOBE new-file-spec

### NAME + TOBE Example
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTUSER
!JUSTUSER logged in
> CDIR server/sft
!Changed working directory to /server/sft
> LIST F
+\
New Compressed (zipped) Folder.zip
new folder
New Microsoft PowerPoint Presentation.pptx
txt.txt

> NAME txt.txt
+File exists, send TOBE command with file's new name
> CDIR server/   
!Changed working directory to /server/
> TOBE txt1.txt
-File wasn't renamed because there has been a change in current directory since last NAME command. Current directory is C:\root\725-sftp\server. Please restart renaming process.
> CDIR server/sft
!Changed working directory to /server/sft
> NAME txt.txt
+File exists, send TOBE command with file's new name
> TOBE txt1.txt
+txt.txt renamed to txt1.txt
> LIST F
+
Capture.PNG
New Compressed (zipped) Folder.zip
new folder
New Microsoft PowerPoint Presentation.pptx
txt1.txt

>
> TOBE txt1.txt   
-ERROR: send NAME cmd first
> NAME txt.txt  
-Invalid file name. Is not a file in this directory
```

## DONE Command
The DONE command communicates to the server that the client wants to close the connection. The socket is then closed on both the client and server sides respectively, and the respective thread on the server side ends. Format: DONE

### Example
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTUSER
!JUSTUSER logged in
> TYPE A
+Using Ascii mode
> DONE
+Finishing command received. Closing connection...
PS C:\Users\Shenae\uni\CS725\Assignments\A1\725-sftp\client> java Client
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> TYPE A
-Command not available, please log in first.
```

## RETR Command, and SEND or STOP
Requests that the remote system then uses the SEND to send the specified file.
Not permitted: making requests for files in the /client folder.
SEND or STP command follows the RETR to tell the remote host the client wishes to continue with the request, and to send the file/stop the RETR process.
If a file already exists in the /client folder with the same name, it will be overwritten.

This function assumes the correct TYPE command has been entered before starting. If sending a binary formatted file for example, PNG files, need to send: ```TYPE B``` or ```TYPE C``` first. For text or ASCII formatted files send: ```TYPE A```.

Format: RETR file-spec

### General Use case
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTUSER    
!JUSTUSER logged in
> CDIR client
!Changed working directory to /client
> LIST F
+
Capture1.PNG
Capture2.PNG
Client.class
Client.java
txt.txt

>
> TYPE B
+Using Binary mode
> CDIR server/sft
!Changed working directory to /server/sft
> LIST F
+
Capture.PNG
New Compressed (zipped) Folder.zip
new folder
New Microsoft PowerPoint Presentation.pptx
txt.txt

> RETR Capture.PNG
3469
Input either a SEND to receive file or STOP command to stop receiving process
> SEND
File saved at C:\root\725-sftp\client\Capture.PNG
> CDIR client
!Changed working directory to /client
> LIST V
+
|Name                                                           |Size (kB)|R/W|Last Modified             |
|Capture.PNG                                                    |3        |R/W|1/09/21, 8:26:20 PM NZST  |
|Capture1.PNG                                                   |0        |R/W|1/09/21, 3:01:47 AM NZST  |
|Capture2.PNG                                                   |7        |R/W|1/09/21, 3:49:58 AM NZST  |
|Client.class                                                   |13       |R/W|1/09/21, 8:20:00 PM NZST  |
|Client.java                                                    |26       |R/W|1/09/21, 8:15:37 PM NZST  |
|txt.txt                                                        |0        |R/W|1/09/21, 5:38:30 PM NZST  |

>
```

### Cannot Retrieve from Client Folder Example
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTUSER
!JUSTUSER logged in
> CDIR CLIENT
!Changed working directory to /CLIENT
> RETR TXT1.txt         
-Invalid directory, this is the destination folder, any file you're requesting from here is already there
```

### Permission Example
The JUSTACCT user doesn't have permission as per the .restrict file within /folder
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTACCT                     
+JUSTACCT valid, send account
> ACCT ACCTNAME
! Account valid, logged-in
> CDIR server/sft/new folder
!Changed working directory to /server/sft/new folder
> RETR /folder/.restrict            
-ERROR: cannot make requests for file(s) in that directory, current user does not have permission to access to that directory
```

### Error Case
KILL command removes the file to be sent, causes the socket to close on server side and connection to be lost
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTUSER 
!JUSTUSER logged in
> CDIR server/sft
!Changed working directory to /server/sft
> LIST F
+
Capture.PNG
New Compressed (zipped) Folder.zip
new folder
New Microsoft PowerPoint Presentation.pptx     
txt.txt

> TYPE a
+Using Ascii mode
> RETR txt.txt
24
Input either a SEND to receive file or STOP command to stop receiving process
> KILL txt.txt
+txt.txt deleted
> SEND
> CDIR /
ERROR: Cannot send command. Connection to server closed.
```

### SEND Usage Error Case
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER TWOACCT 
+TWOACCT valid, send account
> ACCT ACCT1
! Account valid, logged-in
> TYPE A       
+Using Ascii mode
> RETR txt.txt     
9
Input either a SEND to receive file or STOP command to stop receiving process
> SEND
> SEND
ERROR: need valid RETR command before SEND can be used
```
### Conflicting Input File and Send Types Example
The client outputs and error.
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTUSER
!JUSTUSER logged in
> TYPE A
+Using Ascii mode
> CDIR server/sft
!Changed working directory to /server/sft
> LIST F
+
Capture.PNG
New Compressed (zipped) Folder.zip
new folder
New Microsoft PowerPoint Presentation.pptx
txt.txt

> RETR Capture.PNG
ERROR: conflicting send type and input file type. Send type is A and file type is B or C
```  

## STOR Command
Tells the remote system to receive the following file and save it under that name in /server/ftFolder (file transfer folder).
Format: { NEW | OLD | APP } file-spec
"NEW" means a new file should be generated, "OLD" means it should overwrite the current file, and "APP" is that it should append the existing file.

This function assumes the correct TYPE command has been entered before starting. If sending a binary formatted file for example, PNG files, need to send: ```TYPE B``` or ```TYPE C``` first. For text or ASCII formatted files send: ```TYPE A```.

### NEW Example
The example shows a typical use case, and also how it shall rename the transfer file if a file already exists with that name
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTUSER
!JUSTUSER logged in
> CDIR server/ftFolder
!Changed working directory to /server/ftFolder
> LIST F
+
Capture2.PNG

> CDIR client 
!Changed working directory to /client
> LIST F   
+
Authorisation.txt
Capture.PNG
Capture1.PNG
Capture2.PNG
Client.java
txt.txt

> TYPE A
+Using Ascii mode
> STOR NEW txt.txt  
+File exists, will create new generation of file
Validating that there is space for file. File to be sent size is 19 bytes
+Ok, waiting for file
Sending...
+Saved txt.txt
> CDIR server/ftFolder
!Changed working directory to /server/ftFolder
> LIST F
+
Capture2.PNG
txt.txt

> CDIR client          
!Changed working directory to /client
> STOR NEW txt.txt  
+File exists, will create new generation of file
Validating that there is space for file. File to be sent size is 19 bytes
+Ok, waiting for file
Sending...
+Saved txt-20210901062131.txt
> CDIR server/ftFolder
!Changed working directory to /server/ftFolder
> LIST F
+
Capture2.PNG
txt-20210901062131.txt
txt.txt
```

### OLD Example
This example continues on from the previous one (NEW):
```
> CDIR client
!Changed working directory to /client
> STOR OLD txt.txt
+Will write over old file
Validating that there is space for file. File to be sent size is 25 bytes
+Ok, waiting for file
Sending...
+Saved txt.txt
```

### APP Example
This example continues on from the previous one (OLD), so the final txt.txt size is 50 bytes.
i.e. 19 bytes overwritten by 25 butes, then appended by another 25 bytes = 50 bytes.
```
> STOR APP txt.txt 
+Will append to file
Validating that there is space for file. File to be sent size is 25 bytes
+Ok, waiting for file
Sending...
+Saved txt.txt
> LIST V
+
|Name                                                           |Size (kB)|R/W|Last Modified             |
|Authorisation.txt                                              |0        |R/W|1/09/21, 5:02:24 AM NZST  |
|Capture.PNG                                                    |3        |R/W|1/09/21, 8:26:20 PM NZST  |
|Capture1.PNG                                                   |0        |R/W|1/09/21, 3:01:47 AM NZST  |
|Capture2.PNG                                                   |7        |R/W|1/09/21, 3:49:58 AM NZST  |
|Client.java                                                    |26       |R/W|1/09/21, 8:48:34 PM NZST  |
|txt.txt                                                        |0        |R/W|1/09/21, 8:50:37 PM NZST  |
```

### Error case
Continuing from previous example:
```
> STOR APP txt1.txt 
ERROR: File txt1.txt does not exist in pathing: C:\root\725-sftp\client
```
This is an output from the client, not the remote host

### Cannot Retrieve from server/ftFolder Folder Example
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTUSER        
!JUSTUSER logged in
> CDIR server/ftFolder
!Changed working directory to /server/ftFolder
> STOR NEW Capture.PNG
-Invalid directory, this is the destination folder, any file you're requesting from here is already there
```

### Conflicting Input File and Send Types Example
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTUSER        
!JUSTUSER logged in
> CDIR client
!Changed working directory to /client
> TYPE A
+Using Ascii mode
> STOR NEW Capture.PNG
ERROR: conflicting send type and input file type. Send type is A and file type is B or C
> CDIR server
!Changed working directory to /server
> SEND 
ERROR: need valid RETR command before SEND can be used
```


# 725-sftp
## Table of Contents
1. [Introduction](#introduction)
2. [Elements](#elements)
   1. [SFTPServer](#sftpserver)
   2. [SFTPClient](#sftpclient)
   3. [Authorisation.txt](#authorisation.txt)
   4. [Restricted folders](restricted-folders)
3. [Set-ups](#setup)
   1. [Server](#server-setup)
   2. [Client](#client-setup)
   3. [Authorisation.txt](#authorisation.txt)
   4. [Restricted folders](restricted-folders)
4. [Command Guide](#command-guide)
   1. [USER, ACCT and PASS Commands](#user-acct-and-pass-commands)
   2. [TYPE Command](#type-command)
   3. [LIST Command](#list-command)
   4. [CDIR Command](#cdir-command)
   5. [KILL Command](#kill-command)
   6. [NAME Command](#name-command)
   7. [TOBE Command](#tobe-command)
   8. [DONE Command](#done-command)
   9. [RETR Command](#retr-command)
   10. [STOR Command](#stor-command)
5. [Use Cases](#use-cases)
   1. [Example 1](#example-1)
   2. [Example 2](#example-2)
   3. [Example 3](#example-3)


# Introduction


# Elements

# Set-ups
## Server
Preface: make sure the authorisation file is set-up first. The default file will work, however, if you desire different users/accounts/passwords, then you'll need to edit as per the "[Authorisation.txt](#authorisiation.txt)" file instructions.
To run the Server (Command Prompt or PowerShell):
1. Open ../725-sftp/server and compile using "javac *.java" 
2. Run with "java Server \[authorisation file name]"
   a. the default name of the authorisation file is Authorisation.txt
   b. if you've changed the file name then use that name instead
   c. the authorisation file must be in the '725-sftp/server/' folder

## Client



## Authorisation.txt
Valid syntax for user info in Authorisation.txt
Note: case sensitive

IF Authorisation.txt IS LEFT EMPTY, SYSTEM IS ASSUMED TO HAVE NO USERS, SO ANY USER INPUT FROM CLIENT WILL BE VALID

Can have more than one account, but not more than one password. 
This is because the password is only ascociated with the user, not the individual accounts.
There can be multiple accounts for billing purposes.

Scenario 1: user without account or password required. There must be two # after the username 
USERNAME##

Scenario 2: user only requires an account name. There must be a # after the last account name USERNAME#ACCOUNTNAME,ACCOUNTNAME2#

Scenario 3: user requires account and password. 
USERNAME#ACCOUNTNAME#PASSWORD

SCENARIO 4: user only needs a password, need two spaces for 'split' function
USERNAME##PASSWORD

## Restricted folders
File format must be the same as Authorisation.txt, having '#' between each field, and use ',' to separate ACCTs 
e.g. USER#ACCT,ACCT2#PASSWORD or USER##PASSWORD or USER#ACCT#
Account and password info for a given user must match the contents of the Authorisation file, i.e. if a user X has password Y in Authorisation.txt, then if a password is specified in .restrict, it must be Y. Same principle for any specified account info.

# Command Guide
## USER, ACCT and PASS Commands


## TYPE Command


## LIST Command


## CDIR Command


## KILL Command

### Permissions Example
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTACCT 
+JUSTACCT valid, send account
> ACCT ACCTNAME
! Account valid, logged-in
> CDIR server/sft/new folder/folder
-Cannot connect to /server/sft/new folder/folder because: current user does not have permission to access
> KILL folder/TXT.txt
-Not deleted because file does not exist
> CDIR server/sft/new folder/      
!Changed working directory to /server/sft/new folder/
> KILL folder/TXT.txt
-Not deleted because current user does not have permission to access to that directory. File existence unknown
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
> KILL New Text Document.txt
-Not deleted because file does not exist
```


## NAME Command


### Permissions example
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
> LIST F
+\
folder
New Text Document.txt

> NAME folder/TXT.txt
-Not deleted because current user does not have permission to access to that directory. File existence unknown
> NAME folder\TXT.txt 
-Not deleted because current user does not have permission to access to that directory. File existence unknown
> TOBE folder\txt1.txt
-ERROR: send NAME cmd first
```
## TOBE Command


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
-File wasn't renamed because there has been a change in current directory since last NAME command. Current directory is C:\Users\Shenae\uni\CS725\Assignments\A1\725-sftp\server. Please restart renaming process.
> CDIR server/sft
!Changed working directory to /server/sft
> NAME txt.txt
+File exists, send TOBE command with file's new name
> TOBE txt1.txt
+txt.txt renamed to txt1.txt
> TOBE txt1.txt   
-ERROR: send NAME cmd first
> NAME txt.txt  
-Invalid file name. Is not a file in this directory
```

## DONE Command
The DONE command communicates to the server that the client wants to close the connection. The socket is then closed on both the client and server sides respoctively, and the respective thread on the server side ends.

### Example
```
Connected to localhost via port number 9999
+SFTP RFC913 Server Activated :)
> USER JUSTUSER   
!JUSTUSER logged in
> DONE
+Finishing command received. Closing connection...
```


## RETR Command


## STOR Command


# Use Cases
## Example 1


## Example 2


## Example 3


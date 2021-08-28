# 725-sftp
## Table of Contents
1. [Introduction](#introduction)
2. [Elements](#elements)
   1. [SFTPServer](#sftpserver)
   2. [SFTPClient](#sftpclient)
   3. [Required Files](#required-files)
   4. [Folder](#folders)
3. [Set-ups](#setup)
   1. [Server](#server-setup)
   2. [Client](#client-setup)
4. [Command Guide](#command-guide)
   1. [USER, ACCT and PASS Commands](#user-acct-and-pass-commands)
   2. [TYPE Command](#type-command)
   3. [LIST Command](#list-command)
   4. [CDIR Command](#cdir-command)
   5. [KILL Command](#kill-command)
   6. [NAME Command](#name-command)
   7. [DONE Command](#done-command)
   8. [RETR Command](#retr-command)
   9. [STOR Command](#stor-command)
5. [Use Cases](#use-cases)
   1. [Example 1](#example-1)
   2. [Example 2](#example-2)
   3. [Example 3](#example-3)


# Introduction


# Elements
## Server
Preface: make sure the authorisation file is set-up first. The default file will work, however, if you desire different users/accounts/passwords, then you'll need to edit as per the "[Authorisation.txt](#authorisiation.txt)" file instructions.
To run the Server (Command Prompt or PowerShell):
1. Open ../725-sftp/server and compile using "javac *.java" 
2. Run with "java Server \[authorisation file name]"
   a. the default name of the authorisation file is Authorisation.txt
   b. if you've changed the file name then use that name instead
   c. the authorisation file must be in the '725-sftp/server/' folder

## Client


## Required Files 
### Authorisation.txt
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

## Folders
### Restricted folders
File format must be the same as Authorisation.txt, having '#' between each field, and use ',' to separate ACCTs 
e.g. USER#ACCT,ACCT2#PASSWORD or USER##PASSWORD or USER#ACCT#
Account and password info for a given user must match the contents of the Authorisation file, i.e. if a user X has password Y in Authorisation.txt, then if a password is specified in .restrict, it must be Y. Same principle for any specified account info.

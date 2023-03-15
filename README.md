# CS6378 - Advanced Operating Systems Project 2
# Distributed system mutual exclusion
## Description
This program is about implementing the Ricart-Agrawala algorithm for distributed mutual exclusion, with the optimization proposed by Roucairol and Carvalho, in a client-server model.
## Getting Started
### Dependencies
- If you are out of campus, you are required to establish VPN connection with UTD first.
- Please use one of dc, here we use dc30, machine to execute program
### Executing Program
1. Go to dc30 machine, and go to the directory.
2. Enter "bash init.sh" to initiate the program
3. At the beginning, you are required to compile the java file by selecting mode "0".
4. Then you can choose server or client mode to execute.
5. For Server, you need to activate from server 0 to 2. You cannot activate more than 3 servers.
6. For Client, you need to activate from client 3 to 7. You must activate the clientID between 3 and 7. 
### Server side supported command
- start
  - Delete previous folder and data.
  - Create a whole new folder and file.
- setup
  - Establish socket connection with neighbor servers.
- check
  - You can check the socket that connect to current server.
- test
  - Sending a test message to every socket that connect to you. 
- exit
  - End the program.
### Client side supported command
- setup
  - Establish socket connection to all servers.
- check
  - Check the server socket connection status.
- send
  - Randomly choose a server to sent local timestamp
  - Timestamp format: <clientID, HH.MM.SS.XXXXXX>
- enquiry
  - Randomly choose a server to query the list of host file.
- exit
  - End the program
## Authors
- Name: Chun Hao (William), Chang
- NetID: cxc200006
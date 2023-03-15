
# Ricart-Agrawala algorithm for distributed mutual exclusion

This program is about implementing the Ricart-Agrawala algorithm for distributed mutual exclusion, with the optimization proposed by Roucairol and Carvalho, in a client-server model.

## Description
- There are three servers in the system, numbered from zero to two.
- There are five clients in the system, numbered from zero to four.
- Assume that each file is replicated on all the servers, and all replicas of a file are consistent in the beginning.
- A client can perform `WRITE` operations on files. The client  randomly select one of the three servers and send the `WRITE` request to that server.
- The server, acting as the proxy for the client, then broadcasts the `WRITE` request to all other servers.
- Once the `WRITE` is performed (i.e., the proxy server exits the critical section), the proxy server sends a message to the client informing it of the completion of the `WRITE`.
- A client issues only one `WRITE` request at a time.
- Different servers (acting as proxies for different clients) can concurrently perform `WRITE` on different files.

```lua
           +----------------+        +----------------+        +----------------+
           |     Server 0   |        |    Server 1    |        |     Server 2   |
           +----------------+        +----------------+        +----------------+
           |  Replica of F1 |        | Replica of F1  |        | Replica of F1  |
           |  Replica of F2 |        | Replica of F2  |        | Replica of F2  |
           |  Replica of F3 |        | Replica of F3  |        | Replica of F3  |
           +----------------+        +----------------+        +----------------+
                    |                         |                         |
                    |                         |                         |
                    |                         |                         |
                    |         WRITE           |         WRITE           |
                    |------------------------>|------------------------>|
                    |  Broadcast WRITE to all servers                   |
                    |<------------------------|<------------------------|
                    |                         |                         |
                    |                         |                         |
                    |                         |                         |
                    |    Inform client of     |    Inform client of     |
                    |    completion of WRITE  |    completion of WRITE  |
           +----------------+        +----------------+        +----------------+
           |     Client 0   |        |    Client 1    |        |     Client 2   |
           +----------------+        +----------------+        +----------------+

```




## Run Locally

Start the program

```bash
  bash init.sh
```

At the beginning, you are required to compile the java file by selecting mode
```bash
0
```

Then you can choose server or client mode to execute.

- For Server, you need to activate from server 0 to 2. You cannot activate more than 3 servers.

- For Client, you need to activate from client 3 to 7. You must activate the clientID between 3 and 7. 


## Features

#### Server-side supported command
- `start`
    - Delete previous folder and data.
  - Create a whole new folder and file.
- `setup`
  - Establish socket connection with neighbor servers.
- `check`
  - You can check the socket that connect to current server.
- `test`
  - Sending a test message to every socket that connect to you. 
- `exit`
  - End the program.

#### Client-side supported command

- `setup`
  - Establish socket connection to all servers.
- `check`
  - Check the server socket connection status.
- `send`
  - Randomly choose a server to sent local timestamp
  - Timestamp format: `<clientID, HH.MM.SS.XXXXXX>`
- `enquiry`
  - Randomly choose a server to query the list of host file.
- `exit`
  - End the program


## License

[![MIT License](https://img.shields.io/badge/License-MIT-green.svg)](https://choosealicense.com/licenses/mit/)


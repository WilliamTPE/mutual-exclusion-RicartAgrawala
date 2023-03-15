
# Ricart-Agrawala algorithm for distributed mutual exclusion

This program is about implementing the Ricart-Agrawala algorithm for distributed mutual exclusion, with the optimization proposed by Roucairol and Carvalho, in a client-server model.




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


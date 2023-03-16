/**
 * @author William Chang
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server {

    String Id;
    ServerSocket server;
    List<Node> allServerNodes = new LinkedList<>();
    List<Node> allClientNodes = new LinkedList<>();
    HashMap<String,Boolean> serverPermissionRequired = new HashMap<>();
    List<ServerSocketConnection> s_socketConnectionList = new LinkedList<>();
    List<ServerSocketConnection> ss_socketConnectionList = new LinkedList<>();
    List<ServerSocketConnection> clientSocketConnectionList = new LinkedList<>();
    HashMap<String,ServerSocketConnection> s_socketConnectionHashMap = new HashMap<>();
    HashMap<String,ServerSocketConnection> ss_socketConnectionHashMap = new HashMap<>();
    HashMap<String,ServerSocketConnection> clientSocketConnectionHashMap = new HashMap<>();
    Integer logicalClock = 0;
    Integer highestLogicalClockValue = 0;
    Integer outStandingReplyCount = 0;
    Boolean requestedCS = false;
    Boolean usingCS = false;
    List<String> deferredReplyList = new LinkedList<>();
    String requestedCSForFile;
    String requestedCSForFileTS;
    String proxyServer;
    String requestingClient;
    Boolean csWriteComplete = true;
    Integer noOfServer = 0;
    Integer writeAckCount = 0;


    private String pathName;

    public Server(String id){
        this.Id = id;
    }
    private void setPathName(String id){
        pathName = "./files"+ Integer.valueOf(id);
    }
    private String getPathName(){
        return pathName;
    }

    public static void deleteDir(File file){
        File[] files = file.listFiles();
        if(files!=null) { //some JVMs return null for empty dirs
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteDir(f);
                } else {
                    f.delete();
                }
            }
        }
        file.delete();
    }

    public static void createDir(File path){
        path.mkdir();
    }

    public static void createFile(File path) throws IOException {
        path.createNewFile();
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public List<Node> getAllServerNodes() {
        return allServerNodes;
    }

    public List<Node> getAllClientNodes() {
        return allClientNodes;
    }

    public void setAllClientNodes(List<Node> allClientNodes) {
        this.allClientNodes = allClientNodes;
    }

    public void setAllServerNodes(List<Node> allServerNodes) {
        this.allServerNodes = allServerNodes;
    }

    public Integer getLogicalClock() {
        return logicalClock;
    }

    public void setLogicalClock(Integer logicalClock) {
        this.logicalClock = logicalClock;
    }


    /**
     * For terminal input command
     */
    public class CommandParser extends Thread{

        Server currentServer;

        public CommandParser(Server currentServer){
            this.currentServer = currentServer;
        }

        Pattern START = Pattern.compile("^start$");
        Pattern SETUP = Pattern.compile("^setup$");
        Pattern TEST = Pattern.compile("^test$");
        Pattern EXIT = Pattern.compile("^exit$");
        Pattern CHECKCONNECTION = Pattern.compile("^check$");
        Pattern WTEST = Pattern.compile("^wtest$");
        Pattern CLIENT_SETUP = Pattern.compile("^clientsetup$");
        //Pattern ENQUIRY = Pattern.compile("^enquiry$");


        int rx_cmd(Scanner cmd){
            String cmd_in = null;
            if (cmd.hasNext())
                cmd_in = cmd.nextLine();
            Matcher m_START = START.matcher(cmd_in);
            Matcher m_EXIT = EXIT.matcher(cmd_in);
            Matcher m_SETUP = SETUP.matcher(cmd_in);
            Matcher m_CHECKCONNECTION = CHECKCONNECTION.matcher(cmd_in);
            Matcher m_TEST = TEST.matcher(cmd_in);
            //Matcher m_ENQUIRY = ENQUIRY.matcher(cmd_in);
            Matcher m_WTEST = WTEST.matcher(cmd_in);
            Matcher m_CLIENT_SETUP = CLIENT_SETUP.matcher(cmd_in);

            if(m_START.find()){
                deleteDir(new File(currentServer.getPathName()));
                createDir(new File(currentServer.getPathName()));
                for (int i = 0;i<5;i++){
                    try {
                        createFile(new File(currentServer.getPathName()+"/"+(i+3)+".txt"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("Delete previous folder and Create new folder...");
            }

            else if (m_CLIENT_SETUP.find()){
                System.out.println("Connect to client");
                setupClientConnections(currentServer);
            }

            else if (m_WTEST.find()){
                sendRequest(Id,"3","timestamp", "3");
            }

            /*else if (m_ENQUIRY.find()){
                String cID = "0";
                listHost(Id, cID);
            }*/

            else if (m_EXIT.find()){
                System.out.println("\n---Server closed---\n");
                System.exit(0);
            }

            else if (m_SETUP.find()){
                System.out.println("Establish connection with neighbor server");
                setupConnections(currentServer);
            }

            else if (m_CHECKCONNECTION.find()){
                System.out.println("Number of socket connect: " + s_socketConnectionList.size() + "\n");
                Integer i = 0;
                for (i = 0; i < s_socketConnectionList.size(); i++){
                    System.out.println("Socket" + s_socketConnectionList.get(i).getRemote_id());
                    System.out.println("IP: " + s_socketConnectionList.get(i).getOtherClient().getInetAddress() + " Port: " + s_socketConnectionList.get(i).getOtherClient().getPort());
                }
                /*
                for (String key: s_socketConnectionHashMap.keySet()){
                    System.out.println("ServerID: " + key + " Socket: " + s_socketConnectionHashMap.get(key).getOtherClient().getPort());
                }*/
            }

            else if (m_TEST.find()){
                sendP();
            }

            return 1;
        }

        public void run() {
            System.out.print("\n>>");
            Scanner input = new Scanner(System.in);
            while(rx_cmd(input) != 0) {
                System.out.print("\n>>");
            }
        }
    }

    /**
     * Send a request to other servers in order to get into critical section.
     * @param proxyServer the server that process write request from client
     * @param fileName the file that client request to write
     * @param ts the client's local timestamp
     */
    public synchronized void sendRequest(String proxyServer, String clientID, String ts, String fileName){
        if (!(this.requestedCS || this.usingCS)){
            this.requestedCSForFile = fileName;
            this.requestedCSForFileTS = ts;
            this.proxyServer = proxyServer;
            this.requestingClient = clientID;
            this.requestedCS = true;
            this.logicalClock = this.highestLogicalClockValue + 1;
            System.out.println("Sending REQUEST with logical clock: " + this.logicalClock +" requesting write file " + this.requestedCSForFile +".txt");
            Integer i;
            for (i = 0; i < allServerNodes.size()-1; i++){
                if (serverPermissionRequired.get(s_socketConnectionList.get(i).getRemote_id()) == true){
                    this.outStandingReplyCount = this.outStandingReplyCount + 1;
                    s_socketConnectionList.get(i).request(this.logicalClock, this.requestedCSForFile, this.requestedCSForFileTS, this.requestingClient);
                }
            }
            if (this.outStandingReplyCount == 0){
                enterCriticalSection(fileName, ts, proxyServer, clientID);
            }
        }
        else {
            System.out.println("Currently in C.S. or already requested for C.S.");
        }
    }

    /**
     * To process REQUEST from proxy server sent
     * @param proxyServerId the ID of server that send WRITE request to other server
     * @param proxyServerLogicalClock the logical clock of proxy server
     * @param fileName the file name that request to write
     * @param TS the timestamp of the client sent
     */
    public synchronized void processRequest(String proxyServerId, Integer proxyServerLogicalClock, String fileName, String TS, String clientID) {
        if (fileName.equals(this.requestedCSForFile)){
            System.out.println("Process Request for request Proxy Server: " + proxyServerId + " which had logical clock value of: " + proxyServerLogicalClock);
            this.highestLogicalClockValue = Math.max(this.highestLogicalClockValue, proxyServerLogicalClock);
            if (this.usingCS || this.requestedCS) {
                if (proxyServerLogicalClock > this.logicalClock) {
                    System.out.println("USING OR REQUESTED CS - 1");
                    System.out.println("Highest Logical Clock Value: " + this.highestLogicalClockValue);
                    System.out.println("Current Logical Clock Value:" + this.logicalClock);
                } else if (proxyServerLogicalClock == this.logicalClock) {
                    System.out.println("Highest Logical Clock Value: " + this.highestLogicalClockValue);
                    System.out.println("Current Logical Clock Value:" + this.logicalClock);
                    System.out.println("USING OR REQUESTED CS - 2");
                }
            }
            if (((this.usingCS || this.requestedCS) && (proxyServerLogicalClock > this.logicalClock)) || ((this.usingCS || this.requestedCS) && proxyServerLogicalClock == this.logicalClock && Integer.valueOf(proxyServerId) > Integer.valueOf(this.getId()))){
                System.out.println("_____________________________________________________________________________________________________");
                System.out.println("Deferred Reply for request Proxy Server: " + proxyServerId + " which had logical clock value of: " + proxyServerLogicalClock);
                System.out.println("Critical Section Access from this node had SERVER ID" + this.getId() + "and last updated logical clock is: " + this.logicalClock);
                System.out.println("_____________________________________________________________________________________________________");
                this.serverPermissionRequired.replace(proxyServerId, true);
                this.deferredReplyList.add(proxyServerId);
            } else {
                System.out.println("Sending REPLY without block as defer condition is not met for the same file " + this.requestedCSForFile + fileName);
                this.serverPermissionRequired.replace(proxyServerId, true);
                ServerSocketConnection requestServerSocketConnection = s_socketConnectionHashMap.get(proxyServerId);
                requestServerSocketConnection.reply(fileName, TS, clientID);
            }
        }
        else {
            System.out.println("Process Request for ** DIFFERENT FILE ** request Proxy Server: " + proxyServerId + " which had logical clock value of: " + proxyServerLogicalClock);
            this.highestLogicalClockValue = Math.max(this.highestLogicalClockValue, proxyServerLogicalClock);
            System.out.println("Sending REPLY without block");
            this.serverPermissionRequired.replace(proxyServerId, true);
            ServerSocketConnection requestingConnect = s_socketConnectionHashMap.get(proxyServerId);
            requestingConnect.reply(fileName, TS, clientID);
        }

    }

    /**
     * To process the REPLY that sent from other servers.
     * If REPLY message had all received, then get into critical section.
     * @param replyingServerId the Id of server that sent reply message
     * @param fileName the file name that request to write
     * @param TS the timestamp of the client sent
     */
    public synchronized void processReply(String replyingServerId, String fileName, String TS, String clientID) {
        if (fileName.equals(this.requestedCSForFile)){
            System.out.println("Process Reply for replying Server: " + replyingServerId +" for the file " + fileName +".txt");
            this.serverPermissionRequired.replace(replyingServerId, false);
            this.outStandingReplyCount = this.outStandingReplyCount -1;
            if (this.outStandingReplyCount == 0){
                enterCriticalSection(fileName, TS, this.proxyServer, clientID);
            }
        } else {
            System.out.println("Process Reply for replying Server: " + replyingServerId +" for the file " + fileName + ".txt ### NO ACTION TAKEN");
        }
    }

    /**
     * Begin to write file to proxy server and other servers
     * @param fileName the file name that ready to write
     * @param TS the client's timestamp that ready to append
     * @param proxyServer the server that process write request from client
     */
    private void enterCriticalSection(String fileName, String TS, String proxyServer, String clientID) {
        System.out.println("\n------------ ENTER CRITICAL SECTION -----------");
        System.out.println("Initiating WRITE timestamp to all servers");
        this.usingCS = true;
        this.requestedCS = false;
        this.csWriteComplete = false;
        try {
            this.writeAckCount = this.noOfServer;
            Integer serverConnectIndex;
            writeFileLocal(proxyServer, fileName, TS, clientID);
            for (serverConnectIndex = 0; serverConnectIndex < allServerNodes.size()-1; serverConnectIndex++){
                this.s_socketConnectionList.get(serverConnectIndex).write(fileName, TS, proxyServer, clientID);
            }

            System.out.println("All servers write "+ fileName+ ".txt complete." );
        } catch (Exception e) {
            System.out.println("File write error");
        }
        System.out.println("------------ EXIT CRITICAL SECTION -----------\n");
    }

    /**
     * If all servers had written the file complete,
     * @param fileName the file name that already written
     */
    public synchronized void processWriteACK(String fileName, String clientID) {
        System.out.println("Inside WRITE_TO_FILE_ACK processor ");
        if (fileName.equals(this.requestedCSForFile)){
            this.writeAckCount = this.writeAckCount - 1;
            if (this.writeAckCount == 0){
                this.csWriteComplete = true;
                System.out.println("All servers write "+ fileName + ".txt complete." );
                s_socketConnectionList.get(Integer.valueOf(clientID)-1).sendComplete();
                releaseCSCleanUp();
            }
        }
    }

    /**
     * To release the critical section,
     * sends REPLY message to all the deferred requests.
     */
    private void releaseCSCleanUp() {
        //System.out.println("\nRecieved necessary acknowledgement");
        System.out.println("\n------- Begin to Clean Up -------");
        System.out.println("Send deferred reply and reset flag");
        this.usingCS = false;
        this.requestedCS = false;
        Iterator<String> deferredReplyClientId = deferredReplyList.iterator();
        while (deferredReplyClientId.hasNext()){
            s_socketConnectionHashMap.get(deferredReplyClientId.next()).reply(this.requestedCSForFile, this.requestedCSForFileTS, this.requestingClient);
        }
        this.requestedCSForFile = "";
        this.requestedCSForFileTS = "";
        deferredReplyList.clear();
        System.out.println("      <<Finished clean up>>");
        System.out.println("-------- Finished Clean Up ----------\n");
    }

    /**
     * Server connects to neighbor server
     * @param currentServer the current server that ready to connect to the neighbor
     */
    public void setupConnections(Server currentServer){
        try {
            Integer serverId;
            for (serverId = Integer.valueOf(this.Id)+1; serverId < allServerNodes.size(); serverId++){
                Socket s_clientConnection = new Socket(this.allServerNodes.get(serverId).getIpAddress(), Integer.valueOf(allServerNodes.get(serverId).getPort()));
                ServerSocketConnection serverSocketConnectionServer = new ServerSocketConnection(s_clientConnection, this.getId(),true,  currentServer);
                if (serverSocketConnectionServer.getRemote_id() == null){
                    serverSocketConnectionServer.setRemote_id(Integer.toString(serverId));
                }
                ss_socketConnectionList.add(serverSocketConnectionServer);
                ss_socketConnectionHashMap.put(serverSocketConnectionServer.getRemote_id(), serverSocketConnectionServer);
                s_socketConnectionList.add(serverSocketConnectionServer);
                s_socketConnectionHashMap.put(serverSocketConnectionServer.getRemote_id(), serverSocketConnectionServer);

                serverPermissionRequired.put(serverSocketConnectionServer.getRemote_id(), true);

                //serverSocketConnectionList.add(serverSocketConnection);
                //serverSocketConnectionListServer.add(serverSocketConnection);
                //serverSocketConnectionHashMap.put(serverSocketConnection.getRemote_id(), serverSocketConnection);
            }
            this.noOfServer = allServerNodes.size()-1;
            //this.noOfServer = s_socketConnectionList.size();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setupClientConnections(Server currentServer){
        try {
            Integer clientId;
            for (clientId = 0; clientId < allClientNodes.size(); clientId++){
                Socket clientConnection = new Socket(this.allClientNodes.get(clientId).getIpAddress(), Integer.valueOf(this.allClientNodes.get(clientId).getPort()));
                ServerSocketConnection socketConnectionClient = new ServerSocketConnection(clientConnection, this.getId(), true,currentServer);
                if (socketConnectionClient.getRemote_id() == null){
                    socketConnectionClient.setRemote_id(Integer.toString(clientId));
                }
                clientSocketConnectionList.add(socketConnectionClient);
                clientSocketConnectionHashMap.put(socketConnectionClient.getRemote_id(),socketConnectionClient);
            }
        } catch (Exception e) {
            System.out.println("Setup Client Connection Failure");
        }
    }

    /**
     * Sending test message to all neighbor server
     */
    public void sendP(){
        System.out.println("Sending test message");
        Integer i;
        for (i = 0; i < s_socketConnectionList.size(); i++){
            s_socketConnectionList.get(i).publish();
        }
    }

    /**
     * Once gain CS permission, begin writing file (WRITE)
     * and send WRITE acknowledge to proxy server
     * @param serverID the folder name
     * @param fileName the file name that ready to write
     * @param timeStamp Client's local timestamp
     *                  FORMAT: < clientId, HH:MM:SS.XXXXXX>
     */
    public synchronized void writeFile(String serverID, String fileName, String timeStamp, String proxyServer, String clientID){
        try {
            System.out.println("Write timestamp to ./files"+ Integer.valueOf(serverID) + "/" + Integer.valueOf(fileName)+".txt");
            FileWriter writer = new FileWriter("./files" + Integer.valueOf(serverID) + "/" + Integer.valueOf(fileName)+".txt", true);
            writer.append(clientID + "," + timeStamp+"\n");
            writer.close();
            s_socketConnectionHashMap.get(proxyServer).sendWriteACK(fileName, clientID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Once gain CS permission, begin writing file (WRITE) to local server
     * @param serverID the folder name
     * @param fileName the file name that ready to write
     * @param timeStamp Client's local timestamp
     */
    public synchronized void writeFileLocal(String serverID, String fileName, String timeStamp, String clientID){
        try {
            FileWriter writer = new FileWriter("./files" + Integer.valueOf(serverID) + "/" + Integer.valueOf(fileName)+".txt", true);
            writer.append(clientID + "," + timeStamp+"\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * List all files (ENQUIRY)
     * @param serverID the folder name
     */
    public synchronized void listHost(String serverID, String clientID){
        File folder = new File("./files" + Integer.valueOf(serverID)+ "/");
        String[] pathnames;
        pathnames = folder.list();
        /*for (String pathname : pathnames){
            System.out.println(pathname);
        }*/
        s_socketConnectionList.get(Integer.valueOf(clientID)-1).replyHostFiles(pathnames);
    }

    /**
     * Process server config file and set the details for the use of current node
     */
    public void setServerList(){
        try {
            BufferedReader br = new BufferedReader(new FileReader("ServerAddressAndPorts.txt"));
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    List<String> parsed_server = Arrays.asList(line.split(","));
                    Node n_server = new Node(parsed_server.get(0),parsed_server.get(1),parsed_server.get(2));
                    this.getAllServerNodes().add(n_server);
                    sb.append(System.lineSeparator());
                    line = br.readLine();
                }
                String everything = sb.toString();
                //System.out.println(everything);
                //System.out.println(this.getAllServerNodes().size());

            } finally {
                br.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Process client config file and set the details for the use of current node
     */
    public void setClientList(){
        try {
            BufferedReader br = new BufferedReader(new FileReader("ClientAddressAndPorts.txt"));
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    List<String> parsed_client = Arrays.asList(line.split(","));
                    Node n_client= new Node(parsed_client.get(0),parsed_client.get(1),parsed_client.get(2));
                    this.getAllClientNodes().add(n_client);
                    sb.append(System.lineSeparator());
                    line = br.readLine();
                }
                String everything = sb.toString();
                //System.out.println(everything);
                //System.out.println(this.getAllClientNodes().size());

            } finally {
                br.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initiating server socket to listen
     * If other machine establish socket connection, then put these machines into LinkList and HashMap
     * @param serverId server number
     * @param currentServer current local server
     */
    public void serverSocket(Integer serverId, Server currentServer){
        try
        {
            server = new ServerSocket(Integer.valueOf(this.allServerNodes.get(serverId).port));
            Id = Integer.toString(serverId);
            InetAddress myServerIp = InetAddress.getLocalHost();
            System.out.println(" Server" + Id + " Information");
            System.out.println("       Port: " + Integer.valueOf(this.allServerNodes.get(serverId).port));
            System.out.println(" IP address: " + myServerIp.getHostAddress());
            System.out.println("   Hostname: " + myServerIp.getHostName());
        }
        catch (IOException e)
        {
            System.out.println("Error creating socket");
            System.exit(-1);
        }

        Server.CommandParser cmdpsr = new Server.CommandParser(currentServer);
        cmdpsr.start();

        Thread current_node = new Thread() {
            public void run(){
                while(true){
                    try{
                        Socket s = server.accept();
                        ServerSocketConnection serverSocketConnection = new ServerSocketConnection(s,Id,false, currentServer);
                        s_socketConnectionList.add(serverSocketConnection);
                        s_socketConnectionHashMap.put(serverSocketConnection.getRemote_id(), serverSocketConnection);
                        serverPermissionRequired.put(serverSocketConnection.getRemote_id(), true);

                    }
                    catch(IOException e){ e.printStackTrace(); }
                }
            }
        };

        current_node.setDaemon(true);
        current_node.start();
    }

    /**
     * 1. Setup folder path name
     * 2. Read server config file into the allServerNodes list
     * 3. Read client config file into the allClientNodes list
     * 4. Initiating server socket
     * @param args server id number
     */
    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Usage: java Server <server-number>");
            System.exit(1);
        }

        Server server = new Server(args[0]);
        System.out.println("\n========== Welcome to Server" + server.getId() + " ==========");
        server.setPathName(args[0]);
        server.setServerList();
        server.setClientList();
        server.serverSocket(Integer.valueOf(args[0]),server);

    }
}


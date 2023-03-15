/**
 * CS6378 Advanced Operating System - Project 2
 * @author William Chang
 * @netid cxc200006
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {

    String Id;
    ServerSocket server;
    List<Node> allClientNodes = new LinkedList<>();
    List<Node> allServerNodes = new LinkedList<>();
    Integer logicalClock = 0;
    List<SocketConnection> socketConnectionList = new LinkedList<>();
    List<SocketConnection> socketConnectionListServer = new LinkedList<>();
    HashMap<String,SocketConnection> socketConnectionHashMap = new HashMap<>();
    HashMap<String,SocketConnection> socketConnectionHashMapServer = new HashMap<>();


    public Client(String id) {
        this.Id = id;
    }

    public String getId() {
        return this.Id;
    }

    public void setId(String id) {
        this.Id = id;
    }

    public List<Node> getAllClientNodes() {
        return allClientNodes;
    }

    public void setAllClientNodes(List<Node> allClientNodes) {
        this.allClientNodes = allClientNodes;
    }

    public List<Node> getAllServerNodes() {
        return allServerNodes;
    }

    public void setAllServerNodes(List<Node> allServerNodes) {
        this.allServerNodes = allServerNodes;
    }


    /**
     * For terminal input command
     */
    public class CommandParser extends Thread{

        Client current;

        public CommandParser(Client current){
            this.current = current;
        }

        Pattern SETUP = Pattern.compile("^setup$");
        Pattern SEND = Pattern.compile("^send$");
        Pattern EXIT = Pattern.compile("^exit$");
        Pattern ENQUIRY = Pattern.compile("^enquiry$");
        Pattern SERVER_TEST = Pattern.compile("^server_test$");
        Pattern CONNECTION_DETAIL = Pattern.compile("^check$");

        int rx_cmd(Scanner cmd){
            String cmd_in = null;
            if (cmd.hasNext())
                cmd_in = cmd.nextLine();
            Matcher m_SETUP = SETUP.matcher(cmd_in);
            Matcher m_EXIT = EXIT.matcher(cmd_in);
            Matcher m_SEND = SEND.matcher(cmd_in);
            Matcher m_ENQUIRY = ENQUIRY.matcher(cmd_in);
            Matcher m_SERVER_TEST = SERVER_TEST.matcher(cmd_in);
            Matcher m_CONNECTION_DETAIL = CONNECTION_DETAIL.matcher(cmd_in);

            /**
             * Setup connection to server
             */
            if(m_SETUP.find()){
                setupServerConnection(current);
            }
            /**
             * sending time stamp to the server
             */
            else if (m_SEND.find()){
                sendTS();
            }

            else if (m_EXIT.find()){
                System.exit(0);
            }

            else if (m_ENQUIRY.find()){
                enquiry(Id);
            }

            else if (m_SERVER_TEST.find()){
                sendTest();
            }

            else if(m_CONNECTION_DETAIL.find()){
                System.out.println("Number of socket connection: " + socketConnectionListServer.size());
                Integer i = 0;

                for(i = 0; i < socketConnectionListServer.size(); i++){
                    System.out.println("\nServer" + socketConnectionListServer.get(i).getRemote_id());
                    System.out.println("IP: " + socketConnectionListServer.get(i).getOtherClient().getInetAddress() + " Port: " + socketConnectionListServer.get(i).getOtherClient().getPort());
                }
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
     * Randomly choose a server to send local timestamp
     */
    public void sendTS(){
        LocalTime ts = LocalTime.now();
        Random random = new Random();
        int proxyServer = random.nextInt(socketConnectionListServer.size());
        Integer fileName = random.nextInt(7 - 3 + 1) + 3;
        System.out.println("Randomly choose Server" + proxyServer + " to send WRITE request for file " + fileName + ".txt");
        socketConnectionListServer.get(proxyServer).send(
                new Message(String.valueOf(proxyServer),
                        this.getId(),
                        String.valueOf(ts),
                        String.valueOf(fileName)));
    }

    public void sendTest(){
        LocalTime ts = LocalTime.now();
        Random random = new Random();
        int num = random.nextInt(socketConnectionListServer.size());
        //System.out.println(num);
        socketConnectionListServer.get(num).sendTest(String.valueOf(ts));
    }

    /**
     * Client can enquiry for host file list
     * @param Id Client ID
     */
    public void enquiry(String Id) {
        Random random = new Random();
        int proxyServer = random.nextInt(socketConnectionListServer.size());
        System.out.println("Randomly choose Server" + proxyServer + " to send ENQUIRY request.");
        String serverID = String.valueOf(proxyServer);
        socketConnectionListServer.get(proxyServer).sendEnquiry(serverID, Id);
    }

    /*Helps establish the socket connection to all the servers available*/

    /**
     * Establish socket connection to all servers
     * @param current current client
     */
    public void setupServerConnection(Client current){
        try{
            System.out.println("Connecting to the server");
            Integer serverId;
            for (serverId =0; serverId < allServerNodes.size(); serverId ++){
                Socket serverConnection = new Socket(this.allServerNodes.get(serverId).getIpAddress(), Integer.valueOf(this.allServerNodes.get(serverId).getPort()));
                SocketConnection socketConnectionServer = new SocketConnection(serverConnection,this.getId(),false,current);
                if(socketConnectionServer.getRemote_id() == null){
                    socketConnectionServer.setRemote_id(Integer.toString(serverId));
                }
                socketConnectionList.add(socketConnectionServer);
                socketConnectionHashMap.put(socketConnectionServer.getRemote_id(),socketConnectionServer);
                socketConnectionListServer.add(socketConnectionServer);
                socketConnectionHashMapServer.put(socketConnectionServer.getRemote_id(),socketConnectionServer);
            }
        }
        catch (Exception e){
            System.out.println("Setup Server Connection Failure");
        }
    }

    /**
     * Printout the list of host file that query from server
     * @param hostFile
     */
    public synchronized void showHostFile(String[] hostFile){
        System.out.println("Host file: ");
        for (String fileName : hostFile){
            System.out.println(fileName);
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
        }
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
        }

    }

    /*Used to create client listen socket and use the listener to add requesting socket connection*/

    /**
     * Initiating client side socket to listen
     * @param ClientId client number
     * @param current current client
     */
    public void clientSocket(Integer ClientId, Client current){
        try
        {
            server = new ServerSocket(Integer.valueOf(this.allClientNodes.get((ClientId-3)).port));
            Id = Integer.toString(ClientId);
            System.out.println(" Client" + Id +" Information:");
            InetAddress myip = InetAddress.getLocalHost();
            System.out.println("       Port: " + Integer.valueOf(this.allClientNodes.get(ClientId-3).port));
            System.out.println(" IP address: " + myip.getHostAddress());
            System.out.println("   Hostname: " + myip.getHostName());
        }
        catch (IOException e)
        {
            System.out.println("Error creating socket");
            System.exit(-1);
        }

        CommandParser cmdpsr = new CommandParser(current);
        cmdpsr.start();

        Thread current_node = new Thread() {
            public void run(){
                while(true){
                    try{
                        Socket s = server.accept();
                        SocketConnection socketConnection = new SocketConnection(s,Id, true, current);
                        socketConnectionList.add(socketConnection);
                        socketConnectionHashMap.put(socketConnection.getRemote_id(),socketConnection);
                    }
                    catch(IOException e){ e.printStackTrace(); }
                }
            }
        };

        current_node.setDaemon(true);
        current_node.start();
    }

    /**
     * 1. Read server config file into the allServerNodes list
     * 2. Read client config file into the allClientNodes list
     * 3. Initiating client socket
     * @param args client id number
     */
    public static void main(String[] args) {

        if (args.length != 1)
        {
            System.out.println("Usage: java Client <client-number>");
            System.exit(1);
        }

        Client clientside = new Client(args[0]);
        System.out.println("\n========== Welcome to Client" + clientside.getId() + " ==========");
        clientside.setClientList();
        clientside.setServerList();
        clientside.clientSocket(Integer.valueOf(args[0]),clientside);

    }
}

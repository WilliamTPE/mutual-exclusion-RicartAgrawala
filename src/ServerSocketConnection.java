/**
 * CS6378 Advanced Operating System - Project 2
 * @author William Chang
 * @netid cxc200006
 */
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Process in and out buffer that given by socket
 */
public class ServerSocketConnection {
    Socket otherClient;
    String my_id;
    String remote_id;
    BufferedReader in;
    PrintWriter out;
    Boolean Initiator;
    Server my_master;

    public String getRemote_id() {
        return remote_id;
    }

    public void setRemote_id(String remote_id) {
        this.remote_id = remote_id;
    }

    public ServerSocketConnection(Socket otherClient, String myId, Boolean isServer,Server my_master) {
        this.otherClient = otherClient;
        this.my_id = myId;
        this.my_master = my_master;
        try{
            in = new BufferedReader(new InputStreamReader(this.otherClient.getInputStream()));
            out = new PrintWriter(this.otherClient.getOutputStream(), true);
        }
        catch (Exception e){

        }

        try {
            if(!isServer) {
                out.println("SEND_ID");
                out.println(my_id);
                //System.out.println("SEND_ID");
                remote_id = in.readLine();
                //System.out.println("SEND_ID request response received with ID: " + remote_id);
            }
        }

        catch (Exception e){

        }
        Thread read = new Thread(){
            public void run(){
                while(rx_cmd(in,out) != 0) { }
            }
        };
        read.setDaemon(true); 	// terminate when main ends
        read.start();
    }


    public int rx_cmd(BufferedReader cmd,PrintWriter out) {
        try {
            String cmd_in = cmd.readLine();

            if(cmd_in.equals("SEND")){
                String proxyServer = cmd.readLine();
                String clientID = cmd.readLine();
                String ts = cmd.readLine();
                String fileName = cmd.readLine();
                System.out.println("\nClient's message: <"+ clientID + ", " + ts + ">");
                System.out.println("Client request to write file: " + fileName + ".txt");
                my_master.sendRequest(proxyServer, clientID, ts, fileName);
            }

            else if(cmd_in.equals("P")){
                System.out.println("Test from sender" + remote_id);
            }

            else if (cmd_in.equals("WRITEFILE")){
                System.out.println("Writing File...");
                String fileName = cmd.readLine();
                String TS = cmd.readLine();
                String proxyServer = cmd.readLine();
                String clientID = cmd.readLine();
                my_master.writeFile(my_master.getId(),fileName,TS, proxyServer, clientID);
            }

            else if(cmd_in.equals("SENDTEST")){
                String ts = cmd.readLine();
                String clientID = cmd.readLine();
                System.out.print("Test message received: ");
                System.out.println("<"+ clientID + ", " + ts + ">");
            }

            else if (cmd_in.equals("ENQUIRY")){
                System.out.println("Received enquiry.");
                String serverID = cmd.readLine();
                String clientID = cmd.readLine();
                my_master.listHost(serverID, clientID);
            }

            else if (cmd_in.equals("REQ")){
                String RerquestingServerId = cmd.readLine();
                Integer RequestingServerLogicalClock = Integer.valueOf(cmd.readLine());
                String FileName = cmd.readLine();
                String TS = cmd.readLine();
                String clientID = cmd.readLine();
                //System.out.println("Received request");
                System.out.println("\nReceived REQUEST from Proxy Server: " + RerquestingServerId + " which had logical clock value of: "+ RequestingServerLogicalClock);
                System.out.println("------Processing REQUEST------\n");
                my_master.processRequest(RerquestingServerId, RequestingServerLogicalClock, FileName, TS, clientID);
            }

            else if (cmd_in.equals("REP")){
                String ReplyingServerId = cmd.readLine();
                String FileName = cmd.readLine();
                String TS = cmd.readLine();
                String clientID = cmd.readLine();
                System.out.println("Received reply from server: " + ReplyingServerId);
                my_master.processReply(ReplyingServerId, FileName, TS, clientID);
            }

            else if (cmd_in.equals("WRITE_FILE_ACK")){
                System.out.println("Received write ACK");
                String fileName = cmd.readLine();
                String clientID = cmd.readLine();
                my_master.processWriteACK(fileName, clientID);
            }

            else if(cmd_in.equals("SEND_ID")){
                out.println(this.my_id);
            }

            else if(cmd_in.equals("SEND_CLIENT_ID")){
                out.println(this.my_id);
            }
        }
        catch (Exception e){}
        return 1;
    }

    public synchronized void request(Integer logicalClock, String fileName, String TS, String clientID) {
        System.out.println("SENDING REQ FROM CLIENT WITH CLIENT ID: " + this.my_id +" to remote CLIENT ID: " + this.getRemote_id() + " for file: "+ fileName);
        out.println("REQ");
        out.println(this.my_id);
        out.println(logicalClock);
        out.println(fileName);
        out.println(TS);
        out.println(clientID);
    }

    public synchronized void reply(String fileName, String TS, String clientID){
        System.out.println("SENDING REP FROM SERVER" + this.my_id +" TO PROXY SERVER" + this.getRemote_id() + " for file: "+ fileName + ".txt");
        out.println("REP");
        out.println(this.my_id);
        out.println(fileName);
        out.println(TS);
        out.println(clientID);
    }

    public synchronized void write(String fileName, String TS, String proxyServer, String clientID) {
        System.out.println("Sending write request from Client ID: " + this.my_id +" to server with SERVER ID: " + this.getRemote_id());
        out.println("WRITEFILE");
        out.println(fileName);
        out.println(TS);
        out.println(proxyServer);
        out.println(clientID);
    }

    public synchronized void sendWriteACK(String fileName, String clientID) {
        System.out.println("Sending write ACK: " + fileName);
        out.println("WRITE_FILE_ACK");
        out.println(fileName);
        out.println(clientID);
    }

    public synchronized void publish() {
        out.println("P");
    }

    public synchronized void replyHostFiles(String[] hostFiles){
        out.println("HOSTFILESLIST");
        out.println(hostFiles.length);
        for (String fileName: hostFiles) {
            out.println(fileName);
        }
    }

    public Socket getOtherClient() {
        return otherClient;
    }

    public void setOtherClient(Socket otherClient) {
        this.otherClient = otherClient;
    }

    public synchronized void sendComplete() {
        System.out.println("Sending complete ack to client");
        out.println("SEND_COMPLETE");
    }
}

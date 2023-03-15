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
public class SocketConnection {

    Socket otherClient;
    String my_id;
    String remote_id;
    String temp;
    BufferedReader in;
    PrintWriter out;
    Boolean Initiator;
    Client my_master;

    public String getRemote_id() {
        return remote_id;
    }

    public void setRemote_id(String remote_id) {
        this.remote_id = remote_id;
    }

    public SocketConnection(Socket otherClient, String myId, Boolean Initiator, Client my_master) {
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
            if(!Initiator) {
                //out.println("SEND_CLIENT_ID");
                out.println(my_id);
                //System.out.println("SEND_CLIENT_ID+++");
                temp = in.readLine();
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

            if (cmd_in.equals("HOSTFILESLIST")){
                int length = Integer.parseInt(cmd.readLine());
                String[] hostFile = new String[length];
                for (int i = 0; i < length; i++){
                    hostFile[i] = cmd.readLine();
                }
                my_master.showHostFile(hostFile);
            }

            else if (cmd_in.equals("SEND_COMPLETE")){
                System.out.println("All server write file completed.");
            }

            else if(cmd_in.equals("SEND_ID")){
                out.println(this.my_id);
            }

            else if(cmd_in.equals("SEND_CLIENT_ID")){
                out.println(this.my_id);
            }

            else if(cmd_in.equals("P")){
                System.out.println("Test from sender" + remote_id);
            }

        }
        catch (Exception e){}
        return 1;
    }

    public synchronized void send(Message message){
        out.println("SEND");
        out.println(message.proxyServer);
        out.println(message.clientId);
        out.println(message.timeStamp);
        out.println(message.fileName);
        //out.println(my_id);
    }

    public synchronized void sendTest(String ts){
        out.println("SENDTEST");
        out.println(ts);
        out.println(my_id);
    }

    public synchronized void sendEnquiry(String serverID, String Id){
        out.println("ENQUIRY");
        out.println(serverID);
        out.println(Id);
    }

    public Socket getOtherClient() {
        return otherClient;
    }

    public void setOtherClient(Socket otherClient) {
        this.otherClient = otherClient;
    }
}



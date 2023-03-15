/**
 * CS6378 Advanced Operating System - Project 2
 * @author William Chang
 * @netid cxc200006
 */
public class Node {

    String Id;
    String ip;
    String port;

    public  Node(String Id, String ip, String port){
        this.Id = Id;
        this.ip = ip;
        this.port = port;
    }


    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public String getIpAddress() {
        return ip;
    }

    public void setIpAddress(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}

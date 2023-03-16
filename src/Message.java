/**
 * @author William Chang
 */
public class Message {

    String clientId;
    String proxyServer;
    String timeStamp;
    String fileName;

    public Message(String proxyServer, String clientId, String timeStamp, String fileName) {
        this.proxyServer = proxyServer;
        this.clientId = clientId;
        this.timeStamp = timeStamp;
        this.fileName = fileName;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getProxyServer() {
        return proxyServer;
    }

    public void setProxyServer(String proxyServer) {
        this.proxyServer = proxyServer;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}

package org.khoicg.chat.model;

public class PeerInfo {
    private String peerId;
    private String ipAddress;
    private int port;

    public PeerInfo(String peerId, String ipAddress, int port) {
        this.peerId = peerId;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getPeerId() { return peerId; }
    public void setPeerId(String peerId) { this.peerId = peerId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    @Override
    public String toString() {
        return peerId + " [" + ipAddress + ":" + port + "]";
    }
}

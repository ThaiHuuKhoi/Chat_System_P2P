package org.khoicg.chat.peer.service;

public final class FileTransferOutbound {

    private final String transferId;
    private final String targetIp;
    private final int targetPort;
    private final String filePath;
    private final String fileName;
    private final int totalChunks;
    private volatile int nextChunkIndex;
    private final long createdAt;

    public FileTransferOutbound(String transferId, String targetIp, int targetPort,
                                String filePath, String fileName, int totalChunks) {
        this.transferId = transferId;
        this.targetIp = targetIp;
        this.targetPort = targetPort;
        this.filePath = filePath;
        this.fileName = fileName;
        this.totalChunks = totalChunks;
        this.nextChunkIndex = 0;
        this.createdAt = System.currentTimeMillis();
    }

    public String getTransferId()    { return transferId; }
    public String getTargetIp()      { return targetIp; }
    public int    getTargetPort()    { return targetPort; }
    public String getFilePath()      { return filePath; }
    public String getFileName()      { return fileName; }
    public int    getTotalChunks()   { return totalChunks; }
    public int    getNextChunkIndex(){ return nextChunkIndex; }
    public void   setNextChunkIndex(int idx) { this.nextChunkIndex = idx; }
    public long   getCreatedAt()     { return createdAt; }

    public boolean targetsAddress(String ip, int port) {
        return targetPort == port && normalize(targetIp).equals(normalize(ip));
    }

    private static String normalize(String ip) {
        if (ip == null) return "";
        String h = ip.trim().toLowerCase();
        return "127.0.0.1".equals(h) ? "localhost" : h;
    }
}

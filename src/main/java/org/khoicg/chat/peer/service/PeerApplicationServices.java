package org.khoicg.chat.peer.service;

import org.khoicg.chat.peer.session.PeerSessionContext;

/**
 * Composition root for peer-side services (facade — avoids constructor chains in menu actions).
 */
public final class PeerApplicationServices {

    public final PeerDirectoryService directory;
    public final OfflineMessageService offline;
    public final DirectChatService     directChat;
    public final GroupChatService      groupChat;
    public final FileSendService       fileSend;
    public final RelaySendService      relaySend;

    private PeerApplicationServices(PeerDirectoryService directory, OfflineMessageService offline,
                                     DirectChatService directChat, GroupChatService groupChat,
                                     FileSendService fileSend, RelaySendService relaySend) {
        this.directory  = directory;
        this.offline    = offline;
        this.directChat = directChat;
        this.groupChat  = groupChat;
        this.fileSend   = fileSend;
        this.relaySend  = relaySend;
    }

    public static PeerApplicationServices create(PeerSessionContext session,
                                                  FileTransferRegistry fileRegistry) {
        PeerDirectoryService directory = new PeerDirectoryService(session);
        OfflineMessageService offline  = new OfflineMessageService(session);
        DirectChatService directChat   = new DirectChatService(session, directory, offline);
        GroupChatService  groupChat    = new GroupChatService(session, directory, offline);
        FileSendService   fileSend     = new FileSendService(session, fileRegistry);
        RelaySendService  relaySend    = new RelaySendService(session);
        return new PeerApplicationServices(directory, offline, directChat, groupChat, fileSend, relaySend);
    }
}

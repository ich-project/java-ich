package org.ich.core.net.messagehandler;

import org.ich.core.exception.P2pException;
import org.ich.core.net.message.TronMessage;
import org.ich.core.net.peer.PeerConnection;

public interface TronMsgHandler {

  void processMessage(PeerConnection peer, TronMessage msg) throws P2pException;

}

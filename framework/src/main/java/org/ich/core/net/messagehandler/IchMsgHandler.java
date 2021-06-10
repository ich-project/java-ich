package org.ich.core.net.messagehandler;

import org.ich.core.exception.P2pException;
import org.ich.core.net.message.IchMessage;
import org.ich.core.net.peer.PeerConnection;

public interface IchMsgHandler {

  void processMessage(PeerConnection peer, IchMessage msg) throws P2pException;

}

package org.ich.core.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.ich.common.common.overlay.server.Channel;
import org.ich.common.common.overlay.server.MessageQueue;
import org.ich.core.net.message.IchMessage;
import org.ich.core.net.peer.PeerConnection;

@Component
@Scope("prototype")
public class IchNetHandler extends SimpleChannelInboundHandler<IchMessage> {

  protected PeerConnection peer;

  private MessageQueue msgQueue;

  @Autowired
  private IchNetService ichNetService;

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, IchMessage msg) throws Exception {
    msgQueue.receivedMessage(msg);
    ichNetService.onMessage(peer, msg);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    peer.processException(cause);
  }

  public void setMsgQueue(MessageQueue msgQueue) {
    this.msgQueue = msgQueue;
  }

  public void setChannel(Channel channel) {
    this.peer = (PeerConnection) channel;
  }

}
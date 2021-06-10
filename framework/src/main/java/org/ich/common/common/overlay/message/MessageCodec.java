package org.ich.common.common.overlay.message;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.ich.common.common.overlay.server.Channel;
import org.ich.core.exception.P2pException;
import org.ich.core.metrics.MetricsKey;
import org.ich.core.metrics.MetricsUtil;
import org.ich.core.net.message.MessageTypes;
import org.ich.core.net.message.PbftMessageFactory;
import org.ich.core.net.message.IchMessageFactory;

@Component
@Scope("prototype")
public class MessageCodec extends ByteToMessageDecoder {

  private Channel channel;
  private P2pMessageFactory p2pMessageFactory = new P2pMessageFactory();
  private IchMessageFactory tronMessageFactory = new IchMessageFactory();
  private PbftMessageFactory pbftMessageFactory = new PbftMessageFactory();

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out)
      throws Exception {
    int length = buffer.readableBytes();
    byte[] encoded = new byte[length];
    buffer.readBytes(encoded);
    try {
      Message msg = createMessage(encoded);
      channel.getNodeStatistics().tcpFlow.add(length);
      MetricsUtil.meterMark(MetricsKey.NET_TCP_IN_TRAFFIC, length);
      out.add(msg);
    } catch (Exception e) {
      channel.processException(e);
    }
  }

  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  private Message createMessage(byte[] encoded) throws Exception {
    byte type = encoded[0];
    if (MessageTypes.inP2pRange(type)) {
      return p2pMessageFactory.create(encoded);
    }
    if (MessageTypes.inIchRange(type)) {
      return tronMessageFactory.create(encoded);
    }
    if (MessageTypes.inPbftRange(type)) {
      return pbftMessageFactory.create(encoded);
    }
    throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE, "type=" + encoded[0]);
  }

}
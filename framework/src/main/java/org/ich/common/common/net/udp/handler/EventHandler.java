package org.ich.common.common.net.udp.handler;

public interface EventHandler {

  void channelActivated();

  void handleEvent(UdpEvent event);

}

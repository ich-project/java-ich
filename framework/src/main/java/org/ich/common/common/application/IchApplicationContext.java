package org.ich.common.common.application;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.ich.common.common.overlay.discover.DiscoverServer;
import org.ich.common.common.overlay.discover.node.NodeManager;
import org.ich.common.common.overlay.server.ChannelManager;
import org.ich.core.db.Manager;

public class IchApplicationContext extends AnnotationConfigApplicationContext {

  public IchApplicationContext() {
  }

  public IchApplicationContext(DefaultListableBeanFactory beanFactory) {
    super(beanFactory);
  }

  public IchApplicationContext(Class<?>... annotatedClasses) {
    super(annotatedClasses);
  }

  public IchApplicationContext(String... basePackages) {
    super(basePackages);
  }

  @Override
  public void destroy() {

    Application appT = ApplicationFactory.create(this);
    appT.shutdownServices();
    appT.shutdown();

    DiscoverServer discoverServer = getBean(DiscoverServer.class);
    discoverServer.close();
    ChannelManager channelManager = getBean(ChannelManager.class);
    channelManager.close();
    NodeManager nodeManager = getBean(NodeManager.class);
    nodeManager.close();

    Manager dbManager = getBean(Manager.class);
    dbManager.stopRePushThread();
    dbManager.stopRePushTriggerThread();
    super.destroy();
  }
}

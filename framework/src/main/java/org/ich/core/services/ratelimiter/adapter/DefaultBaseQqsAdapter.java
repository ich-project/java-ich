package org.ich.core.services.ratelimiter.adapter;

import org.ich.core.services.ratelimiter.RuntimeData;
import org.ich.core.services.ratelimiter.strategy.QpsStrategy;

public class DefaultBaseQqsAdapter implements IRateLimiter {

  private QpsStrategy strategy;

  public DefaultBaseQqsAdapter(String paramString) {
    this.strategy = new QpsStrategy(paramString);
  }

  @Override
  public boolean acquire(RuntimeData data) {
    return strategy.acquire();
  }
}
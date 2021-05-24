package org.ich.core.services.ratelimiter.adapter;

import org.ich.core.services.ratelimiter.RuntimeData;
import org.ich.core.services.ratelimiter.strategy.QpsStrategy;

public class QpsRateLimiterAdapter implements IRateLimiter {

  private QpsStrategy strategy;

  public QpsRateLimiterAdapter(String paramString) {
    strategy = new QpsStrategy(paramString);
  }

  @Override
  public boolean acquire(RuntimeData data) {
    return strategy.acquire();
  }

}
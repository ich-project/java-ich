package org.ich.core.services.ratelimiter.adapter;

import org.ich.core.services.ratelimiter.RuntimeData;

public interface IRateLimiter {

  boolean acquire(RuntimeData data);

}

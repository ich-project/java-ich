package org.ich.core.store;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.ich.core.capsule.MarketOrderCapsule;
import org.ich.core.db.IchStoreWithRevoking;
import org.ich.core.exception.ItemNotFoundException;

@Component
public class MarketOrderStore extends IchStoreWithRevoking<MarketOrderCapsule> {

  @Autowired
  protected MarketOrderStore(@Value("market_order") String dbName) {
    super(dbName);
  }

  @Override
  public MarketOrderCapsule get(byte[] key) throws ItemNotFoundException {
    byte[] value = revokingDB.get(key);
    return new MarketOrderCapsule(value);
  }

}
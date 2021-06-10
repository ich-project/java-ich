package org.ich.core.net.message;

import org.ich.core.Protocol;

public class FetchBlockHeadersMessage extends InventoryMessage {

  public FetchBlockHeadersMessage(byte[] packed) throws Exception {
    super(packed);
    this.type = MessageTypes.FETCH_BLOCK_HEADERS.asByte();
  }

  public FetchBlockHeadersMessage(Protocol.Inventory inv) {
    super(inv);
    this.type = MessageTypes.FETCH_BLOCK_HEADERS.asByte();
  }

}
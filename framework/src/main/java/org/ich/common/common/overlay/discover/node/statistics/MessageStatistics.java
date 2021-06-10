package org.ich.common.common.overlay.discover.node.statistics;

import lombok.extern.slf4j.Slf4j;
import org.ich.common.common.net.udp.message.UdpMessageTypeEnum;
import org.ich.common.common.overlay.message.Message;
import org.ich.core.net.message.FetchInvDataMessage;
import org.ich.core.net.message.InventoryMessage;
import org.ich.core.net.message.MessageTypes;
import org.ich.core.net.message.TransactionsMessage;

@Slf4j
public class MessageStatistics {

  //udp discovery
  public final MessageCount discoverInPing = new MessageCount();
  public final MessageCount discoverOutPing = new MessageCount();
  public final MessageCount discoverInPong = new MessageCount();
  public final MessageCount discoverOutPong = new MessageCount();
  public final MessageCount discoverInFindNode = new MessageCount();
  public final MessageCount discoverOutFindNode = new MessageCount();
  public final MessageCount discoverInNeighbours = new MessageCount();
  public final MessageCount discoverOutNeighbours = new MessageCount();

  //tcp p2p
  public final MessageCount p2pInHello = new MessageCount();
  public final MessageCount p2pOutHello = new MessageCount();
  public final MessageCount p2pInPing = new MessageCount();
  public final MessageCount p2pOutPing = new MessageCount();
  public final MessageCount p2pInPong = new MessageCount();
  public final MessageCount p2pOutPong = new MessageCount();
  public final MessageCount p2pInDisconnect = new MessageCount();
  public final MessageCount p2pOutDisconnect = new MessageCount();

  //tcp tron
  public final MessageCount ichInMessage = new MessageCount();
  public final MessageCount ichOutMessage = new MessageCount();

  public final MessageCount ichInSyncBlockChain = new MessageCount();
  public final MessageCount ichOutSyncBlockChain = new MessageCount();
  public final MessageCount ichInBlockChainInventory = new MessageCount();
  public final MessageCount ichOutBlockChainInventory = new MessageCount();

  public final MessageCount ichInTrxInventory = new MessageCount();
  public final MessageCount ichOutTrxInventory = new MessageCount();
  public final MessageCount ichInTrxInventoryElement = new MessageCount();
  public final MessageCount ichOutTrxInventoryElement = new MessageCount();

  public final MessageCount ichInBlockInventory = new MessageCount();
  public final MessageCount ichOutBlockInventory = new MessageCount();
  public final MessageCount ichInBlockInventoryElement = new MessageCount();
  public final MessageCount ichOutBlockInventoryElement = new MessageCount();

  public final MessageCount ichInTrxFetchInvData = new MessageCount();
  public final MessageCount ichOutTrxFetchInvData = new MessageCount();
  public final MessageCount ichInTrxFetchInvDataElement = new MessageCount();
  public final MessageCount ichOutTrxFetchInvDataElement = new MessageCount();

  public final MessageCount ichInBlockFetchInvData = new MessageCount();
  public final MessageCount ichOutBlockFetchInvData = new MessageCount();
  public final MessageCount ichInBlockFetchInvDataElement = new MessageCount();
  public final MessageCount ichOutBlockFetchInvDataElement = new MessageCount();


  public final MessageCount ichInTrx = new MessageCount();
  public final MessageCount ichOutTrx = new MessageCount();
  public final MessageCount ichInTrxs = new MessageCount();
  public final MessageCount ichOutTrxs = new MessageCount();
  public final MessageCount ichInBlock = new MessageCount();
  public final MessageCount ichOutBlock = new MessageCount();
  public final MessageCount ichOutAdvBlock = new MessageCount();

  public void addUdpInMessage(UdpMessageTypeEnum type) {
    addUdpMessage(type, true);
  }

  public void addUdpOutMessage(UdpMessageTypeEnum type) {
    addUdpMessage(type, false);
  }

  public void addTcpInMessage(Message msg) {
    addTcpMessage(msg, true);
  }

  public void addTcpOutMessage(Message msg) {
    addTcpMessage(msg, false);
  }

  private void addUdpMessage(UdpMessageTypeEnum type, boolean flag) {
    switch (type) {
      case DISCOVER_PING:
        if (flag) {
          discoverInPing.add();
        } else {
          discoverOutPing.add();
        }
        break;
      case DISCOVER_PONG:
        if (flag) {
          discoverInPong.add();
        } else {
          discoverOutPong.add();
        }
        break;
      case DISCOVER_FIND_NODE:
        if (flag) {
          discoverInFindNode.add();
        } else {
          discoverOutFindNode.add();
        }
        break;
      case DISCOVER_NEIGHBORS:
        if (flag) {
          discoverInNeighbours.add();
        } else {
          discoverOutNeighbours.add();
        }
        break;
      default:
        break;
    }
  }

  private void addTcpMessage(Message msg, boolean flag) {

    if (flag) {
      ichInMessage.add();
    } else {
      ichOutMessage.add();
    }

    switch (msg.getType()) {
      case P2P_HELLO:
        if (flag) {
          p2pInHello.add();
        } else {
          p2pOutHello.add();
        }
        break;
      case P2P_PING:
        if (flag) {
          p2pInPing.add();
        } else {
          p2pOutPing.add();
        }
        break;
      case P2P_PONG:
        if (flag) {
          p2pInPong.add();
        } else {
          p2pOutPong.add();
        }
        break;
      case P2P_DISCONNECT:
        if (flag) {
          p2pInDisconnect.add();
        } else {
          p2pOutDisconnect.add();
        }
        break;
      case SYNC_BLOCK_CHAIN:
        if (flag) {
          ichInSyncBlockChain.add();
        } else {
          ichOutSyncBlockChain.add();
        }
        break;
      case BLOCK_CHAIN_INVENTORY:
        if (flag) {
          ichInBlockChainInventory.add();
        } else {
          ichOutBlockChainInventory.add();
        }
        break;
      case INVENTORY:
        InventoryMessage inventoryMessage = (InventoryMessage) msg;
        int inventorySize = inventoryMessage.getInventory().getIdsCount();
        messageProcess(inventoryMessage.getInvMessageType(),
                ichInTrxInventory,ichInTrxInventoryElement,ichInBlockInventory,
                ichInBlockInventoryElement,ichOutTrxInventory,ichOutTrxInventoryElement,
                ichOutBlockInventory,ichOutBlockInventoryElement,
                flag, inventorySize);
        break;
      case FETCH_INV_DATA:
        FetchInvDataMessage fetchInvDataMessage = (FetchInvDataMessage) msg;
        int fetchSize = fetchInvDataMessage.getInventory().getIdsCount();
        messageProcess(fetchInvDataMessage.getInvMessageType(),
                ichInTrxFetchInvData,ichInTrxFetchInvDataElement,ichInBlockFetchInvData,
                ichInBlockFetchInvDataElement,ichOutTrxFetchInvData,ichOutTrxFetchInvDataElement,
                ichOutBlockFetchInvData,ichOutBlockFetchInvDataElement,
                flag, fetchSize);
        break;
      case TRXS:
        TransactionsMessage transactionsMessage = (TransactionsMessage) msg;
        if (flag) {
          ichInTrxs.add();
          ichInTrx.add(transactionsMessage.getTransactions().getTransactionsCount());
        } else {
          ichOutTrxs.add();
          ichOutTrx.add(transactionsMessage.getTransactions().getTransactionsCount());
        }
        break;
      case TRX:
        if (flag) {
          ichInMessage.add();
        } else {
          ichOutMessage.add();
        }
        break;
      case BLOCK:
        if (flag) {
          ichInBlock.add();
        }
        ichOutBlock.add();
        break;
      default:
        break;
    }
  }
  
  
  private void messageProcess(MessageTypes messageType,
                              MessageCount inTrx,
                              MessageCount inTrxEle,
                              MessageCount inBlock,
                              MessageCount inBlockEle,
                              MessageCount outTrx,
                              MessageCount outTrxEle,
                              MessageCount outBlock,
                              MessageCount outBlockEle,
                              boolean flag, int size) {
    if (flag) {
      if (messageType == MessageTypes.TRX) {
        inTrx.add();
        inTrxEle.add(size);
      } else {
        inBlock.add();
        inBlockEle.add(size);
      }
    } else {
      if (messageType == MessageTypes.TRX) {
        outTrx.add();
        outTrxEle.add(size);
      } else {
        outBlock.add();
        outBlockEle.add(size);
      }
    }
  }

}

package org.ich.core.net.messagehandler;

import static org.ich.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ich.common.common.overlay.discover.node.statistics.MessageCount;
import org.ich.common.common.overlay.message.Message;
import org.ich.common.common.utils.Sha256Hash;
import org.ich.consensus.ConsensusDelegate;
import org.ich.core.capsule.BlockCapsule;
import org.ich.core.capsule.BlockCapsule.BlockId;
import org.ich.core.capsule.PbftSignCapsule;
import org.ich.core.config.Parameter.NetConstants;
import org.ich.core.exception.P2pException;
import org.ich.core.exception.P2pException.TypeEnum;
import org.ich.core.net.IchNetDelegate;
import org.ich.core.net.message.BlockMessage;
import org.ich.core.net.message.FetchInvDataMessage;
import org.ich.core.net.message.MessageTypes;
import org.ich.core.net.message.PbftCommitMessage;
import org.ich.core.net.message.TransactionMessage;
import org.ich.core.net.message.TransactionsMessage;
import org.ich.core.net.message.IchMessage;
import org.ich.core.net.peer.Item;
import org.ich.core.net.peer.PeerConnection;
import org.ich.core.net.service.AdvService;
import org.ich.core.net.service.SyncService;
import org.ich.core.Protocol.Inventory.InventoryType;
import org.ich.core.Protocol.PBFTMessage.Raw;
import org.ich.core.Protocol.ReasonCode;
import org.ich.core.Protocol.Transaction;

@Slf4j(topic = "net")
@Component
public class FetchInvDataMsgHandler implements IchMsgHandler {

  private volatile Cache<Long, Boolean> epochCache = CacheBuilder.newBuilder().initialCapacity(100)
      .maximumSize(1000).expireAfterWrite(1, TimeUnit.HOURS).build();

  private static final int MAX_SIZE = 1_000_000;
  @Autowired
  private IchNetDelegate ichNetDelegate;
  @Autowired
  private SyncService syncService;
  @Autowired
  private AdvService advService;
  @Autowired
  private ConsensusDelegate consensusDelegate;

  @Override
  public void processMessage(PeerConnection peer, IchMessage msg) throws P2pException {

    FetchInvDataMessage fetchInvDataMsg = (FetchInvDataMessage) msg;

    check(peer, fetchInvDataMsg);

    InventoryType type = fetchInvDataMsg.getInventoryType();
    List<Transaction> transactions = Lists.newArrayList();

    int size = 0;

    for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
      Item item = new Item(hash, type);
      Message message = advService.getMessage(item);
      if (message == null) {
        try {
          message = ichNetDelegate.getData(hash, type);
        } catch (Exception e) {
          logger.error("Fetch item {} failed. reason: {}", item, hash, e.getMessage());
          peer.disconnect(ReasonCode.FETCH_FAIL);
          return;
        }
      }

      if (type == InventoryType.BLOCK) {
        BlockId blockId = ((BlockMessage) message).getBlockCapsule().getBlockId();
        if (peer.getBlockBothHave().getNum() < blockId.getNum()) {
          peer.setBlockBothHave(blockId);
        }
        sendPbftCommitMessage(peer, ((BlockMessage) message).getBlockCapsule());
        peer.sendMessage(message);
      } else {
        transactions.add(((TransactionMessage) message).getTransactionCapsule().getInstance());
        size += ((TransactionMessage) message).getTransactionCapsule().getInstance()
            .getSerializedSize();
        if (size > MAX_SIZE) {
          peer.sendMessage(new TransactionsMessage(transactions));
          transactions = Lists.newArrayList();
          size = 0;
        }
      }
    }
    if (!transactions.isEmpty()) {
      peer.sendMessage(new TransactionsMessage(transactions));
    }
  }

  private void sendPbftCommitMessage(PeerConnection peer, BlockCapsule blockCapsule) {
    try {
      if (!ichNetDelegate.allowPBFT() || peer.isSyncFinish()) {
        return;
      }
      long epoch = 0;
      PbftSignCapsule pbftSignCapsule = ichNetDelegate
          .getBlockPbftCommitData(blockCapsule.getNum());
      long maintenanceTimeInterval = consensusDelegate.getDynamicPropertiesStore()
          .getMaintenanceTimeInterval();
      if (pbftSignCapsule != null) {
        Raw raw = Raw.parseFrom(pbftSignCapsule.getPbftCommitResult().getData());
        epoch = raw.getEpoch();
        peer.sendMessage(new PbftCommitMessage(pbftSignCapsule));
      } else {
        epoch =
            (blockCapsule.getTimeStamp() / maintenanceTimeInterval + 1) * maintenanceTimeInterval;
      }
      if (epochCache.getIfPresent(epoch) == null) {
        PbftSignCapsule srl = ichNetDelegate.getSRLPbftCommitData(epoch);
        if (srl != null) {
          epochCache.put(epoch, true);
          peer.sendMessage(new PbftCommitMessage(srl));
        }
      }
    } catch (Exception e) {
      logger.error("", e);
    }
  }

  private void check(PeerConnection peer, FetchInvDataMessage fetchInvDataMsg) throws P2pException {
    MessageTypes type = fetchInvDataMsg.getInvMessageType();

    if (type == MessageTypes.TRX) {
      for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
        if (peer.getAdvInvSpread().getIfPresent(new Item(hash, InventoryType.TRX)) == null) {
          throw new P2pException(TypeEnum.BAD_MESSAGE, "not spread inv: {}" + hash);
        }
      }
      int fetchCount = peer.getNodeStatistics().messageStatistics.ichInTrxFetchInvDataElement
          .getCount(10);
      int maxCount = advService.getTrxCount().getCount(60);
      if (fetchCount > maxCount) {
        logger.error("maxCount: " + maxCount + ", fetchCount: " + fetchCount);
        //        throw new P2pException(TypeEnum.BAD_MESSAGE,
        //            "maxCount: " + maxCount + ", fetchCount: " + fetchCount);
      }
    } else {
      boolean isAdv = true;
      for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
        if (peer.getAdvInvSpread().getIfPresent(new Item(hash, InventoryType.BLOCK)) == null) {
          isAdv = false;
          break;
        }
      }
      if (isAdv) {
        MessageCount ichOutAdvBlock = peer.getNodeStatistics().messageStatistics.ichOutAdvBlock;
        ichOutAdvBlock.add(fetchInvDataMsg.getHashList().size());
        int outBlockCountIn1min = ichOutAdvBlock.getCount(60);
        int producedBlockIn2min = 120_000 / BLOCK_PRODUCED_INTERVAL;
        if (outBlockCountIn1min > producedBlockIn2min) {
          logger.error("producedBlockIn2min: " + producedBlockIn2min + ", outBlockCountIn1min: "
              + outBlockCountIn1min);
          //throw new P2pException(TypeEnum.BAD_MESSAGE, "producedBlockIn2min: "
          // + producedBlockIn2min
          //  + ", outBlockCountIn1min: " + outBlockCountIn1min);
        }
      } else {
        if (!peer.isNeedSyncFromUs()) {
          throw new P2pException(TypeEnum.BAD_MESSAGE, "no need sync");
        }
        for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
          long blockNum = new BlockId(hash).getNum();
          long minBlockNum =
              peer.getLastSyncBlockId().getNum() - 2 * NetConstants.SYNC_FETCH_BATCH_NUM;
          if (blockNum < minBlockNum) {
            throw new P2pException(TypeEnum.BAD_MESSAGE,
                "minBlockNum: " + minBlockNum + ", blockNum: " + blockNum);
          }
          if (peer.getSyncBlockIdCache().getIfPresent(hash) != null) {
            throw new P2pException(TypeEnum.BAD_MESSAGE,
                new BlockId(hash).getString() + " is exist");
          }
          peer.getSyncBlockIdCache().put(hash, System.currentTimeMillis());
        }
      }
    }
  }

}

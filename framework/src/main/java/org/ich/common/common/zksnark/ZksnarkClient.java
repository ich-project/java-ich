package org.ich.common.common.zksnark;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannelBuilder;
import org.ich.api.IchZksnarkGrpc;
import org.ich.api.ZksnarkGrpcAPI.ZksnarkRequest;
import org.ich.api.ZksnarkGrpcAPI.ZksnarkResponse.Code;
import org.ich.core.capsule.TransactionCapsule;
import org.ich.core.Protocol.Transaction;

public class ZksnarkClient {

  public static final ZksnarkClient instance = new ZksnarkClient();

  private IchZksnarkGrpc.IchZksnarkBlockingStub blockingStub;

  public ZksnarkClient() {
    blockingStub = IchZksnarkGrpc.newBlockingStub(ManagedChannelBuilder
        .forTarget("127.0.0.1:60051")
        .usePlaintext()
        .build());
  }

  public static ZksnarkClient getInstance() {
    return instance;
  }

  public boolean checkZksnarkProof(Transaction transaction, byte[] sighash, long valueBalance) {
    String txId = new TransactionCapsule(transaction).getTransactionId().toString();
    ZksnarkRequest request = ZksnarkRequest.newBuilder()
        .setTransaction(transaction)
        .setTxId(txId)
        .setSighash(ByteString.copyFrom(sighash))
        .setValueBalance(valueBalance)
        .build();
    return blockingStub.checkZksnarkProof(request).getCode() == Code.SUCCESS;
  }
}

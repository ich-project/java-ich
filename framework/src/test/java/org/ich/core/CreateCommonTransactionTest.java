package org.ich.core;

import static stest.ich.wallet.common.client.WalletClient.decodeFromBase58Check;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannelBuilder;
import org.ich.api.GrpcAPI.TransactionExtention;
import org.ich.api.WalletGrpc;
import org.ich.api.WalletGrpc.WalletBlockingStub;
import org.ich.protos.Protocol.Transaction;
import org.ich.protos.Protocol.Transaction.Contract;
import org.ich.protos.Protocol.Transaction.Contract.ContractType;
import org.ich.protos.Protocol.Transaction.raw;
import org.ich.protos.contract.StorageContract.UpdateBrokerageContract;

public class CreateCommonTransactionTest {

  private static String fullnode = "127.0.0.1:50051";

  /**
   * for example create UpdateBrokerageContract
   */
  public static void testCreateUpdateBrokerageContract() {
    WalletBlockingStub walletStub = WalletGrpc
        .newBlockingStub(ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build());
    UpdateBrokerageContract.Builder updateBrokerageContract = UpdateBrokerageContract.newBuilder();
    updateBrokerageContract.setOwnerAddress(
        ByteString.copyFrom(decodeFromBase58Check("TN3zfjYUmMFK3ZsHSsrdJoNRtGkQmZLBLz")))
        .setBrokerage(10);
    Transaction.Builder transaction = Transaction.newBuilder();
    raw.Builder raw = Transaction.raw.newBuilder();
    Contract.Builder contract = Contract.newBuilder();
    contract.setType(ContractType.UpdateBrokerageContract)
        .setParameter(Any.pack(updateBrokerageContract.build()));
    raw.addContract(contract.build());
    transaction.setRawData(raw.build());
    TransactionExtention transactionExtention = walletStub
        .createCommonTransaction(transaction.build());
    System.out.println("Common UpdateBrokerage: " + transactionExtention);
  }

  public static void main(String[] args) {
    testCreateUpdateBrokerageContract();
  }

}

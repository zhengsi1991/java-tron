package stest.tron.wallet.mutisign.accountPermissionUpdate;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Permission;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class MultiSign8 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);

  private final String testWitnesses = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] WitnessesKey = PublicMethed.getFinalAddress(testWitnesses);
  private ManagedChannel channelFull = null;
  private ManagedChannel searchChannelFull = null;

  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidityInFullnode = null;

  private WalletGrpc.WalletBlockingStub searchBlockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String searchFullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  private ManagedChannel channelSolidity = null;
  private ManagedChannel channelSolidityInFullnode = null;
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);


  private ECKey ecKey = new ECKey(Utils.getRandom());
  private byte[] test001Address = ecKey.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());


  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] test002Address = ecKey2.getAddress();
  private String sendAccountKey2 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  private ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] test003Address = ecKey3.getAddress();
  String sendAccountKey3 = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  private ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] test004Address = ecKey4.getAddress();
  String sendAccountKey4 = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
  private ECKey ecKey5 = new ECKey(Utils.getRandom());
  byte[] test005Address = ecKey5.getAddress();
  String sendAccountKey5 = ByteArray.toHexString(ecKey5.getPrivKeyBytes());

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    searchChannelFull = ManagedChannelBuilder.forTarget(searchFullnode)
        .usePlaintext(true)
        .build();
    searchBlockingStubFull = WalletGrpc.newBlockingStub(searchChannelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

  }


  @Test(enabled = false)
  public void testMultiSignAddKey_11() {
    //创建witness
    //使用在permissionlist（activelist）中的address签名（weight >= thredshold),
    // 再使用 permissionlist(permissionId相同)中的address签名（weight >= thredshold).

    Assert.assertTrue(PublicMethed
        .sendcoin(test001Address, 10000000000L, fromAddress, testKey002,
            blockingStubFull));
    byte[] url = "http://jjj".getBytes();
    Assert.assertTrue(createWitness(test001Address, url, dev001Key));
    Assert.assertTrue(PublicMethed
        .sendcoin(test002Address, 10000000000L, fromAddress, testKey002,
            blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(test002Address, 1000000L,
        0, 1, sendAccountKey2, blockingStubFull));
    String voteStr = PublicMethed.getAddressString(dev001Key);
    HashMap<String, String> smallVoteMap = new HashMap<String, String>();
    smallVoteMap.put(voteStr, "1");
    String[] permissionKeyString = new String[1];
    permissionKeyString[0] = sendAccountKey2;

    Boolean aBoolean = PublicMethedForMutiSign
        .voteWitness(smallVoteMap, test002Address, sendAccountKey2, blockingStubFull,
            permissionKeyString);
    Assert.assertTrue(aBoolean);
    Account test001AddressAccount = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList = test001AddressAccount.getActivePermissionList();
    Permission ownerPermission = test001AddressAccount.getOwnerPermission();
    Permission witnessPermission = test001AddressAccount.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission));
    logger.info("wei-----------------------");

    String[] permissionKeyString1 = new String[1];
    permissionKeyString1[0] = dev001Key;

    String accountPermissionJson1 =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\","
            + "\"threshold\":1,\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(dev001Key)
            + "\",\"weight\""
            + ":1}]},\"witness_permission\":{\"type\":1,\"permission_name\":\"owner\",\"threshold\":1,"
            + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\""
            + "threshold\":1,\"operations\":"
            + "\"0020000000000000000000000000000000000000000000000000000000000000\",\"keys\""
            + ":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey2)
            + "\",\"weight\":1}]},"
            + "{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"0020000000000000000000000000000000000000000000000000000000000000\","
            + "\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(sendAccountKey3)
            + "\",\"weight\":1}]}]} ";
    Assert.assertTrue(PublicMethedForMutiSign
        .accountPermissionUpdateWithPermissionId(accountPermissionJson1, test001Address, dev001Key,
            blockingStubFull, 0,
            permissionKeyString1));

    Account test001AddressAccount1 = PublicMethed.queryAccount(test001Address, blockingStubFull);
    List<Permission> permissionsList1 = test001AddressAccount1.getActivePermissionList();
    Permission ownerPermission1 = test001AddressAccount1.getOwnerPermission();
    Permission witnessPermission1 = test001AddressAccount1.getWitnessPermission();
    PublicMethedForMutiSign.printPermissionList(permissionsList1);
    logger.info(PublicMethedForMutiSign.printPermission(ownerPermission1));
    logger.info(PublicMethedForMutiSign.printPermission(witnessPermission1));
    logger.info("1-----------------------");

//    Boolean aBoolean1 = PublicMethedForMutiSign
//        .withdrawBalanceWithPermissionId(test001Address, dev001Key, 0,
//            blockingStubFull, permissionKeyString1);
//    logger.info("aBoolean1:" + aBoolean1);
//    Transaction transaction1 = PublicMethed
//        .addTransactionSign(transaction, sendAccountKey2, blockingStubFull);
//    TransactionSignWeight transactionSignWeight = PublicMethedForMutiSign
//        .getTransactionSignWeight(transaction1, blockingStubFull);
//    logger.info("transaction:" + transactionSignWeight);
//    logger.info("------------------------------------------");
//
//    Transaction transaction2 = PublicMethed
//        .addTransactionSign(transaction1, sendAccountKey3, blockingStubFull);
//    TransactionSignWeight transactionSignWeight1 = PublicMethedForMutiSign
//        .getTransactionSignWeight(transaction2, blockingStubFull);
//    logger.info("transaction1:" + transactionSignWeight1);
//    logger.info("------------------------------------------");
//
//    Return returnResult1 = PublicMethedForMutiSign
//        .broadcastTransaction1(transaction2, blockingStubFull);
//    logger.info("returnResult1:" + returnResult1);
//    Assert.assertTrue(returnResult1.getResult());

  }


  public Boolean createWitness(byte[] owner, byte[] url, String priKey) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Contract.WitnessCreateContract.Builder builder = Contract.WitnessCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setUrl(ByteString.copyFrom(url));
    Contract.WitnessCreateContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.createWitness(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = PublicMethed.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    if (response.getResult() == false) {
      return false;
    } else {
      return true;
    }

  }


  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (searchChannelFull != null) {
      searchChannelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}

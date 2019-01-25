package stest.tron.wallet.mutisign.accountPermissionUpdate;

import static org.tron.api.GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;
import stest.tron.wallet.common.client.utils.Sha256Hash;

@Slf4j
public class accountPermissionUpdate007 {

    private final String testKey002 = Configuration.getByPath("testng.conf")
        .getString("foundationAccount.key1");
    private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

    private final String witnessKey001 = Configuration.getByPath("testng.conf")
        .getString("witness.key1");
    private final byte[] witnessAddress001 = PublicMethed.getFinalAddress(witnessKey001);

    private final String contractTRONdiceAddr = "TMYcx6eoRXnePKT1jVn25ZNeMNJ6828HWk";

    private ECKey ecKey1 = new ECKey(Utils.getRandom());
    private byte[] ownerAddress = ecKey1.getAddress();
    private String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    private ECKey ecKey2 = new ECKey(Utils.getRandom());
    private byte[] normalAddr001 = ecKey2.getAddress();
    private String normalKey001 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    private ECKey tmpECKey01 = new ECKey(Utils.getRandom());
    private byte[] tmpAddr01 = tmpECKey01.getAddress();
    private String tmpKey01 = ByteArray.toHexString(tmpECKey01.getPrivKeyBytes());

    private ECKey tmpECKey02 = new ECKey(Utils.getRandom());
    private byte[] tmpAddr02 = tmpECKey02.getAddress();
    private String tmpKey02 = ByteArray.toHexString(tmpECKey02.getPrivKeyBytes());

    private ManagedChannel channelFull = null;
    private WalletGrpc.WalletBlockingStub blockingStubFull = null;
    private String fullnode = Configuration.getByPath("testng.conf")
        .getStringList("fullnode.ip.list").get(0);
    private long maxFeeLimit = Configuration.getByPath("testng.conf")
        .getLong("defaultParameter.maxFeeLimit");

    private static final long now = System.currentTimeMillis();
    private static String tokenName = "testAssetIssue_" + Long.toString(now);
    private static ByteString assetAccountId = null;
    private static final long TotalSupply = 1000L;
    private byte[] transferTokenContractAddress = null;

    private String description = Configuration.getByPath("testng.conf")
        .getString("defaultParameter.assetDescription");
    private String url = Configuration.getByPath("testng.conf")
        .getString("defaultParameter.assetUrl");


    @BeforeSuite
    public void beforeSuite() {
      Wallet wallet = new Wallet();
      Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    }

    @BeforeClass(enabled = true)
    public void beforeClass() {

      channelFull = ManagedChannelBuilder.forTarget(fullnode)
          .usePlaintext(true)
          .build();
      blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
      PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress, testKey002, blockingStubFull);
    }

    private List<String> getStrings(byte[] data){
      int index = 0;
      List<String> ret = new ArrayList<>();
      while(index < data.length){
        ret.add(byte2HexStr(data, index, 32));
        index += 32;
      }
      return ret;
    }

    public static String byte2HexStr(byte[] b, int offset, int length) {
      String stmp="";
      StringBuilder sb = new StringBuilder("");
      for (int n= offset; n<offset + length && n < b.length; n++) {
        stmp = Integer.toHexString(b[n] & 0xFF);
        sb.append((stmp.length()==1)? "0"+stmp : stmp);
      }
      return sb.toString().toUpperCase().trim();
    }


    @Test
    public void testActiveName01() {
      // name = "owner"

      ECKey ecKey1 = new ECKey(Utils.getRandom());
      byte[] ownerAddress = ecKey1.getAddress();
      String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress, testKey002, blockingStubFull);
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      List<String> ownerPermissionKeys = new ArrayList<>();
      List<String> activePermissionKeys = new ArrayList<>();

      PublicMethed.printAddress(ownerKey);
      PublicMethed.printAddress(tmpKey02);

      ownerPermissionKeys.add(ownerKey);

      logger.info("** update owner and active permission to two address");
      String accountPermissionJson =
          "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
              + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
              + "\",\"weight\":1}]},"
              + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"owner\",\"threshold\":2,"
              + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
              + "\"keys\":["
              + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
              + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
              + "]}]}";

      Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
          ownerAddress, ownerKey, blockingStubFull,
          ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

      ownerPermissionKeys.clear();
      ownerPermissionKeys.add(tmpKey02);

      Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
          blockingStubFull).getActivePermissionList()));

      Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
          blockingStubFull).getOwnerPermission().getKeysCount());

      PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
          blockingStubFull).getActivePermissionList());

      System.out.printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
          blockingStubFull).getOwnerPermission()));

      activePermissionKeys.add(witnessKey001);
      activePermissionKeys.add(tmpKey02);

      logger.info("** trigger a normal transaction");
      Assert.assertTrue(PublicMethedForMutiSign
          .sendcoinWithPermissionId(fromAddress, 1_000000, ownerAddress,2, ownerKey,
              blockingStubFull, activePermissionKeys.toArray(new String[activePermissionKeys.size()])));

    }

  @Test
  public void testActiveName02() {
      // name = "active"
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission()));

    activePermissionKeys.add(witnessKey001);
    activePermissionKeys.add(tmpKey02);

    logger.info("** trigger a normal transaction");
    Assert.assertTrue(PublicMethedForMutiSign
        .sendcoinWithPermissionId(fromAddress, 1_000000, ownerAddress,2, ownerKey,
            blockingStubFull, activePermissionKeys.toArray(new String[activePermissionKeys.size()])));
  }


  @Test
  public void testActiveName03() {
      // name = "activea"
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"activea\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission()));

    activePermissionKeys.add(witnessKey001);
    activePermissionKeys.add(tmpKey02);

    logger.info("** trigger a normal transaction");
    Assert.assertTrue(PublicMethedForMutiSign
        .sendcoinWithPermissionId(fromAddress, 1_000000, ownerAddress,2, ownerKey,
            blockingStubFull, activePermissionKeys.toArray(new String[activePermissionKeys.size()])));
  }


  @Test
  public void testActiveName04() {
      // name = "123"
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":123,\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission()));

    activePermissionKeys.add(witnessKey001);
    activePermissionKeys.add(tmpKey02);

    logger.info("** trigger a normal transaction");
    Assert.assertTrue(PublicMethedForMutiSign
        .sendcoinWithPermissionId(fromAddress, 1_000000, ownerAddress,2, ownerKey,
            blockingStubFull, activePermissionKeys.toArray(new String[activePermissionKeys.size()])));
  }

  @Test
  public void testActiveName05() {
      // name = ""
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"\",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

//    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(accountPermissionJson,
//        ownerAddress, ownerKey, blockingStubFull);
//
//    Assert.assertFalse(response.getResult());
//    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
//    Assert.assertEquals("contract validate error : permission's name should not be empty",
//        response.getMessage().toStringUtf8());

    // for debug
    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission()));

    activePermissionKeys.add(witnessKey001);
    activePermissionKeys.add(tmpKey02);

    logger.info("** trigger a normal transaction");
    Assert.assertTrue(PublicMethedForMutiSign
        .sendcoinWithPermissionId(fromAddress, 1_000000, ownerAddress,2, ownerKey,
            blockingStubFull, activePermissionKeys.toArray(new String[activePermissionKeys.size()])));
  }

  @Test
  public void testActiveName06() {
      // name = null
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":"+ null +",\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    boolean ret = false;
    try {
      GrpcAPI.Return response = PublicMethed
          .accountPermissionUpdateForResponse(accountPermissionJson,
              ownerAddress, ownerKey, blockingStubFull);
    } catch (NullPointerException e) {
      logger.info("Expected NullPointerException!");
      ret = true;
    }
    Assert.assertTrue(ret);
  }

  @Test
  public void testActiveName07() {
    // name =
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":,\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    boolean ret = false;
    try {
      GrpcAPI.Return response = PublicMethed
          .accountPermissionUpdateForResponse(accountPermissionJson,
              ownerAddress, ownerKey, blockingStubFull);
    } catch (com.alibaba.fastjson.JSONException e) {
      logger.info("Expected com.alibaba.fastjson.JSONException!");
      ret = true;
    }
    Assert.assertTrue(ret);
  }


  @Test
  public void testActiveName08() {
    // name = 1.1
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] ownerAddress = ecKey1.getAddress();
    String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    PublicMethed.sendcoin(ownerAddress, 1_000_000, fromAddress, testKey002, blockingStubFull);
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    List<String> ownerPermissionKeys = new ArrayList<>();
    List<String> activePermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":1.1,\"threshold\":2,"
            + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out.printf(PublicMethedForMutiSign.printPermission(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission()));

    activePermissionKeys.add(witnessKey001);
    activePermissionKeys.add(tmpKey02);

    logger.info("** trigger a normal transaction");
    Assert.assertTrue(PublicMethedForMutiSign
        .sendcoinWithPermissionId(fromAddress, 1_000000, ownerAddress,2, ownerKey,
            blockingStubFull, activePermissionKeys.toArray(new String[activePermissionKeys.size()])));
  }


  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}

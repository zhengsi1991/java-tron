package stest.tron.wallet.multiSign.accountPermissionUpdate;

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
public class accountPermissionUpdate008 {

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

    @Test
  public void testActiveParent01() {
    // no parent
     List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson = "[{\"name\":\"owner\",\"threshold\":2,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":2}]},"
        + "{\"name\":\"active\",\"threshold\":3,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}]}]";

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
  public void testActiveParent02() {
    // parent = "123"
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson = "[{\"name\":\"owner\",\"threshold\":2,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":2}]},"
        + "{\"parent\":\"123\",\"name\":\"active\",\"threshold\":3,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}]}]";

    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : permission's parent should be owner",
        response.getMessage().toStringUtf8());
  }


  @Test
  public void testActiveParent03() {
      // parent = 123
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson = "[{\"name\":\"123\",\"threshold\":2,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":2}]},"
        + "{\"parent\":123,\"name\":\"active\",\"threshold\":3,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}]}]";

    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : permission's name should be owner or active",
        response.getMessage().toStringUtf8());

  }

  @Test
  public void testActiveParent04() {
    // parent = "abc"
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson = "[{\"name\":\"123\",\"threshold\":2,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":2}]},"
        + "{\"parent\":\"abc\",\"name\":\"active\",\"threshold\":3,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}]}]";

    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : permission's name should be owner or active",
        response.getMessage().toStringUtf8());
  }

  @Test
  public void testActiveParent05() {
    // parent = "active"
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson = "[{\"name\":\"owner\",\"threshold\":2,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":2}]},"
        + "{\"parent\":\"active\",\"name\":\"active\",\"threshold\":3,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}]}]";

    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : permission's parent should be owner",
        response.getMessage().toStringUtf8());
  }

  @Test
  public void testActiveParent06() {
    // parent = owner
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
    String accountPermissionJson = "[{\"name\":\"owner\",\"threshold\":2,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":2}]},"
        + "{\"parent\":\"owner\",\"name\":\"active\",\"threshold\":3,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}]}]";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "active"));

    Assert.assertEquals(1, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "owner"));

    printPermissionList(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList());

    activePermissionKeys.add(witnessKey001);
    activePermissionKeys.add(tmpKey02);

    logger.info("** trigger a normal transaction");
    Assert.assertTrue(PublicMethedForMutiSign
        .sendcoin(fromAddress, 1_000000, ownerAddress, ownerKey, blockingStubFull,
            activePermissionKeys.toArray(new String[activePermissionKeys.size()])));

    recoverAccountPermission(ownerKey, ownerPermissionKeys);
  }

  @Test
  public void testActiveParent07() {
    // parent = ""
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson = "[{\"name\":\"owner\",\"threshold\":2,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":2}]},"
        + "{\"parent\":\"\",\"name\":\"active\",\"threshold\":3,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}]}]";

    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : active permission's parent should not be empty",
        response.getMessage().toStringUtf8());
  }

  @Test
  public void testActiveParent08() {
    // parent = null
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);
    PublicMethed.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson = "[{\"name\":\"owner\",\"threshold\":2,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":2}]},"
        + "{\"parent\":null,\"name\":\"active\",\"threshold\":3,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(witnessKey001) + "\",\"weight\":3},"
        + "{\"address\":\"" + PublicMethed.getAddressString(tmpKey02) + "\",\"weight\":1}]}]";

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

  public void recoverAccountPermission(String ownerKey, List<String> ownerPermissionKeys) {
    logger.info("** recover account permissions");

    PublicMethed.printAddress(ownerKey);
    byte[] ownerAddress = new WalletClient(ownerKey).getAddress();

    String accountPermissionJson = "[{\"name\":\"owner\",\"threshold\":1,\"parent\":\"owner\",\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}]},"
        + "{\"parent\":\"owner\",\"name\":\"active\",\"threshold\":1,\"keys\":["
        + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1}]}]";

    boolean ret = PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()]));

    Assert.assertTrue(ret);
    Assert.assertEquals(1, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "owner"));
    Assert.assertEquals(1, getPermissionCount(PublicMethed.queryAccount(ownerAddress,
        blockingStubFull).getPermissionsList(), "active"));
  }


  public static int getPermissionCount(List<Permission> permissionList, String permissionName) {
    int permissionCount = 0;
    for (Permission permission : permissionList) {
      if (permission.getName().equals(permissionName)) {
        permissionCount = permission.getKeysCount();
        break;
      }
    }
    return permissionCount;
  }


  public static List<String> getPermissionAddress(List<Permission> permissionList, String permissionName) {
    List<String> permissionAddress = new ArrayList<>();
    for (Permission permission : permissionList) {
      if (permission.getName().equals(permissionName)) {
        if (permission.getKeysCount() > 0) {
          for (Key key : permission.getKeysList()) {
            permissionAddress.add(encode58Check(key.getAddress().toByteArray()));
          }
        }
        break;
      }
    }
    return permissionAddress;
  }

  public static void printPermissionList(List<Permission> permissionList) {
    String result = "\n";
    result += "[";
    result += "\n";
    int i = 0;
    for (Permission permission : permissionList) {
      result += "permission " + i + " :::";
      result += "\n";
      result += "{";
      result += "\n";
      result += printPermission(permission);
      result += "\n";
      result += "}";
      result += "\n";
      i++;
    }
    result += "]";
    System.out.println(result);
  }

  public static String printPermission(Permission permission) {
    StringBuffer result = new StringBuffer();
    result.append("name: ");
    result.append(permission.getName());
    result.append("\n");
    result.append("threshold: ");
    result.append(permission.getThreshold());
    result.append("\n");
    if (permission.getKeysCount() > 0) {
      result.append("keys:");
      result.append("\n");
      result.append("[");
      result.append("\n");
      for (Key key : permission.getKeysList()) {
        result.append(printKey(key));
      }
      result.append("]");
      result.append("\n");
    }
    return result.toString();
  }

  public static String printKey(Key key) {
    StringBuffer result = new StringBuffer();
    result.append("address: ");
    result.append(encode58Check(key.getAddress().toByteArray()));
    result.append("\n");
    result.append("weight: ");
    result.append(key.getWeight());
    result.append("\n");
    return result.toString();
  }

  public static String encode58Check(byte[] input) {
    byte[] hash0 = Sha256Hash.hash(input);
    byte[] hash1 = Sha256Hash.hash(hash0);
    byte[] inputCheck = new byte[input.length + 4];
    System.arraycopy(input, 0, inputCheck, 0, input.length);
    System.arraycopy(hash1, 0, inputCheck, input.length, 4);
    return Base58.encode(inputCheck);
  }


  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}

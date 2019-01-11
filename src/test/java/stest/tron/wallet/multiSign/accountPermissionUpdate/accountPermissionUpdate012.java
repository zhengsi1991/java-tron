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
public class accountPermissionUpdate012 {

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
  public void testPermissionCount01() {
    // count = 1
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson = "[{\"keys\":[{\"address\":\""
        + PublicMethed.getAddressString(ownerKey)
        + "\",\"weight\":1}],\"name\":\"owner\",\"threshold\":1,\"parent\":\"owner\"}]";

    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : permission's count should be 2.",
        response.getMessage().toStringUtf8());
  }


  @Test
  public void testPermissionCount02() {
    // count = 3
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson = "[{\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(ownerKey) +
        "\",\"weight\":1}],\"name\":\"owner\",\"threshold\":1,\"parent\":\"owner\"},"
        + "{\"keys\":[{\"address\":\"" + PublicMethed.getAddressString(ownerKey)
        + "\",\"weight\":1}],\"name\":\"owner\",\"threshold\":1,\"parent\":\"owner\"},"
        + "{\"parent\":\"owner\",\"keys\":[{\"address\":\""
        + PublicMethed.getAddressString(ownerKey)
        + "\",\"weight\":1}],\"name\":\"active\",\"threshold\":1}]";

    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : permission's count should be 2.",
        response.getMessage().toStringUtf8());
  }


  @Test
  public void testPermissionCount03() {
    // count = 0
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethed.printAddress(ownerKey);

    ownerPermissionKeys.add(ownerKey);

    logger.info("** update owner and active permission to two address");
    String accountPermissionJson = "[]";

    GrpcAPI.Return response = PublicMethed.accountPermissionUpdateForResponse(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : permission's count should be 2.",
        response.getMessage().toStringUtf8());
  }



  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}

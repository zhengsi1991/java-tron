package stest.tron.wallet.onlineStress;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class MainNetTransferSendOrAsset {

  //testng001、testng002、testng003、testng004
  //fromAssetIssue
  private final String testKey001 =
      "0528dc17428585fc4dece68b79fa7912270a1fe8e85f244372f59eb7e8925e04";
  //toAssetIssue
  private final String testKey002 =
      "553c7b0dee17d3f5b334925f5a90fe99fb0b93d47073d69ec33eead8459d171e";

  //Default
  private final String defaultKey =
      "549c7797b351e48ab1c6bb5857138b418012d97526fc2acba022357d49c93ac0";

  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey001);

  private final byte[] defaultAddress = PublicMethed.getFinalAddress(defaultKey);

  ByteString assetAccountId1;
  ByteString assetAccountId2;
  Long firstTokenInitialBalance = 500000000000000L;
  Long secondTokenInitialBalance = 500000000000000L;


  private Long start;
  private Long end;


  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Account fromAccount = PublicMethed.queryAccount(testKey001,blockingStubFull);
    Account toAccount   = PublicMethed.queryAccount(defaultKey,blockingStubFull);

    PublicMethed.sendcoin(fromAddress,1000000000000000L,defaultAddress,defaultKey,blockingStubFull);

    if (toAccount.getAssetCount() == 0) {
      start = System.currentTimeMillis() + 2000;
      end = System.currentTimeMillis() + 1000000000;
      PublicMethed.createAssetIssue(defaultAddress, "xxd", 500000000000000L,
          1, 1, start, end, 1, "wwwwww","wwwwwwww", 100000L,
          100000L, 1L, 1L, defaultKey, blockingStubFull);
    }
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethed.queryAccount(defaultAddress, blockingStubFull);
    assetAccountId1 = getAssetIdFromThisAccount.getAssetIssuedID();
    String trx = "_";
    byte [] b = trx.getBytes();
    PublicMethed.exchangeCreate(assetAccountId1.toByteArray(), firstTokenInitialBalance,
              b, secondTokenInitialBalance, defaultAddress,
              defaultKey, blockingStubFull);
  }

  /**
   * constructor.
   */

  @AfterClass(enabled = false)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

  }
}



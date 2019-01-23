package stest.tron.wallet.committee;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;


@Slf4j
public class WalletTestCommittee003 {
  private final String testKey002 = "0528dc17428585fc4dece68b79fa7912270a1fe8e85f244372f59eb7e8925e04";
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = "553c7b0dee17d3f5b334925f5a90fe99fb0b93d47073d69ec33eead8459d171e";
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  private final String witnessKey001 = "0528dc17428585fc4dece68b79fa7912270a1fe8e85f244372f59eb7e8925e04";
  //Witness 47.93.33.201
  private final String witnessKey002 = "553c7b0dee17d3f5b334925f5a90fe99fb0b93d47073d69ec33eead8459d171e";
  //Witness 123.56.10.6
  private final String witnessKey003 = "324a2052e491e99026442d81df4d2777292840c1b3949e20696c49096c6bacb8";
  //Wtiness 39.107.80.135
  private final String witnessKey004 = "ff5d867c4434ac17d264afc6696e15365832d5e8000f75733ebb336d66df148d";
  //Witness 47.93.184.2
  private final String witnessKey005 = "2925e186bb1e88988855f11ebf20ea3a6e19ed92328b0ffb576122e769d45b68";


  private final byte[] witness001Address = PublicMethed.getFinalAddress(witnessKey001);
  private final byte[] witness002Address = PublicMethed.getFinalAddress(witnessKey002);
  private final byte[] witness003Address = PublicMethed.getFinalAddress(witnessKey003);
  private final byte[] witness004Address = PublicMethed.getFinalAddress(witnessKey004);
  private final byte[] witness005Address = PublicMethed.getFinalAddress(witnessKey005);


  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private static final long now = System.currentTimeMillis();

  private String fullnode = "47.94.239.172:50051";
  private String soliditynode = "47.94.239.172:50061";

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }

  @Test(enabled = true)
  public void test1ApproveProposal() {

    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(20L, 1L);
    PublicMethed.createProposal(witness001Address,witnessKey001,
        proposalMap,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //Get proposal list
    ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    Optional<ProposalList> listProposals =  Optional.ofNullable(proposalList);
    final Integer proposalId = listProposals.get().getProposalsCount();
    logger.info(Integer.toString(proposalId));


    //Muti approval
    PublicMethed.approveProposal(witness001Address,witnessKey001,proposalId,
            true,blockingStubFull);
    PublicMethed.approveProposal(witness002Address,witnessKey002,proposalId,
            true,blockingStubFull);
    PublicMethed.approveProposal(witness003Address,witnessKey003,proposalId,
            true,blockingStubFull);
    PublicMethed.approveProposal(witness004Address,witnessKey004,proposalId,
            true,blockingStubFull);
    PublicMethed.approveProposal(witness005Address,witnessKey005,proposalId,
            true,blockingStubFull);




  }
  @Test(enabled = true)
  public void test2ApproveProposal() {

    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(20L, 1L);
    PublicMethed.createProposal(witness001Address,witnessKey001,
            proposalMap,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //Get proposal list
    ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    Optional<ProposalList> listProposals =  Optional.ofNullable(proposalList);
    final Integer proposalId = listProposals.get().getProposalsCount();
    logger.info(Integer.toString(proposalId));


    //Muti approval
    PublicMethed.approveProposal(witness001Address,witnessKey001,proposalId,
            true,blockingStubFull);
    PublicMethed.approveProposal(witness002Address,witnessKey002,proposalId,
            true,blockingStubFull);
    PublicMethed.approveProposal(witness003Address,witnessKey003,proposalId,
            true,blockingStubFull);
    PublicMethed.approveProposal(witness004Address,witnessKey004,proposalId,
            true,blockingStubFull);
    PublicMethed.approveProposal(witness005Address,witnessKey005,proposalId,
            true,blockingStubFull);

    try {
      Thread.sleep(900000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }
  @Test(enabled = true)
  public void test3MultiSignPreparation() {
    //TYtZeP1Xnho7LKcgeNsTY2Xg3LTpjfF6G5
    //41FB6A0BA726411CFC24CD066852B3FB8EB1381EFE
    String ownerKey = "795D7F7A3120132695DFB8977CC3B7ACC9770C125EB69037F19DCA55B075C4AE";
    byte[] ownerAddress = PublicMethed.getFinalAddress(ownerKey);


   PublicMethed.sendcoin(ownerAddress,9968981537400L,witness002Address,witnessKey002,
          blockingStubFull);
   PublicMethed.waitProduceNextBlock(blockingStubFull);
   PublicMethed.waitProduceNextBlock(blockingStubFull);
   PublicMethed.waitProduceNextBlock(blockingStubFull);

    String manager1Key = "ff5d867c4434ac17d264afc6696e15365832d5e8000f75733ebb336d66df148d";
    String[] permissionKeyString = new String[3];
    permissionKeyString[0] = manager1Key;

    String manager2Key = "2925e186bb1e88988855f11ebf20ea3a6e19ed92328b0ffb576122e769d45b68";
    permissionKeyString[1] = manager2Key;

    permissionKeyString[2] = ownerKey;

    String[] ownerKeyString = new String[1];
    ownerKeyString[0] = ownerKey;

    String accountPermissionJson =
            "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":3,\"keys\":["
                    + "{\"address\":\"" + PublicMethed.getAddressString(ownerKey) + "\",\"weight\":1},"
                    + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
                    + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}]},"
                    + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
                    + "\"operations\":\"7fff1fc0033e0000000000000000000000000000000000000000000000000000\","
                    + "\"keys\":["
                    + "{\"address\":\"" + PublicMethed.getAddressString(manager1Key) + "\",\"weight\":1},"
                    + "{\"address\":\"" + PublicMethed.getAddressString(manager2Key) + "\",\"weight\":1}"
                    + "]}]}";
    logger.info(accountPermissionJson);
    PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,ownerAddress,ownerKey,
            blockingStubFull,ownerKeyString);


//permissionKeyString[0] = ownerKey;

    String[] ownerKeyString1 = new String[3];
    ownerKeyString1[0] = ownerKey;
    ownerKeyString1[1] = manager1Key;
    ownerKeyString1[2] = manager2Key;
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,ownerAddress,ownerKey,
            blockingStubFull,ownerKeyString);
//    PublicMethedForMutiSign.sendcoin(
//            witness001Address,4000000L,ownerAddress,ownerKey,blockingStubFull,permissionKeyString);
//
//    PublicMethedForMutiSign.triggerContract()

  }


  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}



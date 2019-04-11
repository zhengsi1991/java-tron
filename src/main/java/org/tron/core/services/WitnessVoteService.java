package org.tron.core.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.entity.WitnessVote;
import org.tron.core.db.WitnessVoteStore;

@Component
public class WitnessVoteService {

  @Autowired
  public WitnessVoteStore witnessVoteStore;

  public WitnessVote getVotes(String address, String round) {
    byte[] key = (round + "_" + address).getBytes();
    WitnessVote wv = WitnessVote.ByteArray2Object(witnessVoteStore.get(key));
    return wv;
  }
}
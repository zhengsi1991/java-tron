package org.tron.common.entity;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WitnessVote implements Serializable {

  HashMap<String, Long> map = new HashMap<>();

  public WitnessVote() {
  }

  public void add(String address, Long voteCount) {
    map.put(address, voteCount);
  }

  public byte[] toByteArray() {
    return new byte[0];
  }


  public static WitnessVote ByteArray2Object(byte[] byteArray) {
    WitnessVote wv = null;
    try (ByteArrayInputStream bin = new ByteArrayInputStream(
        byteArray); ObjectInputStream ois = new ObjectInputStream(bin);) {
      wv = (WitnessVote) ois.readObject();
    } catch (Exception e) {
      logger.error(e.getMessage());
    }
    return wv;
  }

  public String toJSONString() {
    JSONObject jsonObject = new JSONObject();

    for (Map.Entry<String, Long> entry : this.map.entrySet()) {
      jsonObject.put(entry.getKey(), entry.getValue());
    }
    return jsonObject.toJSONString();
  }
}

package org.tron.core.db.datacheck;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class StoreCheckPoint {

  @Getter
  private String name;

  @Setter
  private int key_size;

  @Setter
  @Getter
  private List<byte[]> keyList = new ArrayList<>();

  @Setter
  @Getter
  private List<byte[]> valueList = new ArrayList<>();

  public StoreCheckPoint(String name) {
    this.name = name;
  }

  public String sumKey() {
    int sum = 0;
    for (byte[] b : this.keyList) {
      for (byte abyte : b) {
        sum += (int) abyte;
      }
    }
    return String.valueOf(sum);
  }

  public String sumValue() {
    int sum = 0;
    for (byte[] b : this.valueList) {
      for (byte abyte : b) {
        sum += (int) abyte;
      }
    }
    return String.valueOf(sum);
  }

  public String hashValue() {
    return null;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("db:");
    sb.append(this.name);
    /*sb.append("\n");
    sb.append("key size:");
    sb.append(this.keyList.size());
    sb.append("\n");*/
    sb.append("  key sum:");
    sb.append(this.sumKey());
    //sb.append("\n");
    /*sb.append("value size:");
    sb.append(this.valueList.size());
    sb.append("\n");*/
    sb.append("  value sum:");
    sb.append(this.sumValue());
    //sb.append("\n");
    return sb.toString();
  }
}
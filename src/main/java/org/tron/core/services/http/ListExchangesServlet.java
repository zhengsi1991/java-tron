package org.tron.core.services.http;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db.Manager;


@Component
@Slf4j
public class ListExchangesServlet extends HttpServlet {

  @Autowired
  private Wallet wallet;
  @Autowired
  private Manager dbManager;

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
      Map<String,Integer> map= new TreeMap<String, Integer>(
        new Comparator<String>() {
          public int compare(String obj1, String obj2) {
            // 降序排序
            return obj2.compareTo(obj1);
          }
        });
      long lastNumber = 0;
      for (int i = 0; i < 10000; i ++ ) {
        List<BlockCapsule> value =  dbManager.getBlockStore().getLimitNumber(4500000 + i * 1000, 1000);
        if (value.size() == 0) break;
        for (int j = 0; j < value.size(); j ++ ){
          BlockCapsule bl = value.get(j);
          Date date = new Date(bl.getTimeStamp());
          if (map.containsKey(formatter.format(date))){
            map.put(formatter.format(date),map.get(formatter.format(date))+1);
          }else{
            map.put(formatter.format(date),1);
          }
        }
      }
      response.getWriter().println("{");
      for(String key:map.keySet())
      {
        String timsStamp = "time: "+key +" Value: "+(28800-map.get(key));
        response.getWriter().println(timsStamp);
      }
      response.getWriter().println("}");

    } catch (Exception e) {
      logger.debug("Exception: {}", e.getMessage());
      try {
        response.getWriter().println(Util.printErrorMsg(e));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
    }
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    doPost(request, response);
  }
}
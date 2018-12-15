package org.tron.program;


import com.alibaba.fastjson.JSONArray;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSON;

public class TestCurl {


  public static void main(String args[]) throws MalformedURLException {
    System.out.println("hello world !!!");
    Integer last = 0;
    for (int i = 0; i < 100; i ++) {
      try {
        //创建一个URL实例
        int start = 20 *i;
        URL url = new URL("https://wlcyapi.tronscan.org/api/block?sort=-number&limit=20&count=true&start="+ start +"&producer=TGzz8gjYiYRqpfmDwnLxfgPuLVNmpCswVp");

        try {
          //通过URL的openStrean方法获取URL对象所表示的自愿字节输入流
          InputStream is = url.openStream();
          InputStreamReader isr = new InputStreamReader(is,"utf-8");

          //为字符输入流添加缓冲
          BufferedReader br = new BufferedReader(isr);
          String data = br.readLine();//读取数据
          System.out.println("https://wlcyapi.tronscan.org/api/block?sort=-number&limit=20&count=true&start="+ start +"&producer=TGzz8gjYiYRqpfmDwnLxfgPuLVNmpCswVp");
          System.out.println(data);//输出数据
          JSONObject jsonObj = JSON.parseObject(data);
          JSONArray array = jsonObj.getJSONArray("data");
          for (int j = 0; j < array.size(); j++){
            Integer number = array.getJSONObject(j).getInteger("number");
            if (last != 0 && last - number > 26){
              System.out.println("=="+last + " and "+ number );
            }
            last = number;
          }

          br.close();
          isr.close();
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      } catch (MalformedURLException e) {
        e.printStackTrace();
      }
    }
    }

}

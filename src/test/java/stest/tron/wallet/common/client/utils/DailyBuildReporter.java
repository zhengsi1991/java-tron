package stest.tron.wallet.common.client.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

public class DailyBuildReporter extends TestListenerAdapter{

  private String reportPath;

  @Override
  public void onStart(ITestContext context) {
    File htmlReportDir = new File("build/reports/tests/dailyBuild");
    if (!htmlReportDir.exists()) {
      htmlReportDir.mkdirs();
    }
    String reportName = "DailyBuild.html";
    //String reportName = formateDate()+".html";
    reportPath = htmlReportDir+"/"+reportName;
    File report = new File(htmlReportDir,reportName);
    if(report.exists()){
      try {
        report.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    StringBuilder sb = new StringBuilder("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />"
        + "<title >UI自动化测试报告</title></head><body style=\"background-color:#99FFCC;\">"
        + "<div id=\"top\" align=\"center\"><p style=\"font-weight:bold;\">测试用例运行结果列表</p>"
        + "<table width=\"90%\" height=\"80\" border=\"1\" align=\"center\" cellspacing=\"0\" rules=\"all\" style=\"table-layout:relative;\">"
        + "<thead>"
        + "<tr>"
        + "<th>用例总数</th>"
        + "<th>通过数(Passed)</th>"
        + "<th>跳过数(Skipped)</th>"
        + "<th>跳过数(Skipped)</th>"
        + "<th>总耗时</th>"
        + "</tr>"
        + "</thead>"
        + "</tbody></table><a href=\"#top\">返回顶部</a></div></body>"
        + "<table width=\"90%\" height=\"80\" border=\"1\" align=\"center\" cellspacing=\"0\" rules=\"all\" style=\"table-layout:relative;\">"
        + "<thead>"
        + "<tr>"
        + "<th>测试用例名</th>"
        + "<th>测试用例结果</th>"
        + "</tr>"
        + "</thead>"
        + "<tbody style=\"word-wrap:break-word;font-weight:bold;\" align=\"center\">");
    String res = sb.toString();
    try {
      Files.write((Paths.get(reportPath)),res.getBytes("utf-8"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onTestSuccess(ITestResult result) {
    StringBuilder sb = new StringBuilder("<tr><td>");
    sb.append(result.getMethod().getRealClass()+"."+result.getMethod().getMethodName());
    sb.append("</td><td><font color=\"green\">Passed</font></td></tr>");
    String res = sb.toString();
    try {
      Files.write((Paths.get(reportPath)),res.getBytes("utf-8"),StandardOpenOption.APPEND);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onTestSkipped(ITestResult result) {
    StringBuilder sb = new StringBuilder("<tr><td>");
    sb.append(result.getMethod().getMethodName());
    //sb.append(result.getMethod().getRealClass()+"."+result.getMethod().getMethodName());
    sb.append("</td><td><font color=\"yellow\">Skipped</font>");
    sb.append("<p align=\"left\">测试用例<font color=\"red\">跳过</font>，原因：<br>");
    sb.append("<br><a style=\"background-color:#CCCCCC;\">");
    Throwable throwable = result.getThrowable();
    sb.append(throwable.getMessage());
    sb.append("</a></p></td></tr>");
    String res = sb.toString();
    try {
      Files.write((Paths.get(reportPath)),res.getBytes("utf-8"),StandardOpenOption.APPEND);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onTestFailure(ITestResult result) {
    StringBuilder sb = new StringBuilder("<tr><td>");
    //sb.append(result.getMethod().getMethodName());
    sb.append(result.getMethod().getDescription());
    //sb.append(result.getMethod().getRealClass()+"."+result.getMethod().getMethodName());
    sb.append("</td><td><font color=\"red\">Failed</font><br>");
    sb.append("<p align=\"left\">测试用例执行<font color=\"red\">失败</font>，原因：<br>");
    sb.append("<br><a style=\"background-color:#CCCCCC;\">");
    Throwable throwable = result.getThrowable();
    sb.append(throwable.getMessage());
    sb.append("</a></p></td></tr>");
    String res = sb.toString();
    try {
      Files.write((Paths.get(reportPath)),res.getBytes("utf-8"),StandardOpenOption.APPEND);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onFinish(ITestContext testContext) {
    StringBuilder sb = new StringBuilder("</tbody></table><a href=\"#top\">返回顶部</a></div></body>");
    sb.append("</html>");
    String msg = sb.toString();
    try {
      Files.write((Paths.get(reportPath)),msg.getBytes("utf-8"),StandardOpenOption.APPEND);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public static String formateDate(){
    SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    Calendar cal = Calendar.getInstance();
    Date date = cal.getTime();
    return sf.format(date);
  }
}


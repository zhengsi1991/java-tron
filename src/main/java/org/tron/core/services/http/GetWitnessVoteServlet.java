package org.tron.core.services.http;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.entity.WitnessVote;
import org.tron.core.services.WitnessVoteService;

@Component
@Slf4j(topic = "API")
public class GetWitnessVoteServlet extends HttpServlet {

  @Autowired
  private WitnessVoteService witnessVoteService;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      String address = request.getParameter("address");
      String round = request.getParameter("round");
      WitnessVote wv = witnessVoteService.getVotes(address, round);
      if (wv != null) {
        response.getWriter().println(wv.toJSONString());
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      logger.debug("Exception: {}", e.getMessage());
      try {
        response.getWriter().println(Util.printErrorMsg(e));
      } catch (IOException ioe) {
        logger.debug("IOException: {}", ioe.getMessage());
      }
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {

  }
}

package org.ich.core.services.http;

import com.google.protobuf.ByteString;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ich.api.GrpcAPI.BytesMessage;
import org.ich.common.common.utils.ByteArray;
import org.ich.core.Wallet;
import org.ich.protos.Protocol.Block;


@Component
@Slf4j(topic = "API")
public class GetBlockByIdServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String input = request.getParameter("value");
      fillResponse(visible, ByteString.copyFrom(ByteArray.fromHexString(input)), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      PostParams params = PostParams.getPostParams(request);
      BytesMessage.Builder build = BytesMessage.newBuilder();
      JsonFormat.merge(params.getParams(), build, params.isVisible());
      fillResponse(params.isVisible(), build.getValue(), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private void fillResponse(boolean visible, ByteString blockId, HttpServletResponse response)
      throws IOException {
    Block reply = wallet.getBlockById(blockId);
    if (reply != null) {
      response.getWriter().println(Util.printBlock(reply, visible));
    } else {
      response.getWriter().println("{}");
    }
  }
}
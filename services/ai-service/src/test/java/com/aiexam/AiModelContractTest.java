package com.aiexam;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AiModelContractTest {
 @Test void realModelHttpResponseWithStringOptionsIsNormalized()throws Exception{
  var mapper=new ObjectMapper();
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  String content="[{\"content\":\"输入设备是？\",\"options\":[\"A. 键盘\",\"B. 显示器\"],\"answer\":\"A\",\"analysis\":\"键盘用于输入\"}]";
  server.createContext("/chat/completions",exchange->{
   exchange.getRequestBody().readAllBytes();
   byte[] body=mapper.writeValueAsBytes(Map.of("choices",List.of(Map.of("message",Map.of("content",content)))));
   exchange.getResponseHeaders().add("Content-Type","application/json;charset=UTF-8");exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();
  });
  server.start();
  try{
   var request=new AiController.Request(1L,"SINGLE",1,1,"设备","http://127.0.0.1:"+server.getAddress().getPort()+"/chat/completions","local-test-key","fake-model");
   var response=new AiController().generate(request);
   var option=mapper.valueToTree(response).path("questions").get(0).path("options").get(0);
   assertEquals("A",option.path("key").asText());assertEquals("键盘",option.path("text").asText());assertEquals("LIVE",response.get("mode"));
  }finally{server.stop(0);}
 }
}

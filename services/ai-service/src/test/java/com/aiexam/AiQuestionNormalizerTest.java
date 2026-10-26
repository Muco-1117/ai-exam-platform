package com.aiexam;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AiQuestionNormalizerTest {
 private final ObjectMapper mapper=new ObjectMapper();
 private java.util.Map<String,Object> question(String options,String answer,String type)throws Exception{
  String json="[{\"content\":\"输入设备是？\",\"options\":"+options+",\"answer\":\""+answer+"\"}]";
  return AiQuestionNormalizer.normalize(type,1,mapper.readTree(json)).get(0);
 }
 @Test void modelStringArrayBecomesKeyTextObjects()throws Exception{
  var q=question("[\"A. 键盘\",\"B. 显示器\",\"C. 打印机\",\"D. 音箱\"]","A","SINGLE");
  var options=mapper.valueToTree(q.get("options"));
  assertEquals("A",options.get(0).get("key").asText());assertEquals("键盘",options.get(0).get("text").asText());assertEquals("音箱",options.get(3).get("text").asText());
 }
 @Test void unlabeledStringsReceiveSequentialKeys()throws Exception{var q=question("[\"键盘\",\"显示器\"]","A","SINGLE");assertEquals("B",mapper.valueToTree(q.get("options")).get(1).get("key").asText());}
 @Test void canonicalObjectsArePreserved()throws Exception{var q=question("[{\"key\":\"A\",\"text\":\"键盘\"},{\"key\":\"B\",\"text\":\"显示器\"}]","A","SINGLE");assertEquals("键盘",mapper.valueToTree(q.get("options")).get(0).get("text").asText());}
 @Test void mapOptionsAreSupported()throws Exception{var q=question("{\"A\":\"键盘\",\"B\":\"显示器\"}","A","SINGLE");assertEquals(2,mapper.valueToTree(q.get("options")).size());}
 @Test void emptyContentRejected(){assertThrows(IllegalArgumentException.class,()->question("[{\"key\":\"A\",\"text\":\"\"},{\"key\":\"B\",\"text\":\"显示器\"}]","A","SINGLE"));}
 @Test void duplicateKeysRejected(){assertThrows(IllegalArgumentException.class,()->question("[\"A.键盘\",\"A.显示器\"]","A","SINGLE"));}
 @Test void answerMustReferenceOption(){assertThrows(IllegalArgumentException.class,()->question("[\"A.键盘\",\"B.显示器\"]","D","SINGLE"));}
 @Test void multipleAnswersAreCanonicalized()throws Exception{var q=question("[\"A.键盘\",\"B.鼠标\",\"C.显示器\"]","B、A","MULTI");assertEquals("A,B",q.get("answer"));}
 @Test void wrongCountRejected()throws Exception{assertThrows(IllegalArgumentException.class,()->AiQuestionNormalizer.normalize("SINGLE",2,mapper.readTree("[]")));}
}

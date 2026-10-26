package com.aiexam;
import com.aiexam.common.BusinessException;
import com.aiexam.dto.QuestionDTO;
import com.aiexam.mapper.*;
import com.aiexam.service.QuestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class QuestionOptionsRegressionTest {
 private final ObjectMapper mapper=new ObjectMapper();
 private final QuestionService service=new QuestionService(mock(QuestionMapper.class),mock(QuestionCategoryMapper.class),mapper);
 private QuestionDTO dto(String... labels){var d=new QuestionDTO();d.setType("SINGLE");d.setContent("输入设备是？");d.setAnswer("A");var opts=new ArrayList<QuestionDTO.OptionItem>();for(int i=0;i<labels.length;i++){var o=new QuestionDTO.OptionItem();o.setKey(String.valueOf((char)('A'+i)));o.setText(labels[i]);opts.add(o);}d.setOptions(opts);return d;}
 @Test void historicalStringOptionsRenderAsObjects(){var options=mapper.valueToTree(service.parseOptions("[\"A. 键盘\",\"B. 显示器\"]"));assertEquals("A",options.get(0).path("key").asText());assertEquals("键盘",options.get(0).path("text").asText());}
 @Test void emptyOptionsCannotBeSaved(){assertThrows(BusinessException.class,()->service.buildQuestion(dto("","显示器")));}
 @Test void duplicateLabelsCannotBeSaved(){var d=dto("键盘","显示器");d.getOptions().get(1).setKey("A");assertThrows(BusinessException.class,()->service.buildQuestion(d));}
 @Test void answerMustExist(){var d=dto("键盘","显示器");d.setAnswer("D");assertThrows(BusinessException.class,()->service.buildQuestion(d));}
 @Test void validChoiceRetainsOptionsAndAnswer()throws Exception{var q=service.buildQuestion(dto("键盘","显示器"));assertEquals("键盘",mapper.readTree(q.getOptions()).get(0).path("text").asText());assertEquals("A",q.getAnswer());}
}

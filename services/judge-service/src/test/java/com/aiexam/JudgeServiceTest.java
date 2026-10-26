package com.aiexam;
import com.aiexam.entity.Question;
import com.aiexam.judge.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
class JudgeServiceTest {
 private final JudgeFactory factory=new JudgeFactory(new SingleJudgeStrategy(),new MultiJudgeStrategy(),new JudgeJudgeStrategy(),new FillJudgeStrategy());
 @ParameterizedTest
 @CsvSource(delimiter=';',value={"SINGLE;A;a;true","SINGLE;A;B;false","MULTI;A,C;C,A,A;true","MULTI;A,C;A;false","MULTI;A,C;A,B,C;false","JUDGE;true; TRUE ;true","JUDGE;false;true;false","FILL;网关|gateway; GATEWAY ;true","FILL;Redis;mysql;false"})
 void grade(String type,String expected,String actual,boolean correct){Question q=new Question();q.setType(type);q.setAnswer(expected);assertEquals(correct,factory.get(type).judge(q,actual));}
 @Test void missingAnswerIsWrong(){Question q=new Question();q.setAnswer("A");assertFalse(factory.get("SINGLE").judge(q,null));}
 @Test void rejectUnsupportedType(){assertThrows(RuntimeException.class,()->factory.get("PROGRAMMING"));}
}

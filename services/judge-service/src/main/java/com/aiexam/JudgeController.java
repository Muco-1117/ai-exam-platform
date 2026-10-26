package com.aiexam;
import com.aiexam.entity.Question;
import com.aiexam.judge.JudgeFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
@RestController @RequiredArgsConstructor
public class JudgeController {
 private final JudgeFactory factory;
 private final AtomicLong requests = new AtomicLong();
 public record Item(@Pattern(regexp="SINGLE|MULTI|JUDGE|FILL") String type, @NotBlank String answer, String userAnswer) {}
 public record Batch(@NotEmpty @Size(max=500) List<@Valid Item> items) {}
 @PostMapping("/internal/judge")
 public Map<String,Object> judge(@Valid @RequestBody Batch body) {
  requests.incrementAndGet();
  var results=body.items().stream().map(i->{ Question q=new Question(); q.setType(i.type()); q.setAnswer(i.answer()); return Map.of("correct",factory.get(i.type()).judge(q,i.userAnswer())); }).toList();
  return Map.of("results",results,"service","judge-service");
 }
 @GetMapping("/health") public Map<String,Object> health(){return Map.of("service","judge-service","status","UP","version","2.0.0","requests",requests.get(),"database","无状态计算，不直接访问业务库");}
}

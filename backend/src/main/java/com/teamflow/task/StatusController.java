package com.teamflow.task;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
public class StatusController {
    public record StatusView(TaskStatus id, String label) {}
    @GetMapping("/api/statuses")
    public List<StatusView> statuses() { return Arrays.stream(TaskStatus.values()).map(s -> new StatusView(s, s.getLabel())).toList(); }
}

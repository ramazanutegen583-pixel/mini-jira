package com.teamflow.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static com.teamflow.task.TaskDtos.*;

@RestController
@RequestMapping("/api/tasks")
@Validated
public class TaskController {
    private final TaskService tasks;
    public TaskController(TaskService tasks) { this.tasks = tasks; }
    @GetMapping
    public TaskPage list(@RequestParam(defaultValue="0") @Min(0) int page, @RequestParam(defaultValue="100") @Min(1) @Max(100) int size) {
        return tasks.list(page, size);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public View create(@Valid @RequestBody Create request, @AuthenticationPrincipal Jwt jwt) {
        return tasks.create(request, UUID.fromString(jwt.getSubject()));
    }
    @PutMapping("/{id}")
    public View update(@PathVariable UUID id, @Valid @RequestBody Update request, @AuthenticationPrincipal Jwt jwt) {
        return tasks.update(id, request, UUID.fromString(jwt.getSubject()));
    }
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public View move(@PathVariable UUID id, @Valid @RequestBody Move request, @AuthenticationPrincipal Jwt jwt) {
        return tasks.move(id, request, UUID.fromString(jwt.getSubject()));
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id, @RequestParam @Min(0) long version, @AuthenticationPrincipal Jwt jwt) {
        tasks.delete(id, version, UUID.fromString(jwt.getSubject()));
    }
}

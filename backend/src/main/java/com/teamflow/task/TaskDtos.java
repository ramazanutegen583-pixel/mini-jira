package com.teamflow.task;

import com.teamflow.user.UserController.UserView;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public final class TaskDtos {
    private TaskDtos() {}
    public record Create(@NotBlank @Size(max=160) String title, @NotNull @Size(max=4000) String description,
                         @NotNull Priority priority, UUID assigneeId, LocalDate dueDate) {}
    public record Update(@NotBlank @Size(max=160) String title, @NotNull @Size(max=4000) String description,
                         @NotNull Priority priority, UUID assigneeId, LocalDate dueDate, @NotNull @PositiveOrZero Long version) {}
    public record Move(@NotNull TaskStatus status, @NotNull @PositiveOrZero Long version) {}
    public record View(UUID id, String title, String description, TaskStatus status, Priority priority,
                       UserView assignee, UserView reporter, LocalDate dueDate, Instant createdAt, Instant updatedAt, long version) {
        public static View of(Task task) {
            return new View(task.getId(), task.getTitle(), task.getDescription(), task.getStatus(), task.getPriority(),
                task.getAssignee() == null ? null : UserView.of(task.getAssignee()), UserView.of(task.getReporter()),
                task.getDueDate(), task.getCreatedAt(), task.getUpdatedAt(), task.getVersion());
        }
    }
    public record TaskPage(List<View> content, long totalElements, int totalPages, int number) {}
}

package com.teamflow.task;

import com.teamflow.user.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

class TaskBuilderTest {
    private final AppUser reporter = new AppUser("Tester", "test@example.com", "hash", Role.USER);
    @Test void buildsComplexTaskWithDefaultsAndOptionalFields() {
        Task task = Task.builder("  Release  ", reporter).description("Ship v1")
            .priority(Priority.HIGH).assignee(reporter).dueDate(LocalDate.of(2027, 1, 1)).build();
        assertThat(task.getTitle()).isEqualTo("Release");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(task.getAssignee()).isEqualTo(reporter);
        assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(task.getId()).isNotNull();
        assertThat(task.getCreatedAt()).isEqualTo(task.getUpdatedAt());
    }
    @Test void rejectsInvalidDomainObjects() {
        assertThatThrownBy(() -> Task.builder(" ", reporter).build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Task.builder("x".repeat(161), reporter).build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Task.builder("Valid", null).build()).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Task.builder("Valid", reporter).description("x".repeat(4001)).build()).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void reusingBuilderDoesNotAliasEntities() {
        Task.Builder builder = Task.builder("First", reporter);
        Task first = builder.build(); Task second = builder.status(TaskStatus.DONE).build();
        assertThat(first.getId()).isNotEqualTo(second.getId());
        assertThat(first.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(second.getStatus()).isEqualTo(TaskStatus.DONE);
    }
}

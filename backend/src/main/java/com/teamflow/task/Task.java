package com.teamflow.task;

import com.teamflow.user.AppUser;
import jakarta.persistence.*;
import java.time.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class Task {
    @Id private UUID id;
    @Column(nullable=false, length=160) private String title;
    @Column(nullable=false, length=4000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private TaskStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=16) private Priority priority;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="assignee_id") private AppUser assignee;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="reporter_id") private AppUser reporter;
    private LocalDate dueDate;
    @Column(nullable=false, updatable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    @Version private long version;
    protected Task() {}

    private Task(Builder builder) {
        id = UUID.randomUUID(); title = validTitle(builder.title);
        description = validDescription(builder.description);
        reporter = Objects.requireNonNull(builder.reporter, "reporter is required");
        status = Objects.requireNonNull(builder.status); priority = Objects.requireNonNull(builder.priority);
        assignee = builder.assignee; dueDate = builder.dueDate;
        createdAt = updatedAt = Instant.now();
    }
    public static Builder builder(String title, AppUser reporter) { return new Builder(title, reporter); }

    // Required parameters enter through the constructor; optional values have safe defaults.
    public static final class Builder {
        private final String title;
        private final AppUser reporter;
        private String description = "";
        private TaskStatus status = TaskStatus.TODO;
        private Priority priority = Priority.MEDIUM;
        private AppUser assignee;
        private LocalDate dueDate;
        private Builder(String title, AppUser reporter) { this.title = title; this.reporter = reporter; }
        public Builder description(String value) { description = value; return this; }
        public Builder status(TaskStatus value) { status = value; return this; }
        public Builder priority(Priority value) { priority = value; return this; }
        public Builder assignee(AppUser value) { assignee = value; return this; }
        public Builder dueDate(LocalDate value) { dueDate = value; return this; }
        public Task build() { return new Task(this); }
    }
    private static String validTitle(String title) {
        if (title == null || title.isBlank() || title.trim().length() > 160) throw new IllegalArgumentException("Invalid task title");
        return title.trim();
    }
    private static String validDescription(String description) {
        if (description == null || description.length() > 4000) throw new IllegalArgumentException("Invalid task description");
        return description;
    }
    public void update(String title, String description, Priority priority, AppUser assignee, LocalDate dueDate) {
        this.title = validTitle(title); this.description = validDescription(description);
        this.priority = Objects.requireNonNull(priority); this.assignee = assignee; this.dueDate = dueDate;
        updatedAt = Instant.now();
    }
    public void moveTo(TaskStatus status) { this.status = Objects.requireNonNull(status); updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TaskStatus getStatus() { return status; }
    public Priority getPriority() { return priority; }
    public AppUser getAssignee() { return assignee; }
    public AppUser getReporter() { return reporter; }
    public LocalDate getDueDate() { return dueDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}

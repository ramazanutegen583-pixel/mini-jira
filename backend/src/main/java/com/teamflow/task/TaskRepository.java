package com.teamflow.task;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.UUID;
public interface TaskRepository extends JpaRepository<Task, UUID> {
    @EntityGraph(attributePaths={"assignee", "reporter"})
    Page<Task> findAll(Pageable pageable);
}

package com.teamflow.task;

import com.teamflow.common.ApiException;
import com.teamflow.user.*;
import org.slf4j.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static com.teamflow.task.TaskDtos.*;

@Service
@Transactional
public class TaskService {
    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private final TaskRepository tasks;
    private final UserRepository users;
    public TaskService(TaskRepository tasks, UserRepository users) { this.tasks = tasks; this.users = users; }
    @Transactional(readOnly=true)
    public TaskPage list(int page, int size) {
        var result = tasks.findAll(PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"))));
        return new TaskPage(result.map(View::of).getContent(), result.getTotalElements(), result.getTotalPages(), page);
    }
    public View create(Create request, UUID actor) {
        Task task = Task.builder(request.title(), user(actor)).description(request.description())
            .priority(request.priority()).assignee(assignee(request.assigneeId())).dueDate(request.dueDate()).build();
        tasks.saveAndFlush(task);
        log.info("Task created taskId={} actor={}", task.getId(), actor);
        return View.of(task);
    }
    public View update(UUID id, Update request, UUID actor) {
        Task task = task(id); checkVersion(task, request.version());
        task.update(request.title(), request.description(), request.priority(), assignee(request.assigneeId()), request.dueDate());
        tasks.flush(); log.info("Task updated taskId={} actor={}", id, actor);
        return View.of(task);
    }
    public View move(UUID id, Move request, UUID actor) {
        Task task = task(id); checkVersion(task, request.version());
        TaskStatus previous = task.getStatus(); task.moveTo(request.status()); tasks.flush();
        log.info("Task moved taskId={} from={} to={} actor={}", id, previous, request.status(), actor);
        return View.of(task);
    }
    public void delete(UUID id, long version, UUID actor) {
        Task task = task(id); checkVersion(task, version); tasks.delete(task); tasks.flush();
        log.info("Task deleted taskId={} actor={}", id, actor);
    }
    private void checkVersion(Task task, long version) {
        if (task.getVersion() != version) throw new ApiException(HttpStatus.CONFLICT, "Задача уже изменена. Обновите доску и повторите действие.");
    }
    private Task task(UUID id) { return tasks.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Задача не найдена")); }
    private AppUser user(UUID id) { return users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Участник не найден")); }
    private AppUser assignee(UUID id) { return id == null ? null : user(id); }
}

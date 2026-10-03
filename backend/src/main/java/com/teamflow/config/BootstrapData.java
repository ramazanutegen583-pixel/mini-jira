package com.teamflow.config;

import com.teamflow.task.*;
import com.teamflow.user.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Locale;

@Component
public class BootstrapData implements CommandLineRunner {
    private final UserRepository users; private final TaskRepository tasks;
    private final PasswordEncoder passwords; private final Environment environment;
    private final String email; private final String password;
    public BootstrapData(UserRepository users, TaskRepository tasks, PasswordEncoder passwords, Environment environment,
            @Value("${app.bootstrap.email}") String email, @Value("${app.bootstrap.password}") String password) {
        this.users=users; this.tasks=tasks; this.passwords=passwords; this.environment=environment; this.email=email; this.password=password;
    }
    @Override @Transactional
    public void run(String... args) {
        if (email.isBlank() || users.existsByEmail(email.toLowerCase(Locale.ROOT))) return;
        if (password.length() < 12 || password.length() > 64) throw new IllegalArgumentException("ADMIN_PASSWORD must be 12–64 characters");
        var admin = users.save(new AppUser("Рамазан Утеген", email.toLowerCase(Locale.ROOT), passwords.encode(password), Role.ADMIN));
        if (!environment.acceptsProfiles(Profiles.of("dev"))) return;
        var designer = users.save(new AppUser("Алина Ким", "alina@teamflow.local", passwords.encode("User-demo-2026!"), Role.USER));
        var engineer = users.save(new AppUser("Данил Соколов", "danil@teamflow.local", passwords.encode("User-demo-2026!"), Role.USER));
        String[] titles = {"Спроектировать страницу команды", "Добавить фильтры на доску", "Настроить уведомления", "Реализовать JWT-авторизацию", "Собрать библиотеку компонентов", "Подключить базу данных", "Описать пользовательские сценарии", "Настроить CI-пайплайн"};
        String[] descriptions = {"Профили участников, роли и приглашения. Подготовить адаптивную версию для небольших экранов.", "Поиск по названию, фильтрация по исполнителю и приоритету.", "Показывать понятные сообщения при изменениях задач и ошибках сети.", "Защищённые маршруты, проверка токена и разграничение прав доступа.", "Единые кнопки, поля ввода и модальные окна для всего приложения.", "Миграции Flyway и индексы для быстрого доступа к задачам.", "Зафиксировать основные действия участника и администратора.", "Автоматическая сборка и запуск тестов при каждом pull request."};
        for (int i=0; i<titles.length; i++) {
            tasks.save(Task.builder(titles[i], admin).description(descriptions[i])
                .status(i<3 ? TaskStatus.TODO : i<6 ? TaskStatus.IN_PROGRESS : TaskStatus.DONE)
                .priority(i%3==0 ? Priority.HIGH : i%3==1 ? Priority.MEDIUM : Priority.LOW)
                .assignee(i%3==0 ? admin : i%3==1 ? designer : engineer)
                .dueDate(LocalDate.now().plusDays(i+1)).build());
        }
    }
}

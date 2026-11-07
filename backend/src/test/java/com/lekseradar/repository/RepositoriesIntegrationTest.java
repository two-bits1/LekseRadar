package com.lekseradar.repository;

import com.lekseradar.dto.auth.AuthResponse;
import com.lekseradar.dto.auth.LoginRequest;
import com.lekseradar.dto.auth.RegisterRequest;
import com.lekseradar.model.*;
import com.lekseradar.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Все сущности и репозитории на настоящей базе из database/ (миграции V1–V16, без пробных данных).
 * Запуск: TEST_DB_URL=jdbc:postgresql://localhost:5432/lekseradar TEST_DB_USER=… TEST_DB_PASSWORD=… mvn test
 * Каждый тест в транзакции с откатом: в базе ничего не остаётся.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${TEST_DB_URL}",
        "spring.datasource.username=${TEST_DB_USER:lekseradar}",
        "spring.datasource.password=${TEST_DB_PASSWORD:lekseradar}",
        "app.jwt.secret=integration-test-secret-integration-test",
        "app.openai.api-key=sk-not-used",
})
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
@Transactional
class RepositoriesIntegrationTest {

    private static final String QUIZ_JSON = """
            {"subject": "Matematikk", "topic": "Lineære funksjoner", "level": "beginner", "language": "nb",
             "title": "Quiz: Lineære funksjoner", "model": "test",
             "questions": [
               {"type": "single_choice", "question": "Stigningstallet i y = -4x + 1?",
                "options": ["-4", "1", "4"], "correct_answer": "-4", "explanation": "Tallet foran x."},
               {"type": "multiple_choice", "question": "Hvilke er synkende?",
                "options": ["y = -2x", "y = 3x", "y = -x"], "correct_answer": ["y = -2x", "y = -x"]},
               {"type": "text", "question": "Hva betyr negativt stigningstall?",
                "correct_answer": "Grafen synker."}
             ]}
            """;

    @Autowired AuthService auth;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired UserEventRepository events;
    @Autowired ChatSessionRepository chats;
    @Autowired ChatMessageRepository messages;
    @Autowired PlatformConnectionRepository platforms;
    @Autowired AssignmentRepository assignments;
    @Autowired SubjectRepository subjects;
    @Autowired QuizSetRepository quizSets;
    @Autowired ExerciseRepository exercises;
    @Autowired QuizRepository quizzes;
    @Autowired QuizQuestionRepository quizQuestions;
    @Autowired ExerciseAttemptRepository attempts;
    @Autowired FlashcardRepository flashcards;
    @Autowired MaterialRepository materials;
    @Autowired MaterialChunkRepository chunks;
    @Autowired AiPromptRepository prompts;
    @Autowired ChatModePromptRepository modePrompts;
    @Autowired ReminderRepository reminders;
    @Autowired AchievementRepository achievements;
    @Autowired UserAchievementRepository userAchievements;

    private Long newUser() {
        String email = "it-" + System.nanoTime() + "@example.com";
        AuthResponse r = auth.register(new RegisterRequest(email, "password123", "Test Elev"));
        assertThat(r.token()).isNotBlank();
        assertThat(auth.login(new LoginRequest(email.toUpperCase(), "password123")).user().id())
                .isEqualTo(r.user().id());
        return r.user().id();
    }

    @Test
    void registrationLoginAndEvents() {
        Long userId = newUser();
        assertThat(users.findById(userId)).get().extracting(User::getLastLoginAt).isNotNull();
        assertThat(events.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 50))).isNotEmpty();
        assertThat(users.findDashboard(userId)).isPresent();
    }

    @Test
    void homeworkAndPlatforms() {
        Long userId = newUser();
        PlatformConnection pc = platforms.save(new PlatformConnection(userId, Platform.CANVAS, new byte[]{1, 2, 3}));
        pc.markSynced(true, null, false);
        platforms.flush();
        assertThat(platforms.findByUserIdAndPlatform(userId, Platform.CANVAS)).isPresent();

        Instant due = Instant.now().plus(2, ChronoUnit.DAYS);
        assertThat(assignments.upsert(userId, "canvas", "5001", "Lab 3", "IT1", due, "todo", null, "", "assignment")).isEqualTo(1);
        assignments.upsert(userId, "canvas", "5001", "Lab 3 (ny)", "IT1", due, "submitted", null, "", "assignment");
        var list = assignments.findForUser(userId);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getTitle()).isEqualTo("Lab 3 (ny)");
        assertThat(list.get(0).getStatus()).isEqualTo(AssignmentStatus.SUBMITTED);

        Long assignmentId = list.get(0).getId();
        assertThat(assignments.attachHomeworkFile(assignmentId, "Oppgave 1: løs likningen 2x + 3 = 7.",
                "lab3.txt", "text/plain", null)).isPositive();

        Reminder r = reminders.save(new Reminder(userId, assignmentId, "Lever Lab 3", Instant.now().minusSeconds(60)));
        assertThat(reminders.findDue(Instant.now())).extracting(Reminder::getId).contains(r.getId());

        assertThat(achievements.findAll()).isNotEmpty();
        assertThat(userAchievements.unlock(userId, "connected_all")).isEqualTo(1);
        assertThat(userAchievements.unlock(userId, "connected_all")).isZero();
        assertThat(userAchievements.findByUserIdOrderByUnlockedAt(userId)).hasSize(1);
    }

    @Test
    void chatModesAndPrompts() {
        Long userId = newUser();
        jdbc.queryForObject("SELECT save_prompt('tutor', 'Du er en veileder for {{subject}}.')", Integer.class);

        Long chatId = chats.startChat(userId, "tutor", "Matematikk", "Lineære funksjoner", null, null);
        ChatSession chat = chats.findByIdAndUserId(chatId, userId).orElseThrow();
        assertThat(chat.getMode()).isEqualTo(ChatMode.TUTOR);
        assertThat(chat.getLevel()).isEqualTo(ExerciseLevel.INTERMEDIATE);
        assertThat(subjects.findByName("  matematikk ")).isPresent();

        var sp = chats.systemPrompt(chatId);
        assertThat(sp.getPromptKey()).isEqualTo("tutor");
        assertThat(sp.getSubject()).isEqualTo("Matematikk");
        assertThat(prompts.findByKeyAndActiveTrue("tutor")).isPresent();
        assertThat(prompts.getActiveContent("tutor")).contains("{{subject}}");
        assertThat(modePrompts.findAll()).extracting(ChatModePrompt::getMode).contains(ChatMode.TUTOR, ChatMode.QUIZ);

        chats.setLanguage(chatId, "nb");
        assertThat(chats.promptVars(chatId)).contains("\"language\"");

        ChatMessage m = ChatMessage.fromTutor(chatId, "Hei!", "test", 3);
        m.setPromptId(sp.getPromptId());
        messages.save(m);
        messages.save(ChatMessage.fromStudent(chatId, "Hva er stigningstall?"));
        assertThat(messages.findContext(chatId)).hasSize(2);

        // Свой чат через JPA (как ChatService) тоже работает: режим по умолчанию tutor
        ChatSession own = chats.saveAndFlush(new ChatSession(userId, ChatSession.DEFAULT_TITLE, null));
        assertThat(own.getMode()).isEqualTo(ChatMode.TUTOR);
    }

    @Test
    void quizFlashcardsAndProgress() {
        Long userId = newUser();
        Long setId = quizSets.importJson(QUIZ_JSON, userId);
        assertThat(quizSets.importJson(QUIZ_JSON, userId)).isEqualTo(setId);
        assertThat(quizSets.find("matematikk", "lineære  funksjoner", "beginner", "nb")).isEqualTo(setId);
        QuizSet set = quizSets.findById(setId).orElseThrow();
        assertThat(set.getSourceJson().get("questions").size()).isEqualTo(3);
        assertThat(set.getMaterialIds()).isEmpty();

        var questions = exercises.findByQuizSetIdOrderByPosition(setId);
        assertThat(questions).extracting(Exercise::getQuestionType)
                .containsExactly(QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE, QuestionType.TEXT);
        assertThat(questions.get(0).getOptions().isArray()).isTrue();

        Long quizId = quizzes.start(userId, setId);
        assertThat(quizzes.start(userId, setId)).isEqualTo(quizId);
        assertThat(quizQuestions.findByQuizIdOrderByPosition(quizId)).hasSize(3);
        var pub = quizzes.findPublicQuestions(quizId);
        assertThat(pub).hasSize(3);
        assertThat(pub.get(0).getOptions()).contains("-4");

        assertThat(quizzes.saveAnswer(quizId, 1, "-4", null, null).getVerdict()).isEqualTo("correct");
        assertThat(quizzes.saveAnswer(quizId, 2, "[\"y = 3x\"]", null, null).getVerdict()).isEqualTo("incorrect");
        assertThat(quizzes.saveAnswer(quizId, 3, "Den synker", "partial", "Nesten").getVerdict()).isEqualTo("partial");
        assertThat(quizzes.finish(quizId)).isNotNull();
        Quiz quiz = quizzes.findByIdAndUserId(quizId, userId).orElseThrow();
        assertThat(quiz.getStatus()).isEqualTo(QuizStatus.COMPLETED);
        assertThat(attempts.findByQuizIdOrderByCreatedAt(quizId)).extracting(ExerciseAttempt::getVerdict)
                .containsExactly(Verdict.CORRECT, Verdict.INCORRECT, Verdict.PARTIAL);
        assertThat(quizzes.findProgress(userId)).isNotEmpty();
        assertThat(users.findTopicProgress(userId)).isNotEmpty();
        assertThat(users.findDailyActivity(userId)).isNotEmpty();

        // Ошибки квиза сами попали в колоду
        assertThat(flashcards.findDueCards(userId)).hasSize(2);
        assertThat(flashcards.addQuizSet(userId, setId)).isEqualTo(1);
        assertThat(flashcards.findCards(userId)).hasSize(3);
        var review = flashcards.review(userId, questions.get(0).getId(), 5);
        assertThat(review.getNextReviewAt()).isAfter(Instant.now());
        assertThat(flashcards.findAll()).isNotEmpty();
        assertThat(flashcards.findStats(userId)).get().extracting(FlashcardRepository.Stats::getCards).isEqualTo(3L);
    }

    @Test
    void materialsSearch() {
        Long userId = newUser();
        Long id = materials.add(userId, "Naturfag", "Fotosyntese", "Kapittel 4",
                "Fotosyntesen skjer i kloroplastene. Planter bruker lys, vann og karbondioksid.",
                "kap4.txt", "text/plain", null);
        Material m = materials.findVisible(id, userId).orElseThrow();
        assertThat(m.getSource()).isEqualTo(MaterialSource.UPLOAD);
        assertThat(chunks.findByMaterialIdOrderByPosition(id)).isNotEmpty();
        assertThat(materials.findVisible(userId)).extracting(Material::getId).contains(id);
        assertThat(materials.search(userId, "kloroplastene", null, 5, null)).isNotEmpty();
        assertThat(materials.forPrompt(userId, "kloroplastene", null, 5, null)).contains("Fotosyntesen");
    }
}

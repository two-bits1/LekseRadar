package com.lekseradar.exercise;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "exercises")
public class Exercise {
    public enum Type { CHOICE, OPEN }
    public enum Difficulty { EASY, MEDIUM, HARD }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> options;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String solution;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Exercise() {}

    public Exercise(Long userId, String subject, String topic, Difficulty difficulty, Type type,
                    String question, List<String> options, String solution) {
        this.userId = userId;
        this.subject = subject;
        this.topic = topic;
        this.difficulty = difficulty;
        this.type = type;
        this.question = question;
        this.options = options;
        this.solution = solution;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getSubject() { return subject; }
    public String getTopic() { return topic; }
    public Difficulty getDifficulty() { return difficulty; }
    public Type getType() { return type; }
    public String getQuestion() { return question; }
    public List<String> getOptions() { return options; }
    public String getSolution() { return solution; }
    public Instant getCreatedAt() { return createdAt; }
}

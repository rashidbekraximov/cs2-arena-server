package uz.cs2arena.news;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "news")
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String category;
    private String title;
    private String body;
    private boolean published = true;
    private Long authorId;
    private Instant createdAt = Instant.now();

    protected News() {}

    public News(String category, String title, String body, boolean published, Long authorId) {
        update(category, title, body, published);
        this.authorId = authorId;
    }

    public void update(String category, String title, String body, boolean published) {
        this.category = category;
        this.title = title;
        this.body = body;
        this.published = published;
    }

    public Long getId() { return id; }
    public String getCategory() { return category; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public boolean isPublished() { return published; }
    public Long getAuthorId() { return authorId; }
    public Instant getCreatedAt() { return createdAt; }
}

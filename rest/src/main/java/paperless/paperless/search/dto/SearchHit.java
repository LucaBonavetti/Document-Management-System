package paperless.paperless.search.dto;

public class SearchHit {

    private Long id;
    private Double score;

    public SearchHit() { }

    public SearchHit(Long id, Double score) {
        this.id = id;
        this.score = score;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}

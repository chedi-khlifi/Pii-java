package example;

public class Exam {
    private int id;
    private String title;
    private String description;
    private String examDate;
    private int durationMinutes;
    private String location;
    private int importance;
    private int ownerId;

    public Exam(int id, String title, String description, String examDate, int durationMinutes, String location, int importance, int ownerId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.examDate = examDate;
        this.durationMinutes = durationMinutes;
        this.location = location;
        this.importance = importance;
        this.ownerId = ownerId;
    }

    public int getId()               { return id; }
    public String getTitle()         { return title; }
    public String getDescription()   { return description; }
    public String getExamDate()      { return examDate; }
    public int getDurationMinutes()  { return durationMinutes; }
    public String getLocation()      { return location; }
    public int getImportance()       { return importance; }
    public int getOwnerId()          { return ownerId; }

    public void setTitle(String title)               { this.title = title; }
    public void setDescription(String description)   { this.description = description; }
    public void setExamDate(String examDate)         { this.examDate = examDate; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setLocation(String location)         { this.location = location; }
    public void setImportance(int importance)        { this.importance = importance; }
}
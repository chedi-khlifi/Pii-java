package com.mindforge.entities.carriere;

import java.time.LocalDateTime;

public class Entreprise {

    private int id;
    private String name;
    private String description;
    private String industry;
    private String contactEmail;
    private int contactPhone;
    private String website;
    private LocalDateTime createdAt;

    public Entreprise() {}

    public Entreprise(String name, String description, String industry,
                      String contactEmail, int contactPhone, String website) {
        this.name = name;
        this.description = description;
        this.industry = industry;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.website = website;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public int getContactPhone() { return contactPhone; }
    public void setContactPhone(int contactPhone) { this.contactPhone = contactPhone; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Entreprise{id=" + id + ", name='" + name + "', industry='" + industry +
               "', contactEmail='" + contactEmail + "', website='" + website + "'}";
    }
}

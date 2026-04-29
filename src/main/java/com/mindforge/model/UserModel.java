package com.mindforge.model;

import javafx.beans.property.*;

public class UserModel {

    private final IntegerProperty id;
    private final StringProperty  email;
    private final StringProperty  verified;
    private final StringProperty  createdAt;

    public UserModel(int id, String email, String verified, String createdAt) {
        this.id        = new SimpleIntegerProperty(id);
        this.email     = new SimpleStringProperty(email);
        this.verified  = new SimpleStringProperty(verified);
        this.createdAt = new SimpleStringProperty(createdAt);
    }

    public IntegerProperty idProperty()        { return id; }
    public StringProperty  emailProperty()     { return email; }
    public StringProperty  verifiedProperty()  { return verified; }
    public StringProperty  createdAtProperty() { return createdAt; }

    public int    getId()        { return id.get(); }
    public String getEmail()     { return email.get(); }
    public String getVerified()  { return verified.get(); }
    public String getCreatedAt() { return createdAt.get(); }
}

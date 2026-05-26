package com.mem0.core.message;

import java.util.Date;

public class MessageEntity {

    private String id;
    private String sessionScope;
    private String role;
    private String content;
    private String name;
    private Date createdAt;

    public MessageEntity() {
    }

    public MessageEntity(String id, String sessionScope, String role, String content, String name, Date createdAt) {
        this.id = id;
        this.sessionScope = sessionScope;
        this.role = role;
        this.content = content;
        this.name = name;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionScope() {
        return sessionScope;
    }

    public void setSessionScope(String sessionScope) {
        this.sessionScope = sessionScope;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}

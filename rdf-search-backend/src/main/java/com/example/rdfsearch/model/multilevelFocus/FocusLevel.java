
package com.example.rdfsearch.model.multilevelFocus;

public class FocusLevel {
    private String id;
    private String name;
    private int level;
    private String description;

    // 构造函数
    public FocusLevel() {
    }

    public FocusLevel(String id, String name, int level) {
        this.id = id;
        this.name = name;
        this.level = level;
    }

    // getter/setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
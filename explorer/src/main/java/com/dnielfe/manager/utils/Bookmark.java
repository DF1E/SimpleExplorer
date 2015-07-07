package com.dnielfe.manager.utils;

public class Bookmark {

    private int id;
    private String name;
    private String path;

    public Bookmark() {
    }

    public Bookmark(String title, String author) {
        super();
        this.name = title;
        this.path = author;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String title) {
        this.name = title;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String author) {
        this.path = author;
    }

    @Override
    public String toString() {
        return "Bookmark [id=" + id + ", name=" + name + ", path=" + path + "]";
    }
}

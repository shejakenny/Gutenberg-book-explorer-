package com.student;

/** Represents one book retrieved from Project Gutenberg. */
public class Book {
    private final String title;
    private final String author;
    private final String url;

    public Book(String title, String author, String url) {
        this.title = title;
        this.author = author;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getUrl() {
        return url;
    }
}

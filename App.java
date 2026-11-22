package com.student;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Gutenberg Book Explorer
 * Java 17+ / Maven / JSoup
 */
public class App {
    private static final String TOP_BOOKS_URL =
            "https://www.gutenberg.org/browse/scores/top";
    private static final String SEARCH_URL =
            "https://www.gutenberg.org/ebooks/search/?query=";

    // Fixed-size arrays are used as required by the assignment (no ArrayList).
    private static final Book[] currentBooks = new Book[100];
    private static int currentBookCount = 0;

    private static final Book[] readingList = new Book[20];
    private static int readingListCount = 0;

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        int choice;

        System.out.println("Welcome to Gutenberg Book Explorer!");

        do {
            showMenu();
            choice = readInt("Enter your choice: ");

            if (choice == 1) {
                exploreBooks();
            } else if (choice == 2) {
                System.out.print("Enter book title or keyword: ");
                searchBook(scanner.nextLine().trim());
            } else if (choice == 3) {
                System.out.print("Enter author name or keyword: ");
                searchBook(scanner.nextLine().trim(), true);
            } else if (choice == 4) {
                addBook();
            } else if (choice == 5) {
                viewReadingList();
            } else if (choice == 6) {
                removeBook();
            } else if (choice == 7) {
                displayBookDetails();
            } else if (choice == 0) {
                System.out.println("Thank you for using Gutenberg Book Explorer.");
            } else {
                System.out.println("Invalid choice. Please choose a number from 0 to 7.");
            }

            System.out.println();
        } while (choice != 0);

        scanner.close();
    }

    private static void showMenu() {
        System.out.println("===== GUTENBERG BOOK EXPLORER =====");
        System.out.println("1. Explore Books");
        System.out.println("2. Search by Title");
        System.out.println("3. Search by Author");
        System.out.println("4. Add Book to Reading List");
        System.out.println("5. View Reading List");
        System.out.println("6. Remove Book");
        System.out.println("7. Display Book Details");
        System.out.println("0. Exit");
    }

    // Part B: retrieve at least 10 books from the Project Gutenberg top-books page.
    private static void exploreBooks() {
        System.out.println("\nLoading popular books from Project Gutenberg...");
        try {
            Document doc = Jsoup.connect(TOP_BOOKS_URL)
                    .userAgent("Mozilla/5.0 (compatible; GutenbergBookExplorer/1.0)")
                    .timeout(15000)
                    .get();

            currentBookCount = 0;
            extractBooks(doc, currentBooks, 30);

            if (currentBookCount == 0) {
                System.out.println("No books were found. The website structure may have changed.");
                return;
            }

            System.out.println("\nPOPULAR BOOKS");
            printBooks(currentBooks, currentBookCount);
            System.out.println("Loaded " + currentBookCount + " book(s).");
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
            System.out.println("Check your internet connection and try again.");
        }
    }

    // Mandatory overloaded method 1: search by title.
    private static void searchBook(String title) {
        searchOnline(title, false);
    }

    // Mandatory overloaded method 2: search by author when searchByAuthor is true.
    private static void searchBook(String keyword, boolean searchByAuthor) {
        searchOnline(keyword, searchByAuthor);
    }

    private static void searchOnline(String keyword, boolean byAuthor) {
        if (keyword == null || keyword.isBlank()) {
            System.out.println("Search keyword cannot be empty.");
            return;
        }

        String type = byAuthor ? "author" : "title";
        System.out.println("\nSearching Gutenberg by " + type + ": " + keyword);

        try {
            String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            Document doc = Jsoup.connect(SEARCH_URL + encoded)
                    .userAgent("Mozilla/5.0 (compatible; GutenbergBookExplorer/1.0)")
                    .timeout(15000)
                    .get();

            currentBookCount = 0;
            extractBooks(doc, currentBooks, 100);

            // The Gutenberg search page searches its indexed metadata. Apply a
            // local check as well so the displayed results match the selected field.
            int displayed = 0;
            for (int i = 0; i < currentBookCount; i++) {
                Book book = currentBooks[i];
                String value = byAuthor ? book.getAuthor() : book.getTitle();
                if (value.toLowerCase().contains(keyword.toLowerCase())) {
                    displayed++;
                }
            }

            if (displayed == 0) {
                System.out.println("No matching books were found.");
                currentBookCount = 0;
                return;
            }

            // Keep only the matching books in the current selection array.
            Book[] matches = new Book[100];
            int matchCount = 0;
            for (int i = 0; i < currentBookCount; i++) {
                Book book = currentBooks[i];
                String value = byAuthor ? book.getAuthor() : book.getTitle();
                if (value.toLowerCase().contains(keyword.toLowerCase())) {
                    matches[matchCount++] = book;
                }
            }

            currentBookCount = matchCount;
            for (int i = 0; i < matchCount; i++) {
                currentBooks[i] = matches[i];
            }

            System.out.println("\nSEARCH RESULTS");
            printBooks(currentBooks, currentBookCount);
            System.out.println("Found " + currentBookCount + " matching book(s).");
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
            System.out.println("Check your internet connection and try again.");
        }
    }

    /**
     * Extracts unique Gutenberg ebook links from a document.
     * Gutenberg's page layout can change, so author extraction checks the
     * link's surrounding text and falls back to "Unknown author".
     */
    private static void extractBooks(Document doc, Book[] destination, int limit) {
        Elements links = doc.select("a[href^=/ebooks/]");
        int count = 0;

        for (Element link : links) {
            String href = link.attr("href");
            if (!href.matches("/ebooks/\\d+/?")) {
                continue; // Ignore feeds, search links and other non-book links.
            }

            String title = cleanText(link.text());
            if (title.isBlank()) {
                continue;
            }

            String url = link.absUrl("href");
            if (url.isBlank()) {
                url = "https://www.gutenberg.org" + href;
            }

            if (containsUrl(destination, count, url)) {
                continue;
            }

            String context = findContextText(link);
            String author = extractAuthor(context, title);

            destination[count++] = new Book(title, author, url);
            if (count >= limit || count >= destination.length) {
                break;
            }
        }

        if (destination == currentBooks) {
            currentBookCount = count;
        }
    }

    private static String findContextText(Element link) {
        Element parent = link;
        String best = link.text();

        // Walk a few levels up to include the author text beside the link.
        for (int level = 0; level < 4 && parent != null; level++) {
            String candidate = cleanText(parent.text());
            if (!candidate.isBlank()) {
                best = candidate;
            }
            if (candidate.toLowerCase().contains(" by ")) {
                return candidate;
            }
            parent = parent.parent();
        }
        return best;
    }

    private static String extractAuthor(String context, String title) {
        String remaining = context.replace(title, " ").trim();
        Matcher matcher = Pattern.compile("(?i)\\bby\\s+(.+)$").matcher(remaining);
        if (matcher.find()) {
            String author = cleanText(matcher.group(1));
            // Remove common trailing catalogue annotations if present.
            author = author.replaceAll("\\s*\\([^)]*\\)\\s*$", "").trim();
            if (!author.isBlank()) {
                return author;
            }
        }
        return "Unknown author";
    }

    private static String cleanText(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private static boolean containsUrl(Book[] books, int count, String url) {
        for (int i = 0; i < count; i++) {
            if (books[i] != null && books[i].getUrl().equals(url)) {
                return true;
            }
        }
        return false;
    }

    private static void printBooks(Book[] books, int count) {
        for (int i = 0; i < count; i++) {
            System.out.println((i + 1) + ". " + books[i].getTitle()
                    + " — " + books[i].getAuthor());
            System.out.println("   " + books[i].getUrl());
        }
    }

    // Save one of the books currently displayed (explore/search results).
    private static void addBook() {
        if (currentBookCount == 0) {
            System.out.println("There are no displayed books to add. Explore or search first.");
            return;
        }
        if (readingListCount >= readingList.length) {
            System.out.println("Your reading list is full (maximum 20 books).");
            return;
        }

        printBooks(currentBooks, currentBookCount);
        int number = readInt("Enter the book number to add: ");

        if (number < 1 || number > currentBookCount) {
            System.out.println("Invalid book number.");
            return;
        }

        Book selected = currentBooks[number - 1];
        if (isAlreadySaved(selected)) {
            System.out.println("That book is already in your reading list.");
            return;
        }

        readingList[readingListCount++] = selected;
        System.out.println("Book added to your reading list.");
    }

    private static boolean isAlreadySaved(Book selected) {
        for (int i = 0; i < readingListCount; i++) {
            if (readingList[i].getUrl().equals(selected.getUrl())) {
                return true;
            }
        }
        return false;
    }

    // Required: display saved books using a for loop.
    private static void viewReadingList() {
        System.out.println("\n===== MY READING LIST =====");
        if (readingListCount == 0) {
            System.out.println("Your reading list is empty.");
            return;
        }

        for (int i = 0; i < readingListCount; i++) {
            System.out.println((i + 1) + ". " + readingList[i].getTitle()
                    + " — " + readingList[i].getAuthor());
            System.out.println("   " + readingList[i].getUrl());
        }
    }

    private static void removeBook() {
        if (readingListCount == 0) {
            System.out.println("Your reading list is empty.");
            return;
        }

        viewReadingList();
        int number = readInt("Enter the number of the book to remove: ");
        if (number < 1 || number > readingListCount) {
            System.out.println("Invalid book number.");
            return;
        }

        int index = number - 1;
        String removedTitle = readingList[index].getTitle();

        // Shift remaining elements left to fill the deleted position.
        for (int i = index; i < readingListCount - 1; i++) {
            readingList[i] = readingList[i + 1];
        }
        readingList[--readingListCount] = null;

        System.out.println("\"" + removedTitle + "\" was removed.");
    }

    private static void displayBookDetails() {
        if (currentBookCount == 0) {
            System.out.println("No books are currently displayed. Explore or search first.");
            return;
        }

        printBooks(currentBooks, currentBookCount);
        int number = readInt("Enter the book number for details: ");
        if (number < 1 || number > currentBookCount) {
            System.out.println("Invalid book number.");
            return;
        }

        Book book = currentBooks[number - 1];
        System.out.println("\n===== BOOK DETAILS =====");
        System.out.println("Title : " + book.getTitle());
        System.out.println("Author: " + book.getAuthor());
        System.out.println("URL   : " + book.getUrl());
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }
}

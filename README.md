# Gutenberg Book Explorer

Java console assignment using Java 17+, Maven, and JSoup.

## Run in NetBeans
1. Open this folder as an existing Maven project.
2. Allow Maven to download dependencies.
3. Run `com.student.App`.

## Run in VS Code / terminal
From this project folder:

```bash
mvn compile
mvn exec:java -Dexec.mainClass="com.student.App"
```

If the `exec:java` command is unavailable in your setup, run `com.student.App`
from the Java Projects view, or add the Maven Exec Plugin to `pom.xml`.

## Menu
1. Explore popular Gutenberg books
2. Search by title
3. Search by author
4. Add a displayed book to the reading list
5. View reading list
6. Remove a saved book
7. Display book details
0. Exit

The reading list is a fixed array with a maximum of 20 books. Search and explore
results are retrieved online, so an internet connection is required. Gutenberg's
HTML can change; if the website changes its page structure, extraction may need
small selector adjustments.

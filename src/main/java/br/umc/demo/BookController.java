package br.umc.demo;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/books")
public class BookController {

    private List<Book> books = new ArrayList<>();

    public BookController() {
        books.add(new Book(1L, "Clean Code", "Robert C. Martin", 2008));
        books.add(new Book(2L, "The Pragmatic Programmer", "Andrew Hunt", 1999));
    }

    // GET /api/books
    @GetMapping
    public ResponseEntity<List<Book>> listAll() {
        return new ResponseEntity<>(books, HttpStatus.OK);
    }

    // GET /api/books/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Book> findById(@PathVariable Long id) {
        for (Book book : books) {
            if (book.getId().equals(id)) {
                return new ResponseEntity<>(book, HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    // POST /api/books
    @PostMapping
    public ResponseEntity<Book> create(@RequestBody Book newBook) {
        newBook.setId((long) books.size() +1);
        books.add(newBook);
        return new ResponseEntity<>(newBook, HttpStatus.CREATED);
    }

    // PUT /api/books/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Book> update(@PathVariable Long id, @RequestBody Book updatedBook) {
        for (Book book : books) {
            if (book.getId().equals(id)) {
                book.setTitle(updatedBook.getTitle());
                book.setAuthor(updatedBook.getAuthor());
                book.setYear(updatedBook.getYear());
                return new ResponseEntity<>(book, HttpStatus.CREATED);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    // DELETE /api/books/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable Long id) {
        for (Book book : books) {
            if (book.getId().equals(id)) {
                books.remove(book);
                return new ResponseEntity(HttpStatus.NO_CONTENT);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}

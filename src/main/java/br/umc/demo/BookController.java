package br.umc.demo;

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
    public List<Book> listAll() {
        return books;
    }

    // GET /api/books/{id}
    @GetMapping("/{id}")
    public Book findById(@PathVariable Long id) {
        for (Book book : books) {
            if (book.getId().equals(id)) {
                return book;
            }
        }
        return null;
    }

    // POST /api/books
    @PostMapping
    public Book create(@RequestBody Book newBook) {
        newBook.setId((long) books.size() +1);
        books.add(newBook);
        return newBook;
    }

    // PUT /api/books/{id}
    @PutMapping("/{id}")
    public Book update(@PathVariable Long id, @RequestBody Book updatedBook) {
        for (Book book : books) {
            if (book.getId().equals(id)) {
                book.setTitle(updatedBook.getTitle());
                book.setAuthor(updatedBook.getAuthor());
                book.setYear(updatedBook.getYear());
                return book;
            }
        }
        return null;
    }

    // DELETE /api/books/{id}
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        for (Book book : books) {
            if (book.getId().equals(id)) {
                books.remove(book);
                return "Livro com id " + id + " removido com sucesso.";
            }
        }
        return "Livro com id " + id + " não encontrado.";
    }
}

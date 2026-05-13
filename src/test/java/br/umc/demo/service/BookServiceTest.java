package br.umc.demo.service;

import br.umc.demo.dto.BookRequest;
import br.umc.demo.dto.BookResponse;
import br.umc.demo.entity.Author;
import br.umc.demo.entity.Book;
import br.umc.demo.repository.AuthorRepository;
import br.umc.demo.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private BookService bookService;

    private Author autor;
    private Book livro;

    @BeforeEach
    void setUp() {
        autor = new Author("Robert Martin");
        autor.setId(1L);

        livro = new Book();
        livro.setId(1L);
        livro.setTitle("Clean Code");
        livro.setPublisher("Prentice Hall");
        livro.setIsbn("978-0132350884");
        livro.setSummary("Um guia de boas práticas");
        livro.setAuthors(List.of(autor));
    }

    // -------------------------------------------------------
    // listBooks
    // -------------------------------------------------------

    @Test
    void listBooks_deveRetornarTodosOsLivros() {
        // setup
        when(bookRepository.findAll()).thenReturn(List.of(livro));

        // exercise
        List<BookResponse> resultado = bookService.listBooks();

        // verify
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getTitle()).isEqualTo("Clean Code");
    }

    @Test
    void listBooks_comRepositorioVazio_deveRetornarListaVazia() {
        // setup
        when(bookRepository.findAll()).thenReturn(List.of());

        // exercise
        List<BookResponse> resultado = bookService.listBooks();

        // verify
        assertThat(resultado).isEmpty();
    }

    // -------------------------------------------------------
    // getBook
    // -------------------------------------------------------

    @Test
    void getBook_comIdExistente_deveRetornarBookResponse() {
        // setup
        when(bookRepository.findById(1L)).thenReturn(Optional.of(livro));

        // exercise
        BookResponse resultado = bookService.getBook(1L);

        // verify
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getTitle()).isEqualTo("Clean Code");
    }

    @Test
    void getBook_comIdInexistente_deveLancar404() {
        // setup
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        // exercise + verify
        assertThatThrownBy(() -> bookService.getBook(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // -------------------------------------------------------
    // createBook
    // -------------------------------------------------------

    @Test
    void createBook_deveSalvarERetornarComDadosCorretos() {
        // setup
        BookRequest request = buildRequest("Domain-Driven Design", List.of(1L));
        when(authorRepository.findAllById(List.of(1L))).thenReturn(List.of(autor));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> {
            Book b = inv.getArgument(0);
            b.setId(2L);
            return b;
        });

        // exercise
        BookResponse resultado = bookService.createBook(request);

        // verify
        assertThat(resultado.getTitle()).isEqualTo("Domain-Driven Design");
        assertThat(resultado.getAuthors()).hasSize(1);
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void createBook_semAuthorIds_naoDeveChamarFindAllById() {
        // setup
        BookRequest request = buildRequest("Livro Sem Autor", null);
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> {
            Book b = inv.getArgument(0);
            b.setId(3L);
            return b;
        });

        // exercise
        bookService.createBook(request);

        // verify
        verify(authorRepository, never()).findAllById(any());
    }

    // -------------------------------------------------------
    // updateBook
    // -------------------------------------------------------

    @Test
    void updateBook_comIdExistente_deveAtualizarERetornar() {
        // setup
        BookRequest request = buildRequest("Clean Code - 2a Ed", List.of(1L));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(livro));
        when(authorRepository.findAllById(any())).thenReturn(List.of(autor));
        when(bookRepository.save(any(Book.class))).thenReturn(livro);

        // exercise
        BookResponse resultado = bookService.updateBook(1L, request);

        // verify
        assertThat(resultado.getTitle()).isEqualTo("Clean Code - 2a Ed");
        verify(bookRepository).save(livro);
    }

    @Test
    void updateBook_comIdInexistente_deveLancar404() {
        // setup
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        // exercise + verify
        assertThatThrownBy(() -> bookService.updateBook(99L, buildRequest("X", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // -------------------------------------------------------
    // deleteBook
    // -------------------------------------------------------

    @Test
    void deleteBook_comIdExistente_deveExcluir() {
        // setup
        when(bookRepository.existsById(1L)).thenReturn(true);

        // exercise
        bookService.deleteBook(1L);

        // verify
        verify(bookRepository).deleteById(1L);
    }

    @Test
    void deleteBook_comIdInexistente_deveLancar404() {
        // setup
        when(bookRepository.existsById(99L)).thenReturn(false);

        // exercise + verify
        assertThatThrownBy(() -> bookService.deleteBook(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");

        // verify
        verify(bookRepository, never()).deleteById(any());
    }

    // -------------------------------------------------------
    // Helper
    // -------------------------------------------------------

    private BookRequest buildRequest(String title, List<Long> authorIds) {
        BookRequest r = new BookRequest();
        r.setTitle(title);
        r.setAuthorIds(authorIds);
        r.setPublisher("Editora");
        r.setIsbn("000-000");
        r.setSummary("Resumo");
        return r;
    }
}

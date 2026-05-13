package br.umc.demo.service;

import br.umc.demo.dto.AuthorRequest;
import br.umc.demo.dto.AuthorResponse;
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
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private AuthorService authorService;

    private Author autor;

    @BeforeEach
    void setUp() {
        autor = new Author("Martin Fowler");
        autor.setId(1L);
    }

    // -------------------------------------------------------
    // createAuthor
    // -------------------------------------------------------

    @Test
    void createAuthor_deveSalvarERetornarAuthorResponse() {
        // setup
        AuthorRequest request = buildRequest("Martin Fowler");
        when(authorRepository.save(any(Author.class))).thenAnswer(inv -> {
            Author a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        // exercise
        AuthorResponse resultado = authorService.createAuthor(request);

        // verify
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getName()).isEqualTo("Martin Fowler");
        verify(authorRepository).save(any(Author.class));
    }

    // -------------------------------------------------------
    // listAuthors
    // -------------------------------------------------------

    @Test
    void listAuthors_deveRetornarTodosOsAutores() {
        // setup
        when(authorRepository.findAll()).thenReturn(List.of(autor));

        // exercise
        List<AuthorResponse> resultado = authorService.listAuthors();

        // verify
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getName()).isEqualTo("Martin Fowler");
    }

    @Test
    void listAuthors_comRepositorioVazio_deveRetornarListaVazia() {
        // setup
        when(authorRepository.findAll()).thenReturn(List.of());

        // exercise
        List<AuthorResponse> resultado = authorService.listAuthors();

        // verify
        assertThat(resultado).isEmpty();
    }

    // -------------------------------------------------------
    // getAuthor
    // -------------------------------------------------------

    @Test
    void getAuthor_comIdExistente_deveRetornarAuthorResponse() {
        // setup
        when(authorRepository.findById(1L)).thenReturn(Optional.of(autor));

        // exercise
        AuthorResponse resultado = authorService.getAuthor(1L);

        // verify
        assertThat(resultado.getName()).isEqualTo("Martin Fowler");
    }

    @Test
    void getAuthor_comIdInexistente_deveLancar404() {
        // setup
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        // exercise + verify
        assertThatThrownBy(() -> authorService.getAuthor(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // -------------------------------------------------------
    // updateAuthor
    // -------------------------------------------------------

    @Test
    void updateAuthor_comIdExistente_deveAtualizarNome() {
        // setup
        when(authorRepository.findById(1L)).thenReturn(Optional.of(autor));
        when(authorRepository.save(autor)).thenReturn(autor);

        // exercise
        AuthorResponse resultado = authorService.updateAuthor(1L, buildRequest("M. Fowler"));

        // verify
        assertThat(resultado.getName()).isEqualTo("M. Fowler");
        verify(authorRepository).save(autor);
    }

    @Test
    void updateAuthor_comIdInexistente_deveLancar404() {
        // setup
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        // exercise + verify
        assertThatThrownBy(() -> authorService.updateAuthor(99L, buildRequest("X")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // -------------------------------------------------------
    // deleteAuthor
    // -------------------------------------------------------

    @Test
    void deleteAuthor_comIdExistente_deveExcluir() {
        // setup
        when(authorRepository.existsById(1L)).thenReturn(true);

        // exercise
        authorService.deleteAuthor(1L);

        // verify
        verify(authorRepository).deleteById(1L);
    }

    @Test
    void deleteAuthor_comIdInexistente_deveLancar404() {
        // setup
        when(authorRepository.existsById(99L)).thenReturn(false);

        // exercise + verify
        assertThatThrownBy(() -> authorService.deleteAuthor(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");

        // verify
        verify(authorRepository, never()).deleteById(any());
    }

    // -------------------------------------------------------
    // listBooksByAuthor
    // -------------------------------------------------------

    @Test
    void listBooksByAuthor_comAutorExistente_deveRetornarLivrosDoAutor() {
        // setup
        Book livro = new Book();
        livro.setId(10L);
        livro.setTitle("Refactoring");
        livro.setAuthors(List.of(autor));
        when(authorRepository.existsById(1L)).thenReturn(true);
        when(bookRepository.findByAuthors_Id(1L)).thenReturn(List.of(livro));

        // exercise
        List<BookResponse> resultado = authorService.listBooksByAuthor(1L);

        // verify
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getTitle()).isEqualTo("Refactoring");
    }

    @Test
    void listBooksByAuthor_comAutorInexistente_deveLancar404() {
        // setup
        when(authorRepository.existsById(99L)).thenReturn(false);

        // exercise + verify
        assertThatThrownBy(() -> authorService.listBooksByAuthor(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");

        // verify
        verify(bookRepository, never()).findByAuthors_Id(any());
    }

    // -------------------------------------------------------
    // Helper
    // -------------------------------------------------------

    private AuthorRequest buildRequest(String name) {
        AuthorRequest r = new AuthorRequest();
        r.setName(name);
        return r;
    }
}

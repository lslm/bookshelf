package br.umc.demo;

import br.umc.demo.dto.AuthResponse;
import br.umc.demo.dto.AuthorRequest;
import br.umc.demo.dto.AuthorResponse;
import br.umc.demo.dto.BookRequest;
import br.umc.demo.dto.BookResponse;
import br.umc.demo.dto.LoginRequest;
import br.umc.demo.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para o CRUD de autores.
 *
 * Cobrem os principais fluxos:
 * 1. Listagem de autores (USER e ADMIN)
 * 2. Busca por ID (existente e inexistente)
 * 3. Criação de autor (ADMIN → 201, USER → 403)
 * 4. Atualização de autor (existente → 200, inexistente → 404)
 * 5. Exclusão de autor (existente → 204, inexistente → 404)
 * 6. Listagem de livros por autor (existente → 200, inexistente → 404)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthorIntegrationTest {

    @LocalServerPort
    private int port;

    private RestClient client;
    private String tokenAdmin;
    private String tokenUser;

    @BeforeEach
    void autenticar() {
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (req, res) -> {})
                .build();

        LoginRequest loginAdmin = new LoginRequest();
        loginAdmin.setEmail("admin@biblioteca.com");
        loginAdmin.setPassword("Admin@123");
        tokenAdmin = client.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginAdmin)
                .retrieve()
                .toEntity(AuthResponse.class)
                .getBody().getToken();

        RegisterRequest reg = new RegisterRequest();
        reg.setFullName("Usuario Autor Test");
        reg.setEmail("user.autor." + System.nanoTime() + "@test.com");
        reg.setPhone("11900000002");
        reg.setPassword("Senha@Autor1");
        tokenUser = client.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(reg)
                .retrieve()
                .toEntity(AuthResponse.class)
                .getBody().getToken();
    }

    // -------------------------------------------------------
    // GET /api/authors
    // -------------------------------------------------------

    @Test
    void listAuthors_userAutenticado_deveRetornar200() {
        ResponseEntity<Void> response = client.get().uri("/api/authors")
                .header("Authorization", "Bearer " + tokenUser)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listAuthors_adminAutenticado_deveRetornar200() {
        ResponseEntity<Void> response = client.get().uri("/api/authors")
                .header("Authorization", "Bearer " + tokenAdmin)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // -------------------------------------------------------
    // GET /api/authors/{id}
    // -------------------------------------------------------

    @Test
    void getAuthor_comIdExistente_deveRetornar200ComNome() {
        long ts = System.nanoTime();
        Long id = criarAutor("Autor Busca " + ts);

        ResponseEntity<AuthorResponse> response = client.get().uri("/api/authors/" + id)
                .header("Authorization", "Bearer " + tokenAdmin)
                .retrieve()
                .toEntity(AuthorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(id);
        assertThat(response.getBody().getName()).isEqualTo("Autor Busca " + ts);
    }

    @Test
    void getAuthor_comIdInexistente_deveRetornar404() {
        ResponseEntity<Void> response = client.get().uri("/api/authors/99999")
                .header("Authorization", "Bearer " + tokenAdmin)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // -------------------------------------------------------
    // POST /api/authors
    // -------------------------------------------------------

    @Test
    void createAuthor_comoAdmin_deveRetornar201ComIdENome() {
        long ts = System.nanoTime();
        AuthorRequest request = buildAuthorRequest("Novo Autor " + ts);

        ResponseEntity<AuthorResponse> response = client.post().uri("/api/authors")
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(AuthorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Novo Autor " + ts);
    }

    @Test
    void createAuthor_comoUser_deveRetornar403() {
        AuthorRequest request = buildAuthorRequest("Bloqueado");

        ResponseEntity<Void> response = client.post().uri("/api/authors")
                .header("Authorization", "Bearer " + tokenUser)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // -------------------------------------------------------
    // PUT /api/authors/{id}
    // -------------------------------------------------------

    @Test
    void updateAuthor_comIdExistente_deveRetornar200ComNomeAtualizado() {
        long ts = System.nanoTime();
        Long id = criarAutor("Original " + ts);

        AuthorRequest updateRequest = buildAuthorRequest("Atualizado " + ts);

        ResponseEntity<AuthorResponse> response = client.put().uri("/api/authors/" + id)
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateRequest)
                .retrieve()
                .toEntity(AuthorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getName()).isEqualTo("Atualizado " + ts);
    }

    @Test
    void updateAuthor_comIdInexistente_deveRetornar404() {
        AuthorRequest request = buildAuthorRequest("X");

        ResponseEntity<Void> response = client.put().uri("/api/authors/99999")
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // -------------------------------------------------------
    // DELETE /api/authors/{id}
    // -------------------------------------------------------

    @Test
    void deleteAuthor_comIdExistente_deveRetornar204() {
        Long id = criarAutor("Para Deletar " + System.nanoTime());

        ResponseEntity<Void> response = client.delete().uri("/api/authors/" + id)
                .header("Authorization", "Bearer " + tokenAdmin)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void deleteAuthor_comIdInexistente_deveRetornar404() {
        ResponseEntity<Void> response = client.delete().uri("/api/authors/99999")
                .header("Authorization", "Bearer " + tokenAdmin)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // -------------------------------------------------------
    // GET /api/authors/{id}/books
    // -------------------------------------------------------

    @Test
    void listBooksByAuthor_comAutorExistente_deveRetornar200() {
        Long authorId = criarAutor("Autor Livros " + System.nanoTime());

        ResponseEntity<Void> response = client.get().uri("/api/authors/" + authorId + "/books")
                .header("Authorization", "Bearer " + tokenAdmin)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listBooksByAuthor_comAutorComLivro_deveRetornarLivroNaLista() {
        long ts = System.nanoTime();
        Long authorId = criarAutor("Autor Com Livro " + ts);
        criarLivroComAutor("Livro do Autor " + ts, "AT-" + ts, authorId);

        ResponseEntity<BookResponse[]> response = client.get().uri("/api/authors/" + authorId + "/books")
                .header("Authorization", "Bearer " + tokenAdmin)
                .retrieve()
                .toEntity(BookResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].getTitle()).isEqualTo("Livro do Autor " + ts);
    }

    @Test
    void listBooksByAuthor_comAutorInexistente_deveRetornar404() {
        ResponseEntity<Void> response = client.get().uri("/api/authors/99999/books")
                .header("Authorization", "Bearer " + tokenAdmin)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private Long criarAutor(String nome) {
        AuthorRequest request = buildAuthorRequest(nome);
        return client.post().uri("/api/authors")
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(AuthorResponse.class)
                .getBody().getId();
    }

    private void criarLivroComAutor(String titulo, String isbn, Long authorId) {
        BookRequest request = new BookRequest();
        request.setTitle(titulo);
        request.setAuthorIds(List.of(authorId));
        request.setPublisher("Editora Teste");
        request.setIsbn(isbn);
        request.setSummary("Resumo");
        client.post().uri("/api/books")
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private AuthorRequest buildAuthorRequest(String name) {
        AuthorRequest r = new AuthorRequest();
        r.setName(name);
        return r;
    }
}

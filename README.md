# Biblioteca UMC — API de Acervo

API RESTful para gerenciamento do acervo de livros da biblioteca da UMC.

## Tecnologias

- Java 17
- Spring Boot 4
- Spring Data JPA
- H2 (banco em memória)

## Como executar

```bash
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

O console do banco H2 fica disponível em `http://localhost:8080/h2-console`
(JDBC URL: `jdbc:h2:mem:librarydb`, usuário: `sa`, senha: em branco).

---

## Endpoints

### Autores

#### Cadastrar autor

```
POST /api/authors
```

**Body:**
```json
{
  "name": "Robert C. Martin"
}
```

**Resposta** `201 Created`:
```json
{
  "id": 1,
  "name": "Robert C. Martin"
}
```

---

#### Listar todos os autores

```
GET /api/authors
```

**Resposta** `200 OK`:
```json
[
  { "id": 1, "name": "Robert C. Martin" },
  { "id": 2, "name": "Martin Fowler" }
]
```

---

#### Listar livros de um autor

```
GET /api/authors/{id}/books
```

**Resposta** `200 OK`:
```json
[
  {
    "id": 1,
    "title": "Clean Code",
    "authors": [
      { "id": 1, "name": "Robert C. Martin" }
    ],
    "publisher": "Prentice Hall",
    "isbn": "9780132350884",
    "summary": "Guia de boas práticas de programação."
  }
]
```

**Resposta** `404 Not Found` — quando o autor não existe:
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Autor não encontrado"
}
```

---

### Livros

#### Cadastrar livro

```
POST /api/books
```

**Body:**
```json
{
  "title": "Clean Code",
  "authorIds": [1],
  "publisher": "Prentice Hall",
  "isbn": "9780132350884",
  "summary": "Guia de boas práticas de programação para escrever código limpo e de fácil manutenção."
}
```

**Resposta** `201 Created`:
```json
{
  "id": 1,
  "title": "Clean Code",
  "authors": [
    { "id": 1, "name": "Robert C. Martin" }
  ],
  "publisher": "Prentice Hall",
  "isbn": "9780132350884",
  "summary": "Guia de boas práticas de programação para escrever código limpo e de fácil manutenção."
}
```

---

#### Ver detalhes de um livro

```
GET /api/books/{id}
```

**Resposta** `200 OK`:
```json
{
  "id": 1,
  "title": "Clean Code",
  "authors": [
    { "id": 1, "name": "Robert C. Martin" }
  ],
  "publisher": "Prentice Hall",
  "isbn": "9780132350884",
  "summary": "Guia de boas práticas de programação para escrever código limpo e de fácil manutenção."
}
```

**Resposta** `404 Not Found` — quando o livro não existe:
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Livro não encontrado"
}
```

---

#### Alterar informações de um livro

```
PUT /api/books/{id}
```

**Body** (todos os campos podem ser alterados):
```json
{
  "title": "Clean Code: A Handbook of Agile Software Craftsmanship",
  "authorIds": [1],
  "publisher": "Prentice Hall",
  "isbn": "9780132350884",
  "summary": "Descrição atualizada do livro."
}
```

**Resposta** `200 OK`:
```json
{
  "id": 1,
  "title": "Clean Code: A Handbook of Agile Software Craftsmanship",
  "authors": [
    { "id": 1, "name": "Robert C. Martin" }
  ],
  "publisher": "Prentice Hall",
  "isbn": "9780132350884",
  "summary": "Descrição atualizada do livro."
}
```

**Resposta** `404 Not Found` — quando o livro não existe.

---

#### Remover um livro

```
DELETE /api/books/{id}
```

**Resposta** `204 No Content` — remoção bem-sucedida (sem corpo).

**Resposta** `404 Not Found` — quando o livro não existe.

---

## Resumo dos endpoints

| Método   | Rota                       | Descrição                        |
|----------|----------------------------|----------------------------------|
| `POST`   | `/api/authors`             | Cadastrar autor                  |
| `GET`    | `/api/authors`             | Listar todos os autores          |
| `GET`    | `/api/authors/{id}/books`  | Listar livros de um autor        |
| `POST`   | `/api/books`               | Cadastrar livro                  |
| `GET`    | `/api/books/{id}`          | Ver detalhes de um livro         |
| `PUT`    | `/api/books/{id}`          | Alterar informações de um livro  |
| `DELETE` | `/api/books/{id}`          | Remover um livro                 |

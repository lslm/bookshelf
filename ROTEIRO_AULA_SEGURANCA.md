# Roteiro de Aula — Segurança em APIs Spring Boot

> **Objetivo:** Implementar uma camada de segurança completa em uma API REST existente, cobrindo autenticação com JWT, autorização por roles, armazenamento seguro de senhas com Argon2 e boas práticas OWASP.
>
> **Pré-requisito:** A API de biblioteca já está funcionando com endpoints de autores e livros. Nenhuma segurança está configurada ainda.
>
> **Resultado esperado ao final:** 8 testes passando, API protegida por JWT, dois perfis de usuário (ADMIN e USER), senhas com hash Argon2.

---

## Índice

1. [Conceitos Fundamentais](#1-conceitos-fundamentais)
2. [O que vamos adicionar e por quê](#2-o-que-vamos-adicionar-e-por-quê)
3. [Passo 1 — Dependências](#3-passo-1--dependências)
4. [Passo 2 — Configuração do JWT](#4-passo-2--configuração-do-jwt)
5. [Passo 3 — Entidade e Role do Usuário](#5-passo-3--entidade-e-role-do-usuário)
6. [Passo 4 — Repositório de Usuários](#6-passo-4--repositório-de-usuários)
7. [Passo 5 — DTOs de Autenticação](#7-passo-5--dtos-de-autenticação)
8. [Passo 6 — UserDetailsService](#8-passo-6--userdetailsservice)
9. [Passo 7 — JwtService](#9-passo-7--jwtservice)
10. [Passo 8 — JwtAuthenticationFilter](#10-passo-8--jwtauthenticationfilter)
11. [Passo 9 — SecurityConfig](#11-passo-9--securityconfig)
12. [Passo 10 — AuthService](#12-passo-10--authservice)
13. [Passo 11 — AuthController](#13-passo-11--authcontroller)
14. [Passo 12 — Seed de Admin para Aula](#14-passo-12--seed-de-admin-para-aula)
15. [Passo 13 — Endpoint GET /api/books](#15-passo-13--endpoint-get-apibooks)
16. [Passo 14 — @PreAuthorize nos Controllers](#16-passo-14--preauthorize-nos-controllers)
17. [Passo 15 — Testes de Integração](#17-passo-15--testes-de-integração)
18. [Verificação Manual com curl](#18-verificação-manual-com-curl)
19. [Diagrama do Fluxo Completo](#19-diagrama-do-fluxo-completo)
20. [Problemas Encontrados Durante a Implementação](#20-problemas-encontrados-durante-a-implementação)

---

## 1. Conceitos Fundamentais

Antes de escrever qualquer código, é essencial entender o vocabulário e os conceitos que vamos implementar.

### Autenticação vs Autorização

Esses dois termos são frequentemente confundidos, mas representam etapas distintas e sequenciais:

| Conceito | Pergunta que responde | Exemplo neste projeto |
|---|---|---|
| **Autenticação** | *Quem é você?* | Você enviou email e senha corretos? Aqui está um token JWT provando sua identidade. |
| **Autorização** | *O que você pode fazer?* | Você é ADMIN? Pode criar livros. É USER? Só pode listar. |

> **Analogia:** Autenticação é passar pela roleta do metrô com seu cartão válido. Autorização é o que você pode fazer dentro da estação — só funcionários entram na cabine do operador.

A autenticação acontece PRIMEIRO. Sem ela, não há autorização possível.

---

### O que é JWT?

JWT (JSON Web Token) é um padrão para transmitir informações de autenticação de forma compacta e verificável.

Um token JWT tem três partes separadas por ponto:

```
eyJhbGciOiJIUzI1NiJ9   .   eyJzdWIiOiJhZG1pbkBiaWJsaW90ZWNhLmNvbSJ9   .   xK7...
      HEADER                            PAYLOAD                              SIGNATURE
  (algoritmo usado)              (dados: email, expiração)          (assinatura com chave secreta)
```

As três partes são codificadas em **Base64** — não criptografadas. Qualquer pessoa pode decodificar o header e o payload. O que garante a segurança é a **assinatura digital**: se alguém alterar qualquer byte do token, a assinatura deixa de bater e o servidor rejeita o token.

**Por que JWT em vez de sessão?**

| Sessão tradicional | JWT |
|---|---|
| Servidor guarda estado (quem está logado) | Servidor é **stateless** (sem memória entre requisições) |
| Funciona bem com 1 servidor | Funciona bem com múltiplos servidores / microsserviços |
| Requer armazenamento (memória / banco) | Toda informação está no token |
| Vulnerável a CSRF | Não vulnerável a CSRF (não usa cookie de sessão) |

---

### Por que Argon2 para senhas?

Este é o ponto mais importante de segurança deste projeto. Nunca armazenar senhas em texto puro é óbvio, mas **a escolha do algoritmo de hash importa muito**.

**O problema com hashes rápidos (MD5, SHA-1, SHA-256):**

Esses algoritmos foram projetados para serem rápidos — úteis para verificar integridade de arquivos, mas péssimos para senhas. Uma GPU moderna consegue calcular **bilhões de SHA-256 por segundo**. Isso significa que um atacante que obteve o banco de dados pode testar senhas em escala industrial.

**Por que Argon2 é diferente:**

- **Memory-hard:** o algoritmo exige grande quantidade de RAM para ser executado. Isso torna ataques com GPU ou hardware especializado (ASIC) proibitivamente caros, pois memória não é paralelizável da mesma forma que processamento.
- **Parâmetros configuráveis:** você escolhe quantas iterações, quanta memória e quantos threads usar. Pode aumentar o custo à medida que hardware fica mais rápido.
- **Venceu o Password Hashing Competition em 2015**, concurso que reuniu criptógrafos do mundo todo para definir o melhor algoritmo para senhas.
- **Recomendado pelo OWASP** como primeira escolha para armazenamento de senhas.

> **Resumo:** bcrypt e scrypt também são boas opções. SHA-256, MD5, SHA-1 **nunca** devem ser usados para senhas.

---

### OWASP Top 10 — O que este projeto endereça

O OWASP (Open Worldwide Application Security Project) publica uma lista dos 10 riscos mais críticos em aplicações web. Neste projeto, endereçamos diretamente:

| Risco OWASP | O que é | Como tratamos |
|---|---|---|
| **Broken Authentication** | Falhas no processo de autenticação | JWT com assinatura, Argon2 para senhas, mensagem de erro genérica no login |
| **Sensitive Data Exposure** | Exposição de dados sensíveis | Resposta de auth não retorna senha; chave JWT não fica no código |
| **Security Misconfiguration** | Configuração insegura | CSRF desabilitado corretamente; sessão stateless; secrets em configuração |
| **Broken Access Control** | Falta de controle de acesso | Autorização por role em cada endpoint |

---

## 2. O que vamos adicionar e por quê

Aqui está o mapa completo do que será criado, na ordem em que será feito:

```
src/main/java/br/umc/demo/
├── entity/
│   ├── Role.java                    ← NOVO: enum com ADMIN e USER
│   └── User.java                    ← NOVO: entidade persistida no banco
├── repository/
│   └── UserRepository.java          ← NOVO: busca usuário por email
├── dto/
│   ├── RegisterRequest.java         ← NOVO: dados de entrada do registro
│   ├── LoginRequest.java            ← NOVO: dados de entrada do login
│   └── AuthResponse.java            ← NOVO: token + email + role na resposta
├── service/
│   ├── UserDetailsServiceImpl.java  ← NOVO: ponte entre Spring Security e banco
│   └── AuthService.java             ← NOVO: lógica de registro e login
├── security/
│   ├── JwtService.java              ← NOVO: gera e valida tokens JWT
│   ├── JwtAuthenticationFilter.java ← NOVO: lê o token em cada requisição
│   └── SecurityConfig.java          ← NOVO: configura toda a segurança
├── controller/
│   └── AuthController.java          ← NOVO: endpoints públicos /api/auth/*
└── config/
    └── DataInitializer.java         ← NOVO: seed do admin para aula

src/main/resources/
└── application.properties           ← MODIFICADO: adiciona config JWT

pom.xml                              ← MODIFICADO: novas dependências

src/main/java/.../controller/BookController.java   ← MODIFICADO: GET /api/books + @PreAuthorize
src/main/java/.../controller/AuthorController.java ← MODIFICADO: @PreAuthorize em todos os métodos
src/main/java/.../service/BookService.java         ← MODIFICADO: listBooks()

src/test/java/.../SecurityIntegrationTest.java     ← NOVO: 7 testes de segurança
```

**A ordem importa.** Cada peça depende da anterior:
- `SecurityConfig` depende de `JwtAuthenticationFilter` e `UserDetailsService`
- `JwtAuthenticationFilter` depende de `JwtService`
- `AuthService` depende de `UserRepository`, `PasswordEncoder`, `JwtService` e `AuthenticationManager`
- `AuthenticationManager` é criado pelo `SecurityConfig`

---

## 3. Passo 1 — Dependências

**Arquivo:** `pom.xml`

**Por quê:** Precisamos adicionar 5 grupos de dependências que não existiam:

1. **spring-boot-starter-security** — fornece toda a infraestrutura do Spring Security (filtros, contexto de autenticação, gerenciador de senhas, etc.)
2. **spring-boot-starter-validation** — habilita as anotações `@NotBlank`, `@Email` nos DTOs
3. **jjwt** (3 artefatos) — biblioteca para criar e validar tokens JWT
4. **bcprov-jdk18on** (Bouncy Castle) — implementação do Argon2 que o Spring Security usa internamente
5. **spring-security-test** — permite testar endpoints protegidos

Abra o `pom.xml` e adicione dentro de `<dependencies>`, após a dependência do H2:

```xml
<!-- Spring Security: autenticação e autorização -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Validação de campos nos DTOs (ex: @NotBlank, @Email) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- JWT: geração e validação de tokens -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!--
    Bouncy Castle: necessário para o Argon2PasswordEncoder.
    Argon2 é o algoritmo recomendado para hashing de senhas por ser:
    - Memory-hard: dificulta ataques com GPU e ASIC
    - Configurável em custo de CPU, memória e paralelismo
    - Vencedor do Password Hashing Competition (2015)
    Diferente de SHA-256/MD5 (que são hashes rápidos e não adequados para senhas).
-->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.80</version>
</dependency>

<!-- Suporte a testes com Spring Security -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

> **Atenção:** O jjwt é dividido em 3 artefatos por design: `jjwt-api` é o que o código compila, `jjwt-impl` e `jjwt-jackson` são as implementações em tempo de execução (`scope runtime`). Isso permite trocar a implementação sem mudar o código.

---

## 4. Passo 2 — Configuração do JWT

**Arquivo:** `src/main/resources/application.properties`

**Por quê:** A chave secreta do JWT **nunca pode ficar hardcoded no código-fonte**. Se ficar no código, qualquer pessoa com acesso ao repositório pode assinar tokens falsos. A solução é lê-la de configuração — e em produção, de variáveis de ambiente ou cofres de segredos (AWS Secrets Manager, HashiCorp Vault, etc.).

Adicione ao final do arquivo:

```properties
# -------------------------------------------------------
# JWT — configurações de segurança
# A chave secreta NUNCA deve ficar hardcoded no código-fonte.
# Em produção, use variável de ambiente ou cofre de segredos.
# A chave precisa ter no mínimo 32 caracteres (256 bits para HMAC-SHA256).
# -------------------------------------------------------
security.jwt.secret=biblioteca-demo-secret-key-para-aula-umc-2024
security.jwt.expiration=86400000
```

> **Por que 32 caracteres no mínimo?** O algoritmo HMAC-SHA256 usa uma chave de 256 bits (32 bytes). Se a chave for menor, o JJWT rejeita com erro em tempo de execução.
>
> **O que é `expiration=86400000`?** São 86.400.000 milissegundos = 24 horas. Após esse tempo, o token expira e o usuário precisa fazer login novamente. Para aula, 24h é conveniente. Em produção, o prazo depende da política de segurança da aplicação.

---

## 5. Passo 3 — Entidade e Role do Usuário

### 5.1 — Role (enum)

**Arquivo:** `src/main/java/br/umc/demo/entity/Role.java`

**Por quê:** Usamos um enum (e não uma String) para as roles porque:
- Erros de digitação são capturados em tempo de compilação
- Fica explícito quais roles existem na aplicação
- É fácil de percorrer com `Role.values()` se necessário

```java
package br.umc.demo.entity;

/**
 * Papéis disponíveis na aplicação.
 *
 * ADMIN → acesso total (criar, editar, deletar autores e livros)
 * USER  → acesso somente leitura (listar livros e autores)
 *
 * Conceito de aula: "autorização" é o processo de verificar SE um usuário
 * já autenticado tem PERMISSÃO para executar determinada ação.
 */
public enum Role {
    ADMIN,
    USER
}
```

### 5.2 — User (entidade)

**Arquivo:** `src/main/java/br/umc/demo/entity/User.java`

**Por quê:** Precisamos persistir usuários no banco. A entidade segue o mesmo padrão das entidades `Author` e `Book` já existentes.

Dois pontos merecem atenção especial no design desta entidade:

1. **`@Column(unique = true)` no email** — o banco rejeita emails duplicados a nível de constraint. A validação no serviço é uma defesa adicional, mas a constraint no banco é a garantia definitiva.
2. **A senha é um hash**, nunca o valor original. O campo se chama `password` mas armazenará algo como `$argon2id$v=19$m=65536,t=3,p=1$...`.

```java
package br.umc.demo.entity;

import jakarta.persistence.*;

/**
 * Representa um usuário da aplicação.
 *
 * Conceito de aula: "autenticação" é o processo de verificar QUEM é o usuário —
 * neste caso, validando email e senha para emitir um token JWT.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    /**
     * Senha armazenada como hash Argon2 — NUNCA em texto puro.
     *
     * Por que Argon2 e não MD5, SHA-1 ou SHA-256?
     * - Hashes rápidos (MD5, SHA-256) foram projetados para velocidade,
     *   o que os torna vulneráveis a ataques de força bruta com GPU.
     * - Argon2 é "memory-hard": exige muita memória para ser computado,
     *   tornando ataques em larga escala (ASIC, GPU) proibitivamente caros.
     * - É o vencedor do Password Hashing Competition (2015) e recomendado
     *   pelo OWASP para armazenamento de senhas.
     */
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public User() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
```

> **Por que `@Enumerated(EnumType.STRING)`?** Por padrão, JPA persiste enums como números (0, 1, 2...). Se você adicionar uma nova role no meio da lista, os números mudam e o banco fica corrompido. Com `STRING`, o banco armazena `"ADMIN"` ou `"USER"` — estável e legível.

---

## 6. Passo 4 — Repositório de Usuários

**Arquivo:** `src/main/java/br/umc/demo/repository/UserRepository.java`

**Por quê:** Precisamos de dois métodos customizados além dos herdados do `JpaRepository`:

- `findByEmail(String)` — o Spring Security usa **email como username** neste projeto; precisamos buscar o usuário por ele.
- `existsByEmail(String)` — verificação eficiente de unicidade antes de registrar (não carrega o objeto inteiro, só verifica existência).

```java
package br.umc.demo.repository;

import br.umc.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
```

> **Convenção de nomenclatura do Spring Data:** `findByEmail` é interpretado automaticamente como `SELECT * FROM users WHERE email = ?`. Nenhuma query precisa ser escrita.

---

## 7. Passo 5 — DTOs de Autenticação

DTOs (Data Transfer Objects) são objetos que representam os dados de entrada e saída dos endpoints. Nunca use a entidade diretamente como body de requisição — isso expõe campos que o cliente não deve controlar (como `id` e `role`).

### 7.1 — RegisterRequest

**Arquivo:** `src/main/java/br/umc/demo/dto/RegisterRequest.java`

**Por quê as anotações de validação?** `@NotBlank` e `@Email` são processadas pelo Spring Validation quando o controller usa `@Valid`. Isso garante que dados malformados são rejeitados **antes** de chegar ao serviço. OWASP recomenda validar na borda do sistema.

```java
package br.umc.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para registro de novo usuário.
 * As anotações de validação garantem que dados inválidos nunca cheguem à camada de serviço.
 * Boas práticas OWASP: valide entrada na borda do sistema.
 */
public class RegisterRequest {

    @NotBlank(message = "Nome completo é obrigatório")
    private String fullName;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "Telefone é obrigatório")
    private String phone;

    @NotBlank(message = "Senha é obrigatória")
    private String password;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

### 7.2 — LoginRequest

**Arquivo:** `src/main/java/br/umc/demo/dto/LoginRequest.java`

```java
package br.umc.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    private String password;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

### 7.3 — AuthResponse

**Arquivo:** `src/main/java/br/umc/demo/dto/AuthResponse.java`

**Por quê retornar apenas token, email e role?** A resposta expõe o mínimo necessário. A senha nunca sai da aplicação. O `fullName` e `phone` não são necessários na resposta de autenticação. Isso segue o princípio de **Sensitive Data Exposure** do OWASP: não exponha dados que o cliente não precisa.

```java
package br.umc.demo.dto;

/**
 * Resposta de autenticação.
 *
 * Retorna apenas o necessário — sem expor dados sensíveis como a senha.
 * Boas práticas OWASP: evite Sensitive Data Exposure nas respostas da API.
 *
 * O cliente deve enviar o token no header de todas as requisições protegidas:
 *   Authorization: Bearer <token>
 */
public class AuthResponse {

    private String token;
    private String email;
    private String role;

    public AuthResponse(String token, String email, String role) {
        this.token = token;
        this.email = email;
        this.role = role;
    }

    public String getToken() { return token; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}
```

---

## 8. Passo 6 — UserDetailsService

**Arquivo:** `src/main/java/br/umc/demo/service/UserDetailsServiceImpl.java`

**Por quê esta classe existe?** O Spring Security não sabe nada sobre o seu banco de dados. Ele só entende a interface `UserDetailsService` com o método `loadUserByUsername(String)`. Esta classe é a **ponte** entre o Spring Security e o `UserRepository`.

Quando o Spring Security precisa autenticar alguém, ele chama `loadUserByUsername(email)`. Esta implementação busca o usuário no banco e retorna um objeto `UserDetails` que o Spring Security entende.

```java
package br.umc.demo.service;

import br.umc.demo.entity.User;
import br.umc.demo.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Integra o Spring Security com o repositório de usuários da aplicação.
 *
 * O Spring Security chama loadUserByUsername() durante a autenticação para
 * buscar o usuário e comparar a senha fornecida com o hash armazenado.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));

        // O Spring Security usa o prefixo "ROLE_" internamente para roles.
        // Assim, hasRole("ADMIN") corresponde à authority "ROLE_ADMIN".
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
```

> **O prefixo `ROLE_`:** Este é um detalhe importante do Spring Security. Quando você usa `.hasRole("ADMIN")` na configuração de segurança, o Spring Security procura pela authority `"ROLE_ADMIN"`. Se você armazenar apenas `"ADMIN"`, o acesso será sempre negado. Alternativamente, você pode usar `.hasAuthority("ADMIN")` sem o prefixo, mas a convenção `ROLE_` é a mais comum.

---

## 9. Passo 7 — JwtService

**Arquivo:** `src/main/java/br/umc/demo/security/JwtService.java`

> Crie o pacote `security` se ele não existir.

**Por quê uma classe separada para JWT?** Separação de responsabilidades. Esta classe encapsula toda a lógica de JWT: gerar, validar e extrair informações de tokens. Qualquer outra classe que precisar lidar com JWT usa esta, sem precisar conhecer os detalhes do JJWT.

**Fluxo de uso:**
1. Login bem-sucedido → `generateToken(userDetails)` → token enviado ao cliente
2. Requisição protegida chega → `extractEmail(token)` → `isTokenValid(token, userDetails)` → autenticado

```java
package br.umc.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * Serviço responsável por gerar e validar tokens JWT.
 *
 * O que é JWT?
 * Um token composto por 3 partes (header.payload.signature) codificadas em Base64.
 * O servidor assina o token com uma chave secreta — qualquer alteração invalida a assinatura.
 * Por ser stateless, o servidor NÃO precisa armazenar sessões: basta validar a assinatura.
 */
@Service
public class JwtService {

    // A chave secreta vem do application.properties — NUNCA hardcoded no código-fonte.
    // Boas práticas OWASP: Security Misconfiguration — segredos não pertencem ao código.
    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.expiration}")
    private long expiration;

    /**
     * Gera um token JWT assinado para o usuário autenticado.
     * O "subject" do token é o email do usuário.
     */
    public String generateToken(UserDetails userDetails) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    /** Extrai o email (subject) do token. */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Valida o token: verifica se pertence ao usuário e se não expirou. */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}
```

> **Como funciona `Keys.hmacShaKeyFor`?** Ele cria uma chave criptográfica HMAC-SHA256 a partir dos bytes da string secreta. O JJWT exige no mínimo 32 bytes para garantir 256 bits de entropia — por isso o requisito no `application.properties`.

---

## 10. Passo 8 — JwtAuthenticationFilter

**Arquivo:** `src/main/java/br/umc/demo/security/JwtAuthenticationFilter.java`

**Por quê um filtro?** Em aplicações web Java, os filtros de servlet executam antes dos controllers. Eles têm acesso a cada requisição HTTP que chega. O `JwtAuthenticationFilter` lê o token do header `Authorization`, valida, e registra o usuário no `SecurityContext` — o "estado atual" de segurança daquela requisição.

**Por que `OncePerRequestFilter`?** Garante que o filtro executa exatamente uma vez por requisição, mesmo em casos de redirecionamentos internos.

```java
package br.umc.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro executado uma vez por requisição.
 *
 * Fluxo de autenticação via JWT:
 * 1. Cliente envia: Authorization: Bearer <token>
 * 2. Este filtro extrai e valida o token
 * 3. Se válido, registra o usuário no SecurityContext
 * 4. O Spring Security usa o SecurityContext para verificar permissões nos endpoints
 *
 * Se o token for inválido ou ausente, a requisição prossegue sem autenticação
 * e o Spring Security retorna 401 para rotas protegidas.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        // Verifica se o header Authorization está presente e no formato correto
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7); // remove "Bearer "
            String email = jwtService.extractEmail(token);

            // Autentica apenas se o email foi extraído e não há sessão ativa
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    // Registra o usuário como autenticado no contexto desta requisição
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token inválido, expirado ou malformado — a requisição prossegue sem autenticação.
            // Rotas protegidas retornarão 401 automaticamente.
        }

        filterChain.doFilter(request, response);
    }
}
```

> **Por que o `try/catch` silencioso?** Se o token estiver malformado ou expirado, o JJWT lança uma exceção. Não queremos que isso retorne um 500 — queremos simplesmente que a requisição prossiga sem autenticação. O Spring Security se encarrega de retornar 401 para rotas protegidas.
>
> **O que é o `SecurityContextHolder`?** É um armazenamento thread-local que guarda "quem está autenticado nesta requisição". Cada requisição HTTP tem sua própria thread, então registrar o usuário aqui é seguro e isolado.

---

## 11. Passo 9 — SecurityConfig

**Arquivo:** `src/main/java/br/umc/demo/security/SecurityConfig.java`

Esta é a peça central. Ela define:
- Como a aplicação se comporta sem autenticação (→ 401)
- Como CORS funciona
- Quais rotas são totalmente públicas
- Qual encoder de senha usar
- Onde o filtro JWT se encaixa na cadeia

**Por que `SecurityFilterChain` e não `WebSecurityConfigurerAdapter`?** O `WebSecurityConfigurerAdapter` foi depreciado no Spring Security 5.7 e removido na versão 6. A abordagem moderna usa um bean `SecurityFilterChain` — mais explícita e testável.

**Divisão de responsabilidades:** Este `SecurityConfig` cuida da camada de **transporte** (HTTP): rotas públicas, sessão, CORS, tratamento de erros. As regras por **role** ficam nos próprios controllers via `@PreAuthorize` (ver Passo 14). Isso mantém cada arquivo com um único propósito.

```java
package br.umc.demo.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuração central de segurança da aplicação.
 *
 * Responsabilidades desta classe:
 * - AUTENTICAÇÃO: quem pode entrar (via JWT, filtro, sessão stateless)
 * - ROTAS PÚBLICAS: o que não precisa de token
 * - INFRAESTRUTURA: encoder de senha, AuthenticationManager, CORS
 *
 * As regras de AUTORIZAÇÃO por role estão nos controllers (@PreAuthorize).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // habilita @PreAuthorize, @PostAuthorize nos controllers e services
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          UserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF desabilitado: a API é stateless (JWT, sem cookies de sessão).
            // CSRF protege sessões baseadas em cookie — não se aplica a JWT.
            .csrf(csrf -> csrf.disable())

            // Sem sessões HTTP: cada requisição é autenticada pelo token JWT.
            // Isso evita que o servidor precise "lembrar" quem está logado.
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // CORS: permite requisições de outras origens (necessário para frontends)
            .cors(cors -> cors.configurationSource(request -> {
                var config = new org.springframework.web.cors.CorsConfiguration();
                config.setAllowedOrigins(java.util.List.of("*"));
                config.setAllowedMethods(java.util.List.of("*"));
                config.setAllowedHeaders(java.util.List.of("*"));
                return config;
            }))

            // -------------------------------------------------------
            // Tratamento de erros de autenticação e autorização
            // -------------------------------------------------------
            .exceptionHandling(ex -> ex
                // 401: usuário não autenticado tentando acessar recurso protegido
                .authenticationEntryPoint((request, response, authException) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Não autenticado"))
                // 403: usuário autenticado sem permissão suficiente
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acesso negado"))
            )

            // -------------------------------------------------------
            // AUTORIZAÇÃO POR ROTA — apenas regras globais
            // As restrições por role estão nos controllers via @PreAuthorize
            // -------------------------------------------------------
            .authorizeHttpRequests(auth -> auth

                // PÚBLICO: registro e login não exigem autenticação
                .requestMatchers("/api/auth/**").permitAll()

                // Endpoint de erros do Spring Boot — deve ser público para que
                // exceções retornem o status correto (ex: 409, 404) sem serem
                // bloqueadas pelo Spring Security com 403
                .requestMatchers("/error").permitAll()

                // Console H2: aberto apenas para fins didáticos em dev
                // EM PRODUÇÃO: remova esta linha e desabilite spring.h2.console.enabled
                .requestMatchers("/h2-console/**").permitAll()

                // Piso mínimo: qualquer outro endpoint exige ao menos autenticação.
                // As restrições específicas por role são aplicadas pelo @PreAuthorize.
                .anyRequest().authenticated()
            )

            // Permite que o H2 Console seja exibido em iframes no browser
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

            // Insere o filtro JWT antes do filtro padrão de autenticação
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Encoder de senhas com Argon2.
     *
     * Por que Argon2 e não bcrypt ou scrypt?
     * - Argon2 venceu o PHC (Password Hashing Competition) em 2015
     * - defaultsForSpringSecurity_v5_8() usa Argon2id com parâmetros seguros e balanceados
     *
     * Por que NÃO usar MD5, SHA-1 ou SHA-256?
     * - São hashes de propósito geral, projetados para VELOCIDADE
     * - Uma GPU moderna calcula bilhões de SHA-256 por segundo
     * - Argon2 é deliberadamente lento e memory-hard — cada tentativa de ataque custa muito
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // Spring Security 7: UserDetailsService é obrigatório no construtor
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

> **Por que `.requestMatchers("/error").permitAll()`?** Quando uma exceção ocorre em um controller (ex: email duplicado → 409), o Spring Boot encaminha a requisição internamente para o endpoint `/error` para formatar a resposta. Se `/error` não estiver na lista de rotas permitidas, o Spring Security intercepta esse encaminhamento e retorna 403 em vez do erro correto.
>
> **Por que desabilitar CSRF?** CSRF (Cross-Site Request Forgery) é um ataque que explora o fato de browsers enviarem cookies automaticamente. Com JWT no header `Authorization`, não há cookie de sessão — o browser não enviaria o token automaticamente para outro site. Portanto, CSRF não se aplica e pode ser desabilitado.
>
> **Por que `@EnableMethodSecurity`?** Sem essa anotação, o Spring ignora silenciosamente todos os `@PreAuthorize` — o código compila e executa, mas as restrições de role não funcionam. Este é um erro difícil de detectar, então a anotação é obrigatória.

---

## 12. Passo 10 — AuthService

**Arquivo:** `src/main/java/br/umc/demo/service/AuthService.java`

**Por quê separar em serviço?** O controller deve ser fino — apenas receber a requisição, validar e delegar. A lógica de negócio (verificar email duplicado, hashear senha, gerar token) fica no serviço.

```java
package br.umc.demo.service;

import br.umc.demo.dto.AuthResponse;
import br.umc.demo.dto.LoginRequest;
import br.umc.demo.dto.RegisterRequest;
import br.umc.demo.entity.Role;
import br.umc.demo.entity.User;
import br.umc.demo.repository.UserRepository;
import br.umc.demo.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Registra um novo usuário na aplicação.
     *
     * Fluxo:
     * 1. Verifica se o email já existe (email único)
     * 2. Faz o hash da senha com Argon2 — a senha em texto puro nunca é persistida
     * 3. Persiste o usuário com role USER (novos cadastros sempre são USER)
     * 4. Gera e retorna um token JWT para login imediato após registro
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email já cadastrado");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        // A senha é hashada ANTES de ser salva — nunca armazene senha em texto puro
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // Todo novo registro é USER — ADMIN só pode ser configurado manualmente
        user.setRole(Role.USER);

        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);
        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }

    /**
     * Autentica um usuário e retorna um token JWT.
     *
     * O AuthenticationManager:
     * 1. Busca o usuário pelo email (via UserDetailsService)
     * 2. Compara a senha fornecida com o hash Argon2 armazenado
     * 3. Lança BadCredentialsException se inválido
     */
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            // Credenciais inválidas → 401 Unauthorized
            // OWASP: nunca revele se foi o email ou a senha que falhou —
            // isso evita que um atacante enumere usuários válidos.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtService.generateToken(userDetails);

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }
}
```

> **Por que a mensagem "Credenciais inválidas" é genérica?**
> Uma mensagem específica como "Senha incorreta" ou "Email não encontrado" permitiria que um atacante descobrisse quais emails estão cadastrados no sistema (*user enumeration*). Ao não diferenciar, dificultamos esse tipo de ataque.
>
> **Por que novos usuários sempre são `USER`?**
> Se o cliente pudesse enviar a role no corpo da requisição, qualquer um poderia se registrar como ADMIN. A role só pode ser elevada por alguém com acesso direto ao banco ou por uma funcionalidade administrativa controlada.

---

## 13. Passo 11 — AuthController

**Arquivo:** `src/main/java/br/umc/demo/controller/AuthController.java`

**Por quê `/api/auth/**`?** Mantém consistência com o prefixo `/api/` já existente e agrupa os endpoints de autenticação em um subgrupo claro, fácil de liberar no `SecurityConfig`.

```java
package br.umc.demo.controller;

import br.umc.demo.dto.AuthResponse;
import br.umc.demo.dto.LoginRequest;
import br.umc.demo.dto.RegisterRequest;
import br.umc.demo.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints públicos de autenticação.
 *
 * POST /api/auth/register → cria conta e retorna token JWT
 * POST /api/auth/login    → valida credenciais e retorna token JWT
 *
 * Após o login/registro, o cliente usa o token assim:
 *   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
```

> **O `@Valid`:** Instrui o Spring a executar as validações declaradas no DTO (ex: `@NotBlank`, `@Email`) antes de chamar o método. Se alguma falhar, o Spring retorna automaticamente `400 Bad Request` com as mensagens de erro — sem precisar escrever uma linha de código de validação manual.

---

## 14. Passo 12 — Seed de Admin para Aula

**Arquivo:** `src/main/java/br/umc/demo/config/DataInitializer.java`

> Crie o pacote `config` se ele não existir.

**Por quê um seed?** A API usa banco H2 com `create-drop` — o banco é zerado a cada reinicialização. Para demonstrar funcionalidades de ADMIN em aula sem ter que criar um usuário manualmente toda vez, criamos um admin automaticamente ao iniciar.

**Atenção crítica:** Este código é exclusivo para desenvolvimento e aula. Em produção, jamais insira credenciais fixas desta forma.

```java
package br.umc.demo.config;

import br.umc.demo.entity.Role;
import br.umc.demo.entity.User;
import br.umc.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seed de dados para ambiente de desenvolvimento e aula.
 *
 * ============================================================
 * ATENÇÃO: USE APENAS EM DESENVOLVIMENTO / AULA
 * NÃO inclua seeds com credenciais fixas em ambiente de produção.
 * ============================================================
 *
 * Credenciais do administrador de aula:
 *   Email: admin@biblioteca.com
 *   Senha: Admin@123
 */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByEmail("admin@biblioteca.com")) {
                User admin = new User();
                admin.setFullName("Administrador da Biblioteca");
                admin.setEmail("admin@biblioteca.com");
                admin.setPhone("11999990000");
                // Mesmo aqui, a senha é hashada com Argon2 — nunca em texto puro
                admin.setPassword(passwordEncoder.encode("Admin@123"));
                admin.setRole(Role.ADMIN);
                userRepository.save(admin);

                System.out.println("==============================================");
                System.out.println("  [DEV] Admin de aula criado com sucesso!");
                System.out.println("  Email: admin@biblioteca.com");
                System.out.println("  Senha: Admin@123");
                System.out.println("==============================================");
            }
        };
    }
}
```

> **Por que `CommandLineRunner`?** É uma interface do Spring Boot que executa o método `run()` logo após o contexto da aplicação estar completamente inicializado. Usando `@Bean`, o Spring injeta automaticamente as dependências (repositório e encoder).
>
> **Por que o `if (!existsByEmail(...))`?** Em caso de reinicialização sem `create-drop` (por exemplo, se o banco fosse persistido), o seed não tentaria inserir um registro duplicado.

---

## 15. Passo 13 — Endpoint GET /api/books

A API original não tinha um endpoint para listar todos os livros (`GET /api/books`). Segundo a proposta da aula, esse endpoint deve existir e ser acessível por qualquer usuário autenticado. São duas alterações simples.

### 15.1 — Adicionar `listBooks()` ao BookService

**Arquivo:** `src/main/java/br/umc/demo/service/BookService.java`

Adicione o método antes de `createBook`:

```java
public List<BookResponse> listBooks() {
    return bookRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
}
```

### 15.2 — Adicionar `GET /api/books` ao BookController

**Arquivo:** `src/main/java/br/umc/demo/controller/BookController.java`

Adicione a importação `import java.util.List;` e o endpoint antes de `createBook`:

```java
import java.util.List;

// ...

// GET /api/books → acessível por ADMIN e USER autenticados
@GetMapping
@PreAuthorize("isAuthenticated()")
public List<BookResponse> listBooks() {
    return bookService.listBooks();
}
```

---

## 16. Passo 14 — @PreAuthorize nos Controllers

**Arquivos modificados:**
- `src/main/java/br/umc/demo/controller/BookController.java`
- `src/main/java/br/umc/demo/controller/AuthorController.java`

**Por quê `@PreAuthorize` nos controllers em vez de no `SecurityConfig`?**

Existem duas formas de aplicar regras de autorização por role no Spring Security:

| Abordagem | Onde fica a regra | Vantagem | Desvantagem |
|---|---|---|---|
| `SecurityConfig` centralizado | Um arquivo só | Visão geral em um lugar | Fica enorme; difícil de escalar; pouco legível |
| `@PreAuthorize` nos controllers | Junto ao endpoint | Cada endpoint autodocumenta sua restrição | Requer `@EnableMethodSecurity` ativo |

A abordagem com `@PreAuthorize` é mais comum em aplicações reais porque a restrição fica **exatamente onde o comportamento é definido** — o desenvolvedor não precisa consultar outro arquivo para saber quem pode chamar aquele método.

**Como funciona `@PreAuthorize`?**

O Spring AOP intercepta a chamada ao método antes de executá-lo e avalia a expressão SpEL (Spring Expression Language) fornecida. Se a expressão retornar `false`, o Spring lança `AccessDeniedException` → 403.

- `isAuthenticated()` — aceita qualquer usuário autenticado (qualquer role)
- `hasRole('ADMIN')` — aceita apenas usuários com a role `ROLE_ADMIN` (o prefixo `ROLE_` é adicionado automaticamente)
- `hasAnyRole('ADMIN', 'USER')` — aceita qualquer das roles listadas
- `hasAuthority('ROLE_ADMIN')` — igual a `hasRole`, mas sem adição de prefixo

**Pré-requisito:** `@EnableMethodSecurity` em `SecurityConfig` (adicionado no Passo 9). Sem ele, o Spring ignora silenciosamente todos os `@PreAuthorize`.

### BookController completo com @PreAuthorize

**Arquivo:** `src/main/java/br/umc/demo/controller/BookController.java`

```java
package br.umc.demo.controller;

import br.umc.demo.dto.BookRequest;
import br.umc.demo.dto.BookResponse;
import br.umc.demo.service.BookService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@CrossOrigin(origins = "*")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")          // qualquer usuário autenticado
    public List<BookResponse> listBooks() {
        return bookService.listBooks();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")          // qualquer usuário autenticado
    public BookResponse getBook(@PathVariable Long id) {
        return bookService.getBook(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")           // somente ADMIN
    public BookResponse createBook(@RequestBody BookRequest request) {
        return bookService.createBook(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")           // somente ADMIN
    public BookResponse updateBook(@PathVariable Long id, @RequestBody BookRequest request) {
        return bookService.updateBook(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")           // somente ADMIN
    public void deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
    }
}
```

### AuthorController completo com @PreAuthorize

**Arquivo:** `src/main/java/br/umc/demo/controller/AuthorController.java`

```java
package br.umc.demo.controller;

import br.umc.demo.dto.AuthorRequest;
import br.umc.demo.dto.AuthorResponse;
import br.umc.demo.dto.BookResponse;
import br.umc.demo.service.AuthorService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/authors")
@CrossOrigin(origins = "*")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")          // qualquer usuário autenticado
    public List<AuthorResponse> listAuthors() {
        return authorService.listAuthors();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")          // qualquer usuário autenticado
    public AuthorResponse getAuthor(@PathVariable Long id) {
        return authorService.getAuthor(id);
    }

    @GetMapping("/{id}/books")
    @PreAuthorize("isAuthenticated()")          // qualquer usuário autenticado
    public List<BookResponse> listBooksByAuthor(@PathVariable Long id) {
        return authorService.listBooksByAuthor(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")           // somente ADMIN
    public AuthorResponse createAuthor(@RequestBody AuthorRequest request) {
        return authorService.createAuthor(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")           // somente ADMIN
    public AuthorResponse updateAuthor(@PathVariable Long id, @RequestBody AuthorRequest request) {
        return authorService.updateAuthor(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")           // somente ADMIN
    public void deleteAuthor(@PathVariable Long id) {
        authorService.deleteAuthor(id);
    }
}
```

> **O que muda no `SecurityConfig`?** A única regra que fica em `SecurityConfig` é `.anyRequest().authenticated()` — o piso mínimo que exige autenticação para tudo. As restrições específicas por role saem do `SecurityConfig` e vão para cada método dos controllers via `@PreAuthorize`. Isso mantém o `SecurityConfig` enxuto e cada endpoint autodocumentado.

---

## 17. Passo 15 — Testes de Integração

**Arquivo:** `src/test/java/br/umc/demo/SecurityIntegrationTest.java`

**O que são testes de integração?** Diferente de testes unitários (que testam uma classe isolada com mocks), testes de integração sobem o contexto completo da aplicação — banco, segurança, controllers — e testam o comportamento real do sistema.

**Por que `RANDOM_PORT`?** Com `WebEnvironment.RANDOM_PORT`, o Spring Boot inicia um servidor HTTP real em uma porta aleatória. Isso permite testar o fluxo completo: filtro JWT → controller → serviço → banco → resposta HTTP.

**Por que `defaultStatusHandler(status -> true, (req, res) -> {})`?** Por padrão, o `RestClient` lança exceção em respostas 4xx e 5xx. Para verificar o status code nos testes (inclusive de erro), precisamos desabilitar esse comportamento.

```java
package br.umc.demo;

import br.umc.demo.dto.AuthResponse;
import br.umc.demo.dto.LoginRequest;
import br.umc.demo.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityIntegrationTest {

    @LocalServerPort
    private int port;

    private String base() {
        return "http://localhost:" + port;
    }

    private RestClient client() {
        return RestClient.builder()
                .baseUrl(base())
                .defaultStatusHandler(status -> true, (req, res) -> {})
                .build();
    }

    // 1. Registro bem-sucedido → 201 com token e role USER
    @Test
    void registroDeveRetornar201ETokenJWT() {
        RegisterRequest request = buildRegister(
                "João Silva", "joao.silva@teste.com", "11999991234", "Senha@123");

        ResponseEntity<AuthResponse> response = client().post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getToken()).isNotBlank();
        assertThat(response.getBody().getEmail()).isEqualTo("joao.silva@teste.com");
        assertThat(response.getBody().getRole()).isEqualTo("USER");
    }

    // 2. Email duplicado → 409 Conflict
    @Test
    void registroComEmailDuplicadoDeveRetornar409() {
        RegisterRequest request = buildRegister(
                "Maria Souza", "duplicado@teste.com", "11988887777", "Senha@456");

        client().post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).body(request).retrieve().toBodilessEntity();

        ResponseEntity<Void> response = client().post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).body(request).retrieve().toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // 3. Login com credenciais válidas → 200 com token
    @Test
    void loginComCredenciaisValidasDeveRetornar200EToken() {
        client().post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildRegister("Carlos", "carlos.login@teste.com", "11977776666", "Senha@789"))
                .retrieve().toBodilessEntity();

        LoginRequest login = buildLogin("carlos.login@teste.com", "Senha@789");
        ResponseEntity<AuthResponse> response = client().post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).body(login).retrieve().toEntity(AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getToken()).isNotBlank();
    }

    // 4. Senha errada → 401 Unauthorized
    @Test
    void loginComSenhaErradaDeveRetornar401() {
        ResponseEntity<Void> response = client().post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildLogin("admin@biblioteca.com", "senhaErrada999"))
                .retrieve().toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // 5. USER tentando criar livro → 403 Forbidden
    @Test
    void usuarioUSERDeveReceber403AoTentarCriarLivro() {
        ResponseEntity<AuthResponse> registerResponse = client().post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildRegister("Usuario Comum", "usuario.comum@teste.com", "11966665555", "Senha@User"))
                .retrieve().toEntity(AuthResponse.class);

        String tokenUser = registerResponse.getBody().getToken();

        ResponseEntity<Void> response = client().post().uri("/api/books")
                .header("Authorization", "Bearer " + tokenUser)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"title\":\"Proibido\",\"authorIds\":[],\"publisher\":\"Ed\",\"isbn\":\"X01\",\"summary\":\"\"}")
                .retrieve().toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // 6. ADMIN criando livro → 201 Created
    @Test
    void adminDevePoderCriarLivroEReceber201() {
        ResponseEntity<AuthResponse> loginResponse = client().post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildLogin("admin@biblioteca.com", "Admin@123"))
                .retrieve().toEntity(AuthResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String tokenAdmin = loginResponse.getBody().getToken();

        ResponseEntity<Void> response = client().post().uri("/api/books")
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"title\":\"Clean Code\",\"authorIds\":[],\"publisher\":\"PH\",\"isbn\":\"978-01\",\"summary\":\"\"}")
                .retrieve().toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // 7. Sem token → 401 Unauthorized
    @Test
    void acessoSemTokenDeveRetornar401() {
        ResponseEntity<Void> response = client().get().uri("/api/books")
                .retrieve().toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- helpers ---

    private RegisterRequest buildRegister(String fullName, String email, String phone, String password) {
        RegisterRequest r = new RegisterRequest();
        r.setFullName(fullName);
        r.setEmail(email);
        r.setPhone(phone);
        r.setPassword(password);
        return r;
    }

    private LoginRequest buildLogin(String email, String password) {
        LoginRequest l = new LoginRequest();
        l.setEmail(email);
        l.setPassword(password);
        return l;
    }
}
```

Para rodar os testes:

```bash
./mvnw test
```

Saída esperada:
```
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 18. Verificação Manual com curl

Com a aplicação rodando (`./mvnw spring-boot:run`), execute os seguintes comandos para verificar cada cenário:

### Registro (usuário comum)

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Ana Paula",
    "email": "ana@email.com",
    "phone": "11999991234",
    "password": "Minha@Senha123"
  }' | python3 -m json.tool
```

Resposta esperada (token truncado):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "ana@email.com",
  "role": "USER"
}
```

### Login como admin

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@biblioteca.com", "password": "Admin@123"}' \
  | python3 -m json.tool
```

Guarde o token retornado. Vamos chamá-lo de `TOKEN_ADMIN`.

### Listar livros com token USER (deve funcionar)

```bash
curl -s http://localhost:8080/api/books \
  -H "Authorization: Bearer <seu-token-user>" | python3 -m json.tool
```

### Tentar criar livro com token USER (deve retornar 403)

```bash
curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/books \
  -H "Authorization: Bearer <seu-token-user>" \
  -H "Content-Type: application/json" \
  -d '{"title": "Livro Proibido", "authorIds": [], "publisher": "Ed", "isbn": "X01", "summary": ""}'
# Esperado: 403
```

### Criar livro com token ADMIN (deve retornar 201)

```bash
curl -s -w "\nHTTP Status: %{http_code}\n" -X POST http://localhost:8080/api/books \
  -H "Authorization: Bearer <TOKEN_ADMIN>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Clean Code",
    "authorIds": [],
    "publisher": "Prentice Hall",
    "isbn": "9780132350884",
    "summary": "Guia de boas práticas."
  }'
# Esperado: 201
```

### Acesso sem token (deve retornar 401)

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/books
# Esperado: 401
```

### Verificar o conteúdo do token JWT

Cole o token em [jwt.io](https://jwt.io) para ver o payload decodificado:
```json
{
  "sub": "admin@biblioteca.com",
  "iat": 1711929600,
  "exp": 1712016000
}
```

> **Ponto de discussão em aula:** O payload é apenas Base64 — não está criptografado. Qualquer pessoa com o token pode decodificar e ver o email. O que protege é a **assinatura**: sem a chave secreta, ninguém consegue criar um token válido.

---

## 19. Diagrama do Fluxo Completo

```
FLUXO DE LOGIN:

Cliente                    API                         Banco
  │                         │                             │
  │  POST /api/auth/login   │                             │
  │  {"email", "password"}  │                             │
  │ ──────────────────────► │                             │
  │                         │  findByEmail(email)         │
  │                         │ ───────────────────────────►│
  │                         │◄─────────────────────────── │
  │                         │  compara senha com hash     │
  │                         │  Argon2 (não decripta —     │
  │                         │  refaz o hash e compara)    │
  │                         │  gera token JWT             │
  │◄────────────────────────│                             │
  │  {"token": "eyJ..."}    │                             │


FLUXO DE REQUISIÇÃO PROTEGIDA:

Cliente                 JwtFilter              SecurityContext      Controller
  │                        │                        │                  │
  │  GET /api/books        │                        │                  │
  │  Authorization: Bearer │                        │                  │
  │ ──────────────────────►│                        │                  │
  │                        │  extrai email do token │                  │
  │                        │  valida assinatura     │                  │
  │                        │  valida expiração      │                  │
  │                        │  setAuthentication()   │                  │
  │                        │ ──────────────────────►│                  │
  │                        │                        │  verifica role   │
  │                        │                        │ ────────────────►│
  │                        │                        │                  │  executa
  │◄───────────────────────────────────────────────────────────────────│
  │  200 OK + lista livros  │                       │                  │
```

---

## 20. Problemas Encontrados Durante a Implementação

Esta seção documenta os problemas reais que apareceram ao implementar — útil para entender por que algumas escolhas foram feitas de forma não óbvia.

### Problema 1 — `DaoAuthenticationProvider` no Spring Security 7

Na versão 7 do Spring Security (usado com Spring Boot 4), o `DaoAuthenticationProvider` passou a exigir o `UserDetailsService` como argumento obrigatório no construtor:

```java
// ERRADO (Spring Security < 7):
DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
provider.setUserDetailsService(userDetailsService); // método removido

// CORRETO (Spring Security 7+):
DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
provider.setPasswordEncoder(passwordEncoder());
```

**Motivo da mudança:** Eliminar configuração incompleta — antes, era possível criar um `DaoAuthenticationProvider` sem `UserDetailsService` e só descobrir o erro em tempo de execução.

---

### Problema 2 — Spring Security retornava 403 em vez de 401 para usuários não autenticados

Por padrão, quando nenhum mecanismo de autenticação explícito está configurado (sem form login, sem basic auth), o Spring Security usa o `Http403ForbiddenEntryPoint` que retorna 403 para TODOS os erros de autenticação e autorização.

**Solução:** Configurar explicitamente o `exceptionHandling` no `SecurityFilterChain`:

```java
.exceptionHandling(ex -> ex
    .authenticationEntryPoint((request, response, authException) ->
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Não autenticado"))
    .accessDeniedHandler((request, response, accessDeniedException) ->
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acesso negado"))
)
```

---

### Problema 3 — `ResponseStatusException(CONFLICT)` retornava 403

Ao tentar registrar um email duplicado, o teste recebia 403 em vez de 409. O motivo: quando uma exceção é lançada no controller, o Spring Boot encaminha internamente para `/error`. Como `/error` não estava na lista de rotas permitidas (`permitAll()`), o Spring Security bloqueava esse encaminhamento com 403.

**Solução:** Adicionar `/error` ao `permitAll()`:

```java
.requestMatchers("/error").permitAll()
```

---

### Problema 4 — `BadCredentialsException` não mapeava para 401

Quando `authenticationManager.authenticate()` falha, lança `BadCredentialsException`. Como essa exceção é lançada dentro do controller (não dentro da cadeia de filtros), o `ExceptionTranslationFilter` do Spring Security não a captura automaticamente.

**Solução:** Capturar explicitamente no serviço e relançar como `ResponseStatusException`:

```java
try {
    authenticationManager.authenticate(...);
} catch (BadCredentialsException e) {
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
}
```

---

*Roteiro elaborado para a aula prática de Segurança em APIs — UMC*

package com.vitral;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.vitral.entity.Account;
import com.vitral.enumerations.AccountType;
import com.vitral.repository.AccountRepository;
import com.vitral.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JwtService jwtService;

    private String tokenUsuario;
    private String tokenSebo;

    @BeforeEach
    void setUp() {
        Account usuario = accountRepository.save(Account.builder()
                .name("Usuario Teste")
                .username("usuario.seguranca")
                .email("usuario-seguranca@vitral.com")
                .passwordHash("hash")
                .type(AccountType.USUARIO)
                .emailVerificado(true)
                .build());
        Account sebo = accountRepository.save(Account.builder()
                .name("Sebo Teste")
                .username("sebo.seguranca")
                .email("sebo-seguranca@vitral.com")
                .passwordHash("hash")
                .type(AccountType.SEBO)
                .emailVerificado(true)
                .build());
        tokenUsuario = jwtService.generateToken(usuario.getEmail(), usuario.getType().name());
        tokenSebo = jwtService.generateToken(sebo.getEmail(), sebo.getType().name());
    }

    @Test
    @DisplayName("Rotas publicas de categorias devem aceitar visitante")
    void categorias_visitante_temAcesso() throws Exception {
        mockMvc.perform(get("/api/v1/categorias"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Favoritos devem bloquear visitante sem token")
    void favoritos_visitante_semToken_eBloqueado() throws Exception {
        mockMvc.perform(get("/api/v1/favoritos"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Rotas protegidas devem rejeitar token invalido")
    void cesta_tokenInvalido_eBloqueado() throws Exception {
        mockMvc.perform(get("/api/v1/cesta")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Dados da conta devem bloquear visitante sem token")
    void authMe_visitanteSemToken_eBloqueado() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Usuario autenticado deve acessar favoritos e cesta")
    void usuario_acessaRotasDeUsuario() throws Exception {
        mockMvc.perform(get("/api/v1/favoritos")
                        .header("Authorization", bearer(tokenUsuario)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/cesta")
                        .header("Authorization", bearer(tokenUsuario)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Conta SEBO nao deve acessar favoritos de usuario")
    void sebo_naoAcessaFavoritos() throws Exception {
        mockMvc.perform(get("/api/v1/favoritos")
                        .header("Authorization", bearer(tokenSebo)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Usuario nao deve acessar historico de vendas do sebo")
    void usuario_naoAcessaVendas() throws Exception {
        mockMvc.perform(get("/api/v1/pedidos/vendas")
                        .header("Authorization", bearer(tokenUsuario)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Usuario nao deve cadastrar produto reservado a conta SEBO")
    void usuario_naoCadastraProduto() throws Exception {
        mockMvc.perform(post("/api/v1/produtos")
                        .header("Authorization", bearer(tokenUsuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Livro",
                                  "preco": 10.00,
                                  "estoque": 1,
                                  "condicao": "NOVO"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Rota publica de classicos deve aceitar visitante sem token")
    void classicos_visitante_temAcesso() throws Exception {
        mockMvc.perform(get("/api/v1/produtos/classicos"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Home dinamica deve aceitar visitante sem token")
    void home_visitante_temAcesso() throws Exception {
        mockMvc.perform(get("/api/v1/home"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Token emitido antes da troca de senha deve ser rejeitado")
    void tokenComVersaoAntiga_eBloqueado() throws Exception {
        Account usuario = accountRepository.findByEmail("usuario-seguranca@vitral.com").orElseThrow();
        usuario.setAuthVersion(usuario.getAuthVersion() + 1);
        accountRepository.saveAndFlush(usuario);

        mockMvc.perform(get("/api/v1/favoritos")
                        .header("Authorization", bearer(tokenUsuario)))
                .andExpect(status().isForbidden());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}

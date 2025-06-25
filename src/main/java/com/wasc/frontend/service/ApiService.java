package com.wasc.frontend.service;

import com.wasc.frontend.model.LoginRequest;
import com.wasc.frontend.model.LoginResponse;
import com.wasc.frontend.model.Tarefa;
import com.wasc.frontend.model.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class ApiService {

    @Value("${api.base.url}")
    private String apiBaseUrl;

    @Autowired
    private RestTemplate restTemplate;

    // --- Autenticação ---

    public Optional<LoginResponse> login(String username, String password) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LoginRequest> requestEntity = new HttpEntity<>(loginRequest, headers);

        try {
            ResponseEntity<LoginResponse> response = restTemplate.exchange(
                    apiBaseUrl + "/auth/login",
                    HttpMethod.POST,
                    requestEntity,
                    LoginResponse.class
            );
            return Optional.ofNullable(response.getBody());
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            System.err.println("Login falhou: " + e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            System.err.println("Erro ao tentar login: " + e.getMessage());
            return Optional.empty();
        }
    }

    public boolean registerUser(Usuario usuario) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Para registrar, precisamos de um token (se o endpoint /register estiver protegido)
        // Se este for o PRIMEIRO REGISTRO, a API deve ter um usuário padrão via CommandLineRunner
        // OU o endpoint /register no backend deve ser permitAll() temporariamente.
        // Aqui não passamos token porque a ideia é que o primeiro user seja criado pelo backend ou por um admin
        // já logado no frontend que chama este método.
        // Para simplificar o fluxo de registro, se o endpoint de registro estiver protegido,
        // um usuário admin precisaria estar logado para criar outros usuários.
        
        HttpEntity<Usuario> requestEntity = new HttpEntity<>(usuario, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/auth/register",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException e) {
            System.err.println("Falha no registro: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            System.err.println("Erro ao registrar usuário: " + e.getMessage());
            return false;
        }
    }

    // --- Requisições Autenticadas (com JWT) ---

    private HttpHeaders getAuthHeaders(String jwtToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    public List<Usuario> getUsuarios(String jwtToken) {
        HttpHeaders headers = getAuthHeaders(jwtToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Usuario[]> response = restTemplate.exchange(
                    apiBaseUrl + "/usuarios",
                    HttpMethod.GET,
                    entity,
                    Usuario[].class
            );
            return Arrays.asList(response.getBody());
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            System.err.println("Erro de autenticação/autorização ao buscar usuários: " + e.getResponseBodyAsString());
            throw e; // Lançar exceção para ser tratada pelo controller
        } catch (Exception e) {
            System.err.println("Erro ao buscar usuários: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Tarefa> getTarefas(String jwtToken) {
        HttpHeaders headers = getAuthHeaders(jwtToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Tarefa[]> response = restTemplate.exchange(
                    apiBaseUrl + "/tarefas",
                    HttpMethod.GET,
                    entity,
                    Tarefa[].class
            );
            return Arrays.asList(response.getBody());
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            System.err.println("Erro de autenticação/autorização ao buscar tarefas: " + e.getResponseBodyAsString());
            throw e; // Lançar exceção para ser tratada pelo controller
        } catch (Exception e) {
            System.err.println("Erro ao buscar tarefas: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean saveTarefa(Tarefa tarefa, String jwtToken) {
        HttpHeaders headers = getAuthHeaders(jwtToken);
        HttpEntity<Tarefa> requestEntity = new HttpEntity<>(tarefa, headers);
        
        try {
            if (tarefa.getId() == null) { // Criar
                ResponseEntity<Tarefa> response = restTemplate.exchange(
                    apiBaseUrl + "/tarefas",
                    HttpMethod.POST,
                    requestEntity,
                    Tarefa.class
                );
                return response.getStatusCode().is2xxSuccessful();
            } else { // Atualizar
                ResponseEntity<Tarefa> response = restTemplate.exchange(
                    apiBaseUrl + "/tarefas/" + tarefa.getId(),
                    HttpMethod.PUT,
                    requestEntity,
                    Tarefa.class
                );
                return response.getStatusCode().is2xxSuccessful();
            }
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            System.err.println("Erro de autenticação/autorização ao salvar tarefa: " + e.getResponseBodyAsString());
            throw e;
        } catch (HttpClientErrorException e) {
            System.err.println("Falha ao salvar tarefa: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            System.err.println("Erro ao salvar tarefa: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteTarefa(Long id, String jwtToken) {
        HttpHeaders headers = getAuthHeaders(jwtToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                apiBaseUrl + "/tarefas/" + id,
                HttpMethod.DELETE,
                entity,
                Void.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            System.err.println("Erro de autenticação/autorização ao excluir tarefa: " + e.getResponseBodyAsString());
            throw e;
        } catch (HttpClientErrorException e) {
            System.err.println("Falha ao excluir tarefa: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            System.err.println("Erro ao excluir tarefa: " + e.getMessage());
            return false;
        }
    }
}
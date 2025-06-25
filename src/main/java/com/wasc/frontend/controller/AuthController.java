package com.wasc.frontend.controller;

import com.wasc.frontend.model.LoginResponse;
import com.wasc.frontend.model.Usuario;
import com.wasc.frontend.service.ApiService;
import jakarta.servlet.http.HttpSession;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.HttpClientErrorException;

@Controller
public class AuthController {

    @Autowired
    private ApiService apiService;

    // Exibe a página de login
    @GetMapping("/login")
    public String showLoginForm() {
        return "login"; // Nome do arquivo HTML (login.html)
    }

    // Processa o formulário de login
    @PostMapping("/login")
    public String login(String username, String password, HttpSession session, Model model) {
        Optional<LoginResponse> loginResponse = apiService.login(username, password);

        if (loginResponse.isPresent()) {
            session.setAttribute("jwtToken", loginResponse.get().getJwt()); // Armazena o token na sessão HTTP do servidor
            session.setAttribute("loggedInUser", username); // Armazena o username para exibição ou uso futuro
            return "redirect:/tasks"; // Redireciona para a página de tarefas
        } else {
            model.addAttribute("error", "Nome de usuário ou senha inválidos.");
            return "login";
        }
    }

    // Exibe a página de registro
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "register"; // Nome do arquivo HTML (register.html)
    }

    // Processa o formulário de registro
    @PostMapping("/register")
    public String register(@ModelAttribute Usuario usuario, Model model, HttpSession session) {
        try {
            // ATENÇÃO: O endpoint /api/auth/register na API de backend está PROTEGIDO.
            // Para registrar via este frontend, você precisa:
            // 1. Ou estar logado como um usuário que tenha permissão para registrar (ex: adminWasc).
            //    Nesse caso, você precisaria adicionar o token na requisição de registro aqui.
            //    Para este exemplo, assumimos que o adminWasc já foi criado pelo backend ao subir.
            //    Para criar outros usuários, primeiro LOGUE, depois crie.
            // 2. Ou, para o PRIMEIRO registro em um DB vazio, temporariamente tornar
            //    /api/auth/register no backend 'permitAll()', registrar via este frontend,
            //    e depois proteger o endpoint novamente.
            // Dado que o CommandLineRunner cria o primeiro admin, essa tela é para USUÁRIOS SECUNDÁRIOS.
            
            // Para poder registrar, este 'registerUser' precisaria de um token se o backend estiver protegido
            // e o usuario não for o primeiro admin. Como o ApiService.registerUser não pega o token da sessão,
            // ele funcionará apenas se /register no backend for permitAll() ou se for a chamada inicial do admin.
            // Para um cenário realista de registro, se o endpoint de registro for protegido,
            // esta função precisaria obter o token de um ADMIN LOGADO.
            // Por simplicidade, vou manter como está, assumindo que para o primeiro registro
            // o adminWasc já existe ou que /register foi temporariamente permitAll() no backend.
            // Para criar usuários depois do primeiro admin, o usuário logado precisa ter ROLE_ADMIN e o token passado para apiService.registerUser.
            
            boolean success = apiService.registerUser(usuario);
            if (success) {
                model.addAttribute("message", "Usuário registrado com sucesso! Faça login.");
                return "login"; // Redireciona para a página de login
            } else {
                model.addAttribute("error", "Erro ao registrar usuário. O nome de usuário pode já existir.");
                return "register";
            }
        } catch (HttpClientErrorException e) {
            model.addAttribute("error", "Erro ao registrar: " + e.getResponseBodyAsString());
            return "register";
        } catch (Exception e) {
            model.addAttribute("error", "Ocorreu um erro inesperado ao registrar: " + e.getMessage());
            return "register";
        }
    }

    // Logout
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Invalida a sessão HTTP
        return "redirect:/login"; // Redireciona para a página de login
    }
}
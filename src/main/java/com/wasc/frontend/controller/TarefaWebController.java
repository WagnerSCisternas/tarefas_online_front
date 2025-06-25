package com.wasc.frontend.controller;

import com.wasc.frontend.model.Tarefa;
import com.wasc.frontend.model.Usuario;
import com.wasc.frontend.service.ApiService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Optional;

@Controller
public class TarefaWebController {

    @Autowired
    private ApiService apiService;

    // Mapeamento da URL raiz para a página de tarefas (após login)
    @GetMapping("/")
    public String redirectToTasks() {
        return "redirect:/tasks";
    }

    // Exibe a lista de tarefas
    @GetMapping("/tasks")
    public String listTasks(Model model, HttpSession session) {
        String jwtToken = (String) session.getAttribute("jwtToken");

        if (jwtToken == null) {
            return "redirect:/login"; // Se não há token, redireciona para login
        }

        try {
            List<Tarefa> tarefas = apiService.getTarefas(jwtToken);
            List<Usuario> usuarios = apiService.getUsuarios(jwtToken);

            model.addAttribute("tarefas", tarefas);
            model.addAttribute("usuarios", usuarios); // Para o dropdown de usuários
            model.addAttribute("tarefa", new Tarefa()); // Para o formulário de nova tarefa
            model.addAttribute("loggedInUser", session.getAttribute("loggedInUser")); // Para exibir o user logado
            return "tasks"; // Nome do arquivo HTML (tasks.html)
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            session.invalidate(); // Invalida a sessão se o token não é mais válido
            model.addAttribute("error", "Sessão expirada ou não autorizada. Faça login novamente.");
            return "login";
        } catch (Exception e) {
            model.addAttribute("error", "Erro ao carregar tarefas: " + e.getMessage());
            return "tasks"; // Permanece na tela de tarefas com erro
        }
    }

    // Adiciona ou atualiza uma tarefa
    @PostMapping("/tasks")
    public String saveTask(@ModelAttribute Tarefa tarefa, @RequestParam Long usuarioId, HttpSession session, Model model) {
        String jwtToken = (String) session.getAttribute("jwtToken");
        if (jwtToken == null) {
            return "redirect:/login";
        }

        // Garante que o objeto Usuario na Tarefa tenha apenas o ID
        Usuario usuarioAssociado = new Usuario();
        usuarioAssociado.setId(usuarioId);
        tarefa.setUsuario(usuarioAssociado);

        try {
            boolean success = apiService.saveTarefa(tarefa, jwtToken);
            if (success) {
                model.addAttribute("message", "Tarefa salva com sucesso!");
            } else {
                model.addAttribute("error", "Erro ao salvar tarefa.");
            }
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            session.invalidate();
            model.addAttribute("error", "Sessão expirada. Faça login novamente.");
            return "login";
        } catch (HttpClientErrorException e) {
            model.addAttribute("error", "Erro ao salvar tarefa: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            model.addAttribute("error", "Erro inesperado ao salvar tarefa: " + e.getMessage());
        }

        return "redirect:/tasks"; // Redireciona para atualizar a lista
    }

    // Pré-preenche o formulário para edição
    @GetMapping("/tasks/edit/{id}")
    public String editTask(@PathVariable Long id, Model model, HttpSession session) {
        String jwtToken = (String) session.getAttribute("jwtToken");
        if (jwtToken == null) {
            return "redirect:/login";
        }

        try {
            List<Tarefa> tarefas = apiService.getTarefas(jwtToken);
            Optional<Tarefa> tarefaToEdit = tarefas.stream().filter(t -> t.getId().equals(id)).findFirst();
            List<Usuario> usuarios = apiService.getUsuarios(jwtToken);

            if (tarefaToEdit.isPresent()) {
                model.addAttribute("tarefa", tarefaToEdit.get());
                model.addAttribute("usuarios", usuarios);
                model.addAttribute("tarefas", tarefas); // Para manter a lista visível
                model.addAttribute("loggedInUser", session.getAttribute("loggedInUser"));
                return "tasks"; // Retorna para a mesma página, mas com o formulário preenchido
            } else {
                model.addAttribute("error", "Tarefa não encontrada.");
                return "redirect:/tasks";
            }
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            session.invalidate();
            model.addAttribute("error", "Sessão expirada. Faça login novamente.");
            return "login";
        } catch (Exception e) {
            model.addAttribute("error", "Erro ao carregar tarefa para edição: " + e.getMessage());
            return "redirect:/tasks";
        }
    }

    // Exclui uma tarefa
    @GetMapping("/tasks/delete/{id}")
    public String deleteTask(@PathVariable Long id, HttpSession session, Model model) {
        String jwtToken = (String) session.getAttribute("jwtToken");
        if (jwtToken == null) {
            return "redirect:/login";
        }

        try {
            boolean success = apiService.deleteTarefa(id, jwtToken);
            if (success) {
                model.addAttribute("message", "Tarefa excluída com sucesso!");
            } else {
                model.addAttribute("error", "Erro ao excluir tarefa.");
            }
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            session.invalidate();
            model.addAttribute("error", "Sessão expirada. Faça login novamente.");
            return "login";
        } catch (HttpClientErrorException e) {
            model.addAttribute("error", "Erro ao excluir tarefa: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            model.addAttribute("error", "Erro inesperado ao excluir tarefa: " + e.getMessage());
        }

        return "redirect:/tasks";
    }
}
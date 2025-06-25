package com.wasc.frontend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tarefa {
    private Long id;
    private String titulo; 
    private String descricao;
    private LocalDate data;
    private boolean status;
    private Usuario usuario;
}
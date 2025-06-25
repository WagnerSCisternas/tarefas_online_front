package com.wasc.frontend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    private Long id;
    private String nome;
    private LocalDate dataNascimento;
    private boolean ativo;

}
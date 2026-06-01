package com.matheusmelo.todolist.exception;

/**
 * Lancada quando um recurso de dominio referenciado nao existe
 * (per architecture-conventions.md § Error Handling — tipos de excecao por dominio).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

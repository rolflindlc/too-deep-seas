package com.rolflin.pesca.core;

/** Efecto de un accesorio (no gastado) sobre una estadística. */
public record Modifier(Stat stat, double value) {
}

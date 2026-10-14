package it.docai.consumo;

import java.math.BigDecimal;

public record ConsumoUtente(String proprietario, String utente, String operazione, String modello,
                            long chiamate, long tokenIn, long tokenOut, BigDecimal costoStimato) { }


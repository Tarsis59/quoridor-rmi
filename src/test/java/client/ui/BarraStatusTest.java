package client.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BarraStatusTest {

    @Test
    void mensagemEErro() {
        BarraStatus s = new BarraStatus();
        s.setMensagem("Vez do Jogador 1");
        assertEquals("Vez do Jogador 1", s.getTexto());
        assertEquals(EstiloUI.TEXTO_PRINCIPAL, s.getForeground());
        s.setErro("Jogada inválida");
        assertEquals(EstiloUI.ERRO, s.getForeground());
    }
}

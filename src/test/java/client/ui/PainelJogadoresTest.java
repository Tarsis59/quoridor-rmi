package client.ui;

import common.EstadoJogo;
import common.Posicao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PainelJogadoresTest {

    @Test
    void destacaVezEVoce() {
        Posicao[] pos = { new Posicao(8, 4), new Posicao(0, 4), new Posicao(4, 8), new Posicao(4, 0) };
        EstadoJogo e = new EstadoJogo(pos, List.of(), new int[]{10, 9, 8, 7},
                3, EstadoJogo.Status.EM_ANDAMENTO, 0, new String[]{"Ana", "Bob", "Cid", "Duda"});
        PainelJogadores p = new PainelJogadores();
        p.atualizar(e, 2);
        assertEquals("VEZ", p.badge(3).getText());
        assertEquals("", p.badge(1).getText());
        assertEquals(EstiloUI.VOCE_FUNDO, p.linha(2).getBackground());
    }
}

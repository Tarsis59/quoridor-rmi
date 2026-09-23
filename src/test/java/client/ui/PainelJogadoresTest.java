package client.ui;

import common.EstadoJogo;
import common.Posicao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PainelJogadoresTest {

    @Test
    void destacaVezEVoce() {
        Posicao[] pos = { new Posicao(8, 4), new Posicao(0, 4), new Posicao(4, 8), new Posicao(4, 0) };
        EstadoJogo e = new EstadoJogo(pos, List.of(), new int[0], new int[]{5, 4, 1, 0},
                3, EstadoJogo.Status.EM_ANDAMENTO, 0, new String[]{"Ana", "Bob", "Cid", "Duda"});
        PainelJogadores p = new PainelJogadores();
        p.atualizar(e, 2);
        assertEquals("VEZ", p.badge(3).getText());
        assertEquals("", p.badge(1).getText());
        assertEquals(EstiloUI.VOCE_FUNDO, p.linha(2).getBackground());
        assertEquals("Bob (você)", p.nome(2).getText());
        assertTrue(p.meta(3).getText().contains("1 cerca"));
        assertTrue(p.meta(4).getText().contains("0 cercas"));
        assertTrue(p.meta(1).getText().startsWith("faltam 8 passos"), p.meta(1).getText());
        assertEquals(1, p.cercasDesenhadas(3), "inventário desenhado acompanha o estado");
        assertEquals(5, p.cercasDesenhadas(1));
    }
}

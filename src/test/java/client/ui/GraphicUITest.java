package client.ui;

import common.EstadoJogo;
import common.Posicao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphicUITest {

    private EstadoJogo estado() {
        Posicao[] pos = { new Posicao(8, 4), new Posicao(0, 4), new Posicao(4, 8), new Posicao(4, 0) };
        return new EstadoJogo(pos, List.of(), new int[0], new int[]{5, 5, 5, 5},
                1, EstadoJogo.Status.EM_ANDAMENTO, 0, new String[]{"Ana", "Bob", "Cid", "Duda"});
    }

    @Test
    void modoAutomaticoRefleteConstrutor() {
        assertTrue(new GraphicUI("auto").modoAutomatico());
        assertFalse(new GraphicUI("manual").modoAutomatico());
    }

    @Test
    void recebeEstadoEEntregaNoGetter() {
        GraphicUI g = new GraphicUI("manual");
        g.setSessao(new common.Sessao(2, "token"));
        EstadoJogo e = estado();
        g.novoEstado(e);
        assertSame(e, g.getEstadoAtual());
    }
}

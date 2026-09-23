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

    @Test
    void historicoRegistraOInicioDaPartida() throws Exception {
        GraphicUI g = new GraphicUI("manual");
        g.setSessao(new common.Sessao(1, "token"));
        g.novoEstado(estado());
        javax.swing.SwingUtilities.invokeAndWait(() -> { }); // espera a EDT aplicar o estado
        assertTrue(g.textosHistorico().contains("A partida começou!"));
        assertTrue(g.status().getTexto().startsWith("Sua vez!"), g.status().getTexto());
    }
}

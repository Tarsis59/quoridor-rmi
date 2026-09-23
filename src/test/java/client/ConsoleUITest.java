package client;

import common.Cerca;
import common.EstadoJogo;
import common.Orientacao;
import common.Posicao;
import common.Sessao;
import engine.Tabuleiro;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConsoleUITest {

    @Test
    void interpretaCercaSoComOrientacaoValida() {
        assertEquals(new Cerca(new Posicao(3, 4), Orientacao.HORIZONTAL), ConsoleUI.interpretarCerca("cerca 3 4 h"));
        assertEquals(new Cerca(new Posicao(0, 7), Orientacao.VERTICAL), ConsoleUI.interpretarCerca("cerca 0 7 vertical"));
        assertNull(ConsoleUI.interpretarCerca("cerca 3 4 x"), "orientação desconhecida não vira vertical");
        assertNull(ConsoleUI.interpretarCerca("cerca 3 h"));
        assertNull(ConsoleUI.interpretarCerca("cerca a b h"));
    }

    @Test
    void direcaoPulaPeaoQuandoPossivel() {
        Posicao[] pos = { new Posicao(5, 4), new Posicao(4, 4), new Posicao(4, 8), new Posicao(4, 0) };
        EstadoJogo e = new EstadoJogo(pos, List.of(), new int[0], new int[]{5, 5, 5, 5}, 1,
                EstadoJogo.Status.EM_ANDAMENTO, 0, new String[]{"A", "B", "C", "D"});
        ConsoleUI ui = new ConsoleUI();
        ui.setSessao(new Sessao(1, "t"));
        Tabuleiro t = Tabuleiro.aPartirDe(pos, List.of());
        assertEquals(new Posicao(3, 4), ui.interpretarDestino("mover cima", e, t), "pula o J2");
        assertEquals(new Posicao(5, 3), ui.interpretarDestino("mover esquerda", e, t));
        assertEquals(new Posicao(2, 6), ui.interpretarDestino("mover 2 6", e, t));
        assertNull(ui.interpretarDestino("mover para cima", e, t));
    }
}

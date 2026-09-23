package client.ui;

import common.Cerca;
import common.EstadoJogo;
import common.Orientacao;
import common.Posicao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoricoTest {
    private static final String[] NOMES = {"Ana", "Bob", "Cid", "Duda"};
    private static final Posicao[] INICIO = {
            new Posicao(8, 4), new Posicao(0, 4), new Posicao(4, 8), new Posicao(4, 0)};

    private static EstadoJogo estado(Posicao[] pos, List<Cerca> cercas, int[] donos, EstadoJogo.Status st,
                                     int vencedor, boolean[] ativos, int registrados) {
        return new EstadoJogo(pos, cercas, donos, new int[]{5, 5, 5, 5}, 1, st, vencedor, NOMES, ativos, registrados);
    }

    private static final boolean[] TODOS = {true, true, true, true};

    @Test
    void salaDeEsperaEInicio() {
        EstadoJogo espera = estado(INICIO, List.of(), new int[0], EstadoJogo.Status.AGUARDANDO, 0, TODOS, 2);
        List<Historico.Evento> e1 = Historico.diferencas(null, espera);
        assertEquals("Sala de espera: 2/4 jogadores", e1.get(0).texto());
        EstadoJogo jogo = estado(INICIO, List.of(), new int[0], EstadoJogo.Status.EM_ANDAMENTO, 0, TODOS, 4);
        assertEquals("A partida começou!", Historico.diferencas(espera, jogo).get(0).texto());
    }

    @Test
    void movimentoECercaComAutor() {
        EstadoJogo a = estado(INICIO, List.of(), new int[0], EstadoJogo.Status.EM_ANDAMENTO, 0, TODOS, 4);
        Posicao[] depois = INICIO.clone();
        depois[0] = new Posicao(7, 4);
        EstadoJogo b = estado(depois, List.of(), new int[0], EstadoJogo.Status.EM_ANDAMENTO, 0, TODOS, 4);
        Historico.Evento mov = Historico.diferencas(a, b).get(0);
        assertEquals(1, mov.idJogador());
        assertEquals("Ana moveu (8,4) → (7,4)", mov.texto());

        EstadoJogo c = estado(depois, List.of(new Cerca(new Posicao(3, 4), Orientacao.VERTICAL)), new int[]{2},
                EstadoJogo.Status.EM_ANDAMENTO, 0, TODOS, 4);
        Historico.Evento cerca = Historico.diferencas(b, c).get(0);
        assertEquals(2, cerca.idJogador());
        assertEquals("Bob pôs cerca V em (3,4)", cerca.texto());
    }

    @Test
    void saidaEVitoria() {
        EstadoJogo a = estado(INICIO, List.of(), new int[0], EstadoJogo.Status.EM_ANDAMENTO, 0, TODOS, 4);
        EstadoJogo b = estado(INICIO, List.of(), new int[0], EstadoJogo.Status.FINALIZADO, 3,
                new boolean[]{false, false, true, false}, 4);
        List<Historico.Evento> ev = Historico.diferencas(a, b);
        assertEquals(4, ev.size(), "3 saídas + fim");
        assertTrue(ev.get(0).texto().contains("Ana saiu"));
        assertEquals("Cid venceu a partida!", ev.get(3).texto());
        assertTrue(Historico.diferencas(b, b).isEmpty(), "sem mudança, sem evento");
    }

    @Test
    void painelGuardaSoOsMaisRecentes() {
        PainelHistorico p = new PainelHistorico();
        for (int i = 0; i < PainelHistorico.MAX_ITENS + 5; i++) {
            p.adicionar(List.of(new Historico.Evento(1, "jogada " + i)));
        }
        assertEquals(PainelHistorico.MAX_ITENS, p.itens().size());
        assertEquals("jogada " + (PainelHistorico.MAX_ITENS + 4), p.itens().get(0).texto(), "mais recente em cima");
    }
}

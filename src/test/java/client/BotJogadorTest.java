package client;

import common.Cerca;
import common.EstadoJogo;
import common.Posicao;
import common.Sessao;
import engine.Partida;
import engine.Tabuleiro;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotJogadorTest {

    private static EstadoJogo estado(Posicao[] pos, int[] cercas) {
        return new EstadoJogo(pos, List.of(), new int[0], cercas, 1, EstadoJogo.Status.EM_ANDAMENTO, 0,
                new String[]{"A", "B", "C", "D"});
    }

    @Test
    void movimentoSempreAproximaDaMeta() {
        Tabuleiro t = new Tabuleiro();
        BotJogador bot = new BotJogador(new Sessao(1, "t"), new Random(1));
        assertEquals(new Posicao(7, 4), bot.escolherMovimento(t));
    }

    @Test
    void naoGastaCercaQuandoEstaNaFrente() {
        Posicao[] pos = { new Posicao(2, 4), new Posicao(0, 4), new Posicao(4, 8), new Posicao(4, 0) };
        BotJogador bot = new BotJogador(new Sessao(1, "t"), new Random(1));
        EstadoJogo e = estado(pos, new int[]{5, 5, 5, 5});
        assertNull(bot.escolherCerca(Tabuleiro.aPartirDe(pos, List.of()), e));
    }

    @Test
    void cercaAtrasaOAdversarioQueEstaPertoDeVencer() {
        // J2 está a 1 passo de vencer; o bot (J1) está longe e deve bloqueá-lo.
        Posicao[] pos = { new Posicao(8, 0), new Posicao(7, 4), new Posicao(4, 8), new Posicao(4, 0) };
        Tabuleiro t = Tabuleiro.aPartirDe(pos, List.of());
        BotJogador bot = new BotJogador(new Sessao(1, "t"), new Random(1));
        Cerca c = bot.escolherCerca(t, estado(pos, new int[]{5, 5, 5, 5}));
        assertNotNull(c);
        assertTrue(t.podeColocarCerca(c));
        Tabuleiro depois = t.copiar();
        depois.colocarCerca(c);
        assertTrue(depois.distanciaMinimaAteAlvo(2) > t.distanciaMinimaAteAlvo(2), "a cerca atrasa o J2");
    }

    @Test
    void semCercasNaoTentaColocar() {
        Posicao[] pos = { new Posicao(8, 0), new Posicao(7, 4), new Posicao(4, 8), new Posicao(4, 0) };
        BotJogador bot = new BotJogador(new Sessao(1, "t"), new Random(1));
        assertNull(bot.escolherCerca(Tabuleiro.aPartirDe(pos, List.of()), estado(pos, new int[]{0, 5, 5, 5})));
    }

    @Test
    void quatroBotsTerminamUmaPartidaLocal() throws Exception {
        // Simula a partida inteira sem rede para garantir que os bots sempre terminam o jogo.
        for (long semente = 0; semente < 5; semente++) {
            Partida partida = new Partida();
            partida.iniciar();
            BotJogador[] bots = new BotJogador[4];
            for (int i = 0; i < 4; i++) bots[i] = new BotJogador(new Sessao(i + 1, "t"), new Random(semente + i));
            common.GameServer local = new ServidorLocal(partida);
            int jogadas = 0;
            while (partida.getStatus() == EstadoJogo.Status.EM_ANDAMENTO && jogadas < 1000) {
                EstadoJogo e = partida.gerarEstado();
                bots[e.getJogadorDaVez() - 1].executarJogada(local, e);
                jogadas++;
            }
            assertEquals(EstadoJogo.Status.FINALIZADO, partida.getStatus(), "semente " + semente);
            assertTrue(partida.getVencedor() >= 1 && partida.getVencedor() <= 4);
        }
    }

    /** GameServer em memória sobre uma Partida, ignorando sessão (só para o teste do bot). */
    private record ServidorLocal(Partida partida) implements common.GameServer {
        @Override public Sessao registrar(common.ClientCallback c, String n) { throw new UnsupportedOperationException(); }
        @Override public void mover(Sessao s, Posicao d) throws common.JogadaInvalidaException { partida.mover(s.idJogador(), d); }
        @Override public void colocarCerca(Sessao s, Cerca c) throws common.JogadaInvalidaException { partida.colocarCerca(s.idJogador(), c); }
        @Override public EstadoJogo obterEstado() { return partida.gerarEstado(); }
    }
}

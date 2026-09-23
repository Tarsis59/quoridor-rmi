package engine;

import common.EstadoJogo;
import common.JogadaInvalidaException;
import common.Posicao;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartidaTest {

    private void moverOutrosParaOndeDa(Partida p) throws Exception {
        for (int outro = 2; outro <= 4; outro++) {
            int id = outro; // variável efetivamente final para uso na lambda
            Tabuleiro t = Tabuleiro.aPartirDe(p.gerarEstado().getPosicoes(), p.gerarEstado().getCercas());
            List<Posicao> validos = t.movimentosValidos(id);
            assertTrue(!validos.isEmpty(), "Jogador " + id + " deveria ter movimento");
            Posicao escolha = validos.stream()
                    .max(Comparator.comparingInt(pos -> t.distanciaMinimaAteAlvo(id, pos)))
                    .orElseThrow();
            p.mover(id, escolha);
        }
    }

    @Test
    void jogadorForaDaVezDeveSerRejeitado() {
        Partida p = new Partida();
        p.iniciar();
        assertThrows(JogadaInvalidaException.class, () -> p.mover(2, new Posicao(1, 4)));
        assertThrows(JogadaInvalidaException.class, () -> p.colocarCerca(3, new common.Cerca(
                new Posicao(4, 4), common.Orientacao.HORIZONTAL)));
    }

    @Test
    void turnosAvancamEmCiclo() throws Exception {
        Partida p = new Partida();
        p.iniciar();
        for (int i = 0; i < 4; i++) {
            int vez = p.getJogadorDaVez();
            Tabuleiro t = Tabuleiro.aPartirDe(p.gerarEstado().getPosicoes(), p.gerarEstado().getCercas());
            p.mover(vez, t.movimentosValidos(vez).get(0));
        }
        assertEquals(1, p.getJogadorDaVez());
    }

    @Test
    void jogadorQueAlcancaAValaLinhaVence() throws Exception {
        Partida p = new Partida();
        p.iniciar();
        int rodadas = 0;
        while (p.getStatus() != EstadoJogo.Status.FINALIZADO && rodadas < 30) {
            Tabuleiro t = Tabuleiro.aPartirDe(p.gerarEstado().getPosicoes(), p.gerarEstado().getCercas());
            Posicao melhor = t.movimentosValidos(1).stream()
                    .min(Comparator.comparingInt(Posicao::linha))
                    .orElseThrow(() -> new AssertionError("J1 sem movimentos"));
            p.mover(1, melhor);
            if (p.getStatus() == EstadoJogo.Status.FINALIZADO) break;
            moverOutrosParaOndeDa(p);
            rodadas++;
        }
        assertEquals(EstadoJogo.Status.FINALIZADO, p.getStatus(), "J1 deveria vencer ao alcançar a linha 0");
        assertEquals(1, p.getVencedor());
        assertThrows(JogadaInvalidaException.class, () -> p.mover(2, new Posicao(8, 4)),
                "jogada após o fim deve ser rejeitada");
    }

    @Test
    void movimentoInvalidoDeveSerRejeitado() {
        Partida p = new Partida();
        p.iniciar();
        assertThrows(JogadaInvalidaException.class, () -> p.mover(1, new Posicao(8, 4)), "mesma casa");
        assertThrows(JogadaInvalidaException.class, () -> p.mover(1, new Posicao(9, 4)), "fora do tabuleiro");
    }

    @Test
    void cercaColocadaRegistraODono() throws Exception {
        Partida p = new Partida();
        p.iniciar();
        p.colocarCerca(1, new common.Cerca(new Posicao(0, 0), common.Orientacao.HORIZONTAL));
        EstadoJogo e = p.gerarEstado();
        assertEquals(1, e.getCercas().size());
        assertEquals(1, e.getDonoCerca(0));
        assertEquals(Partida.CERCAS_INICIAIS - 1, e.getCercasRestantes(1));
    }

    @Test
    void estadoInicialTemQuatroJogadoresComCincoCercas() {
        Partida p = new Partida();
        EstadoJogo e = p.gerarEstado();
        assertEquals(EstadoJogo.Status.AGUARDANDO, e.getStatus());
        for (int i = 1; i <= 4; i++) {
            assertEquals(5, e.getCercasRestantes(i), "regra oficial: 5 cercas por jogador com 4 jogadores");
        }
        assertEquals(new Posicao(8, 4), e.getPosicao(1));
        assertEquals(new Posicao(0, 4), e.getPosicao(2));
        assertEquals(new Posicao(4, 8), e.getPosicao(3));
        assertEquals(new Posicao(4, 0), e.getPosicao(4));
    }

    @Test
    void cadaJogadorSoPodeColocarCincoCercas() throws Exception {
        Partida p = new Partida();
        p.iniciar();
        // J1 coloca 5 cercas (os outros só andam) e a 6ª é recusada.
        int[][] bases = {{0, 0}, {0, 2}, {0, 5}, {2, 0}, {2, 6}};
        for (int[] b : bases) {
            p.colocarCerca(1, new common.Cerca(new Posicao(b[0], b[1]), common.Orientacao.HORIZONTAL));
            moverOutrosParaOndeDa(p);
        }
        assertEquals(0, p.gerarEstado().getCercasRestantes(1));
        JogadaInvalidaException ex = assertThrows(JogadaInvalidaException.class, () ->
                p.colocarCerca(1, new common.Cerca(new Posicao(6, 0), common.Orientacao.HORIZONTAL)));
        assertTrue(ex.getMessage().contains("cercas"));
        assertEquals(5, p.gerarEstado().getCercas().size());
    }

    @Test
    void jogadorDesconectadoTemAVezPulada() throws Exception {
        Partida p = new Partida();
        p.iniciar();
        p.removerJogador(2);
        p.mover(1, new Posicao(7, 4));
        assertEquals(3, p.getJogadorDaVez(), "J2 saiu: a vez vai direto para o J3");
        assertFalse(p.gerarEstado().isAtivo(2));
    }

    @Test
    void desconexaoDoJogadorDaVezPassaAVez() {
        Partida p = new Partida();
        p.iniciar();
        p.removerJogador(1);
        assertEquals(2, p.getJogadorDaVez());
    }

    @Test
    void ultimoJogadorConectadoVencePorWO() {
        Partida p = new Partida();
        p.iniciar();
        p.removerJogador(1);
        p.removerJogador(2);
        assertEquals(EstadoJogo.Status.EM_ANDAMENTO, p.getStatus());
        p.removerJogador(4);
        assertEquals(EstadoJogo.Status.FINALIZADO, p.getStatus());
        assertEquals(3, p.getVencedor());
    }

    @Test
    void destinoOuCercaNulosSaoRejeitados() {
        Partida p = new Partida();
        p.iniciar();
        assertThrows(JogadaInvalidaException.class, () -> p.mover(1, null));
        assertThrows(JogadaInvalidaException.class, () -> p.colocarCerca(1, null));
        assertEquals(1, p.getJogadorDaVez(), "jogada rejeitada não consome a vez");
    }

    @Test
    void jogadaAntesDoInicioERejeitada() {
        Partida p = new Partida();
        assertThrows(JogadaInvalidaException.class, () -> p.mover(1, new Posicao(7, 4)));
    }

    @Test
    void nomeESanitizado() {
        assertEquals("Jogador 3", Partida.sanitizarNome("   ", 3));
        assertEquals("Jogador 1", Partida.sanitizarNome(null, 1));
        assertEquals("AnaBia", Partida.sanitizarNome("Ana\nBia\u0007", 2));
        assertEquals(Partida.TAMANHO_MAX_NOME, Partida.sanitizarNome("x".repeat(100), 1).length());
    }
}

package engine;

import common.EstadoJogo;
import common.JogadaInvalidaException;
import common.Posicao;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void estadoInicialTemQuatroJogadoresComDezCercas() {
        Partida p = new Partida();
        EstadoJogo e = p.gerarEstado();
        assertEquals(EstadoJogo.Status.AGUARDANDO, e.getStatus());
        for (int i = 1; i <= 4; i++) {
            assertEquals(10, e.getCercasRestantes(i));
        }
        assertEquals(new Posicao(8, 4), e.getPosicao(1));
        assertEquals(new Posicao(0, 4), e.getPosicao(2));
        assertEquals(new Posicao(4, 8), e.getPosicao(3));
        assertEquals(new Posicao(4, 0), e.getPosicao(4));
    }
}

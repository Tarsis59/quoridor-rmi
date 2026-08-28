package engine;

import common.Cerca;
import common.Orientacao;
import common.Posicao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TabuleiroTest {

    @Test
    void movimentoSimplesNasQuatroDirecoes() {
        Tabuleiro t = new Tabuleiro();
        List<Posicao> validos = t.movimentosValidos(1); // J1 em (8,4)
        assertTrue(validos.contains(new Posicao(7, 4)), "deve poder subir");
        assertTrue(validos.contains(new Posicao(8, 3)), "deve poder ir para a esquerda");
        assertTrue(validos.contains(new Posicao(8, 5)), "deve poder ir para a direita");
        assertFalse(validos.contains(new Posicao(9, 4)), "não pode sair do tabuleiro");
    }

    @Test
    void bordaDoTabuleiroNaoPermiteSair() {
        Tabuleiro t = new Tabuleiro();
        t.moverPeao(1, new Posicao(0, 0));
        List<Posicao> validos = t.movimentosValidos(1);
        assertTrue(validos.contains(new Posicao(1, 0)));
        assertTrue(validos.contains(new Posicao(0, 1)));
        assertFalse(validos.contains(new Posicao(-1, 0)));
        assertFalse(validos.contains(new Posicao(0, -1)));
    }

    @Test
    void paredeBloqueiaMovimento() {
        Tabuleiro t = new Tabuleiro();
        assertTrue(t.podeColocarCerca(new Cerca(new Posicao(7, 4), Orientacao.HORIZONTAL)));
        t.colocarCerca(new Cerca(new Posicao(7, 4), Orientacao.HORIZONTAL));
        List<Posicao> validos = t.movimentosValidos(1); // J1 em (8,4)
        assertFalse(validos.contains(new Posicao(7, 4)), "parede H(7,4) bloqueia subida do J1");
        assertTrue(validos.contains(new Posicao(8, 3)));
        assertTrue(validos.contains(new Posicao(8, 5)));
    }

    @Test
    void puloRetoSobrePeao() {
        Tabuleiro t = new Tabuleiro();
        t.moverPeao(1, new Posicao(5, 4));
        t.moverPeao(2, new Posicao(4, 4));
        List<Posicao> validos = t.movimentosValidos(1);
        assertTrue(validos.contains(new Posicao(3, 4)), "deve pular direto para trás do adversário");
    }

    @Test
    void puloLateralQuandoAtrasBloqueadoPorParede() {
        Tabuleiro t = new Tabuleiro();
        t.moverPeao(1, new Posicao(5, 4));
        t.moverPeao(2, new Posicao(4, 4));
        assertTrue(t.podeColocarCerca(new Cerca(new Posicao(3, 4), Orientacao.HORIZONTAL)));
        t.colocarCerca(new Cerca(new Posicao(3, 4), Orientacao.HORIZONTAL)); // bloqueia casa atrás
        List<Posicao> validos = t.movimentosValidos(1);
        assertTrue(validos.contains(new Posicao(4, 3)), "pulo lateral esquerdo");
        assertTrue(validos.contains(new Posicao(4, 5)), "pulo lateral direito");
        assertFalse(validos.contains(new Posicao(3, 4)), "pulo reto bloqueado");
    }

    @Test
    void puloBloqueadoPorParedesDosDoisLados() {
        Posicao[] posicoes = {
            new Posicao(5, 4), // J1
            new Posicao(4, 4), // J2
            new Posicao(4, 8), // J3
            new Posicao(4, 0)  // J4
        };
        // Cercas montadas como cenário (aPartirDe), não via podeColocarCerca:
        // bloquear as 3 opções de pulo do J1 isolaria o J2, e a garantia de caminho
        // (Task 7) corretamente rejeitaria a colocação da última cerca.
        Tabuleiro t = Tabuleiro.aPartirDe(posicoes, List.of(
            new Cerca(new Posicao(3, 4), Orientacao.HORIZONTAL),
            new Cerca(new Posicao(4, 3), Orientacao.VERTICAL),
            new Cerca(new Posicao(4, 4), Orientacao.VERTICAL)
        ));
        List<Posicao> validos = t.movimentosValidos(1);
        assertFalse(validos.contains(new Posicao(4, 3)), "lateral esquerda bloqueada");
        assertFalse(validos.contains(new Posicao(4, 5)), "lateral direita bloqueada");
        assertFalse(validos.contains(new Posicao(3, 4)), "pulo reto bloqueado");
        assertFalse(validos.contains(new Posicao(4, 4)), "casa do adversário não é destino");
    }

    @Test
    void vitoriaPorLadoParaOsQuatroJogadores() {
        Tabuleiro t = new Tabuleiro();
        t.moverPeao(1, new Posicao(0, 3)); assertTrue(t.chegouAoDestino(1));
        t.moverPeao(2, new Posicao(8, 5)); assertTrue(t.chegouAoDestino(2));
        t.moverPeao(3, new Posicao(0, 0)); assertTrue(t.chegouAoDestino(3));
        t.moverPeao(4, new Posicao(0, 8)); assertTrue(t.chegouAoDestino(4));
    }

    @Test
    void cercaSobrepostaDeveSerRejeitada() {
        Tabuleiro t = new Tabuleiro();
        Cerca c1 = new Cerca(new Posicao(4, 4), Orientacao.HORIZONTAL);
        assertTrue(t.podeColocarCerca(c1));
        t.colocarCerca(c1);
        assertFalse(t.podeColocarCerca(c1), "mesma cerca de novo");
        assertFalse(t.podeColocarCerca(new Cerca(new Posicao(4, 3), Orientacao.HORIZONTAL)),
                "sobrepõe o segmento H(4,4)");
    }

    @Test
    void cercaQueCruzaOutraDeveSerRejeitada() {
        Tabuleiro t = new Tabuleiro();
        Cerca h = new Cerca(new Posicao(4, 4), Orientacao.HORIZONTAL);
        assertTrue(t.podeColocarCerca(h));
        t.colocarCerca(h);
        assertFalse(t.podeColocarCerca(new Cerca(new Posicao(4, 4), Orientacao.VERTICAL)),
                "horizontal e vertical na mesma base cruzam");
    }

    @Test
    void cercaForaDoTabuleiroDeveSerRejeitada() {
        Tabuleiro t = new Tabuleiro();
        assertFalse(t.podeColocarCerca(new Cerca(new Posicao(8, 0), Orientacao.HORIZONTAL)));
        assertFalse(t.podeColocarCerca(new Cerca(new Posicao(0, 8), Orientacao.VERTICAL)));
    }

    @Test
    void cercaQueFechariaOPocketDeveSerRejeitada() {
        Tabuleiro t = new Tabuleiro();
        t.moverPeao(1, new Posicao(4, 4));
        Cerca cima = new Cerca(new Posicao(3, 4), Orientacao.HORIZONTAL);   // fecha saída superior
        Cerca esquerda = new Cerca(new Posicao(4, 3), Orientacao.VERTICAL); // fecha saída esquerda
        Cerca direita = new Cerca(new Posicao(4, 5), Orientacao.VERTICAL);  // fecha saída direita
        Cerca baixo = new Cerca(new Posicao(5, 4), Orientacao.HORIZONTAL);  // fecharia o pocket por baixo
        assertTrue(t.podeColocarCerca(cima)); t.colocarCerca(cima);
        assertTrue(t.podeColocarCerca(esquerda)); t.colocarCerca(esquerda);
        assertTrue(t.podeColocarCerca(direita)); t.colocarCerca(direita);
        assertTrue(t.temCaminho(1), "J1 ainda escapa por baixo");
        assertFalse(t.podeColocarCerca(baixo), "fecharia o pocket e isolaria o J1 (último caminho)");
    }

    @Test
    void cercaValidaPreservaCaminhoDeTodos() {
        Tabuleiro t = new Tabuleiro();
        Cerca c = new Cerca(new Posicao(4, 4), Orientacao.VERTICAL);
        assertTrue(t.podeColocarCerca(c));
        t.colocarCerca(c);
        for (int id = 1; id <= 4; id++) {
            assertTrue(t.temCaminho(id), "jogador " + id + " deve ter caminho após cerca válida");
        }
    }
}

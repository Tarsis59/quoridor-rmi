package client.ui;

import common.Cerca;
import common.EstadoJogo;
import common.Orientacao;
import common.Posicao;
import engine.Tabuleiro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TabuleiroPanelTest {
    private final List<Posicao> movidos = new ArrayList<>();
    private final List<Cerca> cercas = new ArrayList<>();
    private final List<String> avisos = new ArrayList<>();
    private TabuleiroPanel panel;

    @BeforeEach
    void montaPanel() {
        panel = new TabuleiroPanel();
        panel.setListener(new TabuleiroPanel.JogadaListener() {
            @Override public void onMover(Posicao d) { movidos.add(d); }
            @Override public void onColocarCerca(Cerca c) { cercas.add(c); }
            @Override public void onAviso(String m) { avisos.add(m); }
        });
    }

    private EstadoJogo estadoJ1() {
        Posicao[] pos = { new Posicao(8, 4), new Posicao(0, 4), new Posicao(4, 8), new Posicao(4, 0) };
        return new EstadoJogo(pos, List.of(), new int[0], new int[]{5, 5, 5, 5},
                1, EstadoJogo.Status.EM_ANDAMENTO, 0, new String[]{"Ana", "Bob", "Cid", "Duda"});
    }

    private void clique(int linha, int coluna) {
        Point c = Geometria.centroDaCasa(linha, coluna);
        MouseEvent ev = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                0, c.x, c.y, 1, false, MouseEvent.BUTTON1);
        panel.getMouseListeners()[0].mousePressed(ev);
    }

    @Test
    void cliqueEmCasaLegalMove() {
        panel.setMeuId(1);
        panel.setEstado(estadoJ1());
        clique(7, 4); // vizinho acima de (8,4)
        assertEquals(List.of(new Posicao(7, 4)), movidos);
        assertTrue(avisos.isEmpty());
    }

    @Test
    void cliqueEmCasaIlegalAvisa() {
        panel.setMeuId(1);
        panel.setEstado(estadoJ1());
        clique(0, 0);
        assertTrue(movidos.isEmpty());
        assertEquals(1, avisos.size());
    }

    @Test
    void cliqueEmModoCercaColoca() {
        Cerca alvo = new Cerca(new Posicao(2, 4), Orientacao.HORIZONTAL);
        assertTrue(new Tabuleiro().podeColocarCerca(alvo), "pré-condição: cerca deve ser válida");
        panel.setMeuId(1);
        panel.setEstado(estadoJ1());
        panel.setModo(TabuleiroPanel.Modo.CERCA_H);
        Point c = Geometria.centroDaCasa(2, 4);
        MouseEvent ev = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                0, c.x, Geometria.MARGEM + 3 * Geometria.CELULA, 1, false, MouseEvent.BUTTON1);
        panel.getMouseListeners()[0].mousePressed(ev);
        assertEquals(List.of(alvo), cercas);
    }

    @Test
    void cliqueForaDaVezNaoEnviaJogada() {
        panel.setMeuId(2); // o estado diz que é a vez do J1
        panel.setEstado(estadoJ1());
        clique(1, 4);
        assertTrue(movidos.isEmpty());
        assertEquals(1, avisos.size());
        assertTrue(avisos.get(0).contains("Aguarde sua vez"));
    }

    @Test
    void painelNaoInterativoIgnoraCliques() {
        panel.setMeuId(1);
        panel.setEstado(estadoJ1());
        panel.setInterativo(false);
        clique(7, 4);
        assertTrue(movidos.isEmpty());
        assertTrue(avisos.isEmpty());
    }

    @Test
    void jogadasPossiveisSoAparecemNaMinhaVez() {
        panel.setMeuId(1);
        panel.setEstado(estadoJ1());
        assertEquals(3, panel.getLegais().size(), "J1 em (8,4): cima, esquerda e direita");
        panel.setMeuId(2);
        assertTrue(panel.getLegais().isEmpty(), "não é a vez do J2");
    }

    @Test
    void cercaSemEstoqueAvisaSemEnviar() {
        Posicao[] pos = { new Posicao(8, 4), new Posicao(0, 4), new Posicao(4, 8), new Posicao(4, 0) };
        EstadoJogo semCercas = new EstadoJogo(pos, List.of(), new int[0], new int[]{0, 5, 5, 5},
                1, EstadoJogo.Status.EM_ANDAMENTO, 0, new String[]{"Ana", "Bob", "Cid", "Duda"});
        panel.setMeuId(1);
        panel.setEstado(semCercas);
        panel.setModo(TabuleiroPanel.Modo.CERCA_H);
        Point c = Geometria.centroDaCasa(2, 4);
        panel.getMouseListeners()[0].mousePressed(new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, c.x, Geometria.MARGEM + 3 * Geometria.CELULA, 1, false,
                MouseEvent.BUTTON1));
        assertTrue(cercas.isEmpty());
        assertTrue(avisos.get(0).contains("5 cercas"));
    }

    @Test
    void desenhaSemErroEmTodosOsEstados() {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                Geometria.LADO, Geometria.LADO, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        panel.setSize(Geometria.LADO, Geometria.LADO);
        panel.paint(img.getGraphics()); // sem estado ("Conectando")
        panel.setMeuId(1);
        panel.setEstado(estadoJ1());
        panel.paint(img.getGraphics());
        Posicao[] pos = { new Posicao(0, 4), new Posicao(0, 3), new Posicao(4, 8), new Posicao(4, 0) };
        panel.setEstado(new EstadoJogo(pos, List.of(new Cerca(new Posicao(3, 3), Orientacao.VERTICAL)), new int[]{2},
                new int[]{5, 4, 5, 5}, 1, EstadoJogo.Status.FINALIZADO, 1, new String[]{"Ana", "Bob", "Cid", "Duda"},
                new boolean[]{true, true, false, true}, 4));
        panel.paint(img.getGraphics());
        assertEquals(EstadoJogo.Status.FINALIZADO, panel.getEstado().getStatus());
    }

    @Test
    void mudarModoAvisaOuvinte() {
        List<TabuleiroPanel.Modo> modos = new ArrayList<>();
        panel.setAoMudarModo(modos::add);
        panel.setModo(TabuleiroPanel.Modo.CERCA_V);
        assertEquals(List.of(TabuleiroPanel.Modo.CERCA_V), modos);
    }
}

package client.ui;

import common.Cerca;
import common.Orientacao;
import common.Posicao;
import org.junit.jupiter.api.Test;

import java.awt.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GeometriaTest {

    @Test
    void casaEmCentroDeCadaCelula() {
        assertEquals(new Posicao(0, 0), Geometria.casaEm(Geometria.MARGEM + 26, Geometria.MARGEM + 26));
        assertEquals(new Posicao(4, 4), Geometria.casaEm(
                Geometria.MARGEM + 4 * Geometria.CELULA + 26,
                Geometria.MARGEM + 4 * Geometria.CELULA + 26));
        assertEquals(new Posicao(8, 8), Geometria.casaEm(
                Geometria.LADO - Geometria.MARGEM - 1,
                Geometria.LADO - Geometria.MARGEM - 1));
    }

    @Test
    void casaEmForaDoTabuleiroRetornaNull() {
        assertNull(Geometria.casaEm(0, Geometria.MARGEM + 26));
        assertNull(Geometria.casaEm(Geometria.LADO - 1, Geometria.LADO - 1));
    }

    @Test
    void centroDaCasa() {
        Point c = Geometria.centroDaCasa(2, 3);
        assertEquals(Geometria.MARGEM + 3 * Geometria.CELULA + 26, c.x);
        assertEquals(Geometria.MARGEM + 2 * Geometria.CELULA + 26, c.y);
    }

    @Test
    void cercaHorizontalNaLinhaEntre2e3() {
        int y = Geometria.MARGEM + 3 * Geometria.CELULA; // linha de grade entre linhas 2 e 3
        Cerca c = Geometria.cercaHorizontalEm(Geometria.MARGEM + 4 * Geometria.CELULA + 26, y);
        assertEquals(new Cerca(new Posicao(2, 4), Orientacao.HORIZONTAL), c);
    }

    @Test
    void cercaVerticalNaLinhaEntre5e6() {
        int x = Geometria.MARGEM + 6 * Geometria.CELULA; // linha de grade entre colunas 5 e 6
        Cerca c = Geometria.cercaVerticalEm(x, Geometria.MARGEM + 4 * Geometria.CELULA + 26);
        assertEquals(new Cerca(new Posicao(4, 5), Orientacao.VERTICAL), c);
    }

    @Test
    void cercaForaDoLimiteRetornaNull() {
        assertNull(Geometria.cercaHorizontalEm(Geometria.MARGEM + 26, Geometria.MARGEM));
        assertNull(Geometria.cercaVerticalEm(Geometria.MARGEM, Geometria.MARGEM + 26));
    }
}

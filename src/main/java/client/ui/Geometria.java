package client.ui;

import common.Cerca;
import common.Orientacao;
import common.Posicao;

import java.awt.Point;
import java.awt.Rectangle;

/** Geometria pura (pixel <-> casa/aresta). Não toca em componentes Swing, permitindo testes unitários. */
public final class Geometria {
    public static final int CELULA = EstiloUI.CELULA;
    public static final int MARGEM = EstiloUI.MARGEM;
    public static final int LADO = EstiloUI.LADO;
    public static final int TAM = 9;

    private Geometria() {}

    public static Rectangle rectDaCasa(int linha, int coluna) {
        return new Rectangle(MARGEM + coluna * CELULA, MARGEM + linha * CELULA, CELULA, CELULA);
    }

    public static Point centroDaCasa(int linha, int coluna) {
        Rectangle r = rectDaCasa(linha, coluna);
        return new Point(r.x + r.width / 2, r.y + r.height / 2);
    }

    public static Posicao casaEm(int x, int y) {
        int col = Math.floorDiv(x - MARGEM, CELULA);
        int lin = Math.floorDiv(y - MARGEM, CELULA);
        if (col < 0 || col >= TAM || lin < 0 || lin >= TAM) return null;
        return new Posicao(lin, col);
    }

    /** Cerca horizontal de base (r,c): linha entre r e r+1 cobrindo as colunas c..c+1. */
    public static Cerca cercaHorizontalEm(int x, int y) {
        int i = Math.round((y - MARGEM) / (float) CELULA);
        if (i < 1 || i > 8) return null;
        int r = i - 1;
        int c = Math.max(0, Math.min(7, (x - MARGEM) / CELULA));
        return new Cerca(new Posicao(r, c), Orientacao.HORIZONTAL);
    }

    /** Cerca vertical de base (r,c): linha entre c e c+1 cobrindo as linhas r..r+1. */
    public static Cerca cercaVerticalEm(int x, int y) {
        int i = Math.round((x - MARGEM) / (float) CELULA);
        if (i < 1 || i > 8) return null;
        int c = i - 1;
        int r = Math.max(0, Math.min(7, (y - MARGEM) / CELULA));
        return new Cerca(new Posicao(r, c), Orientacao.VERTICAL);
    }
}

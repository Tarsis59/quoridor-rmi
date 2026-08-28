package client.ui;

import common.Cerca;
import common.EstadoJogo;
import common.Orientacao;
import common.Posicao;
import engine.Tabuleiro;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class TabuleiroPanel extends JPanel {
    public enum Modo { MOVER, CERCA_H, CERCA_V }

    public interface JogadaListener {
        void onMover(Posicao destino);
        void onColocarCerca(Cerca cerca);
        void onAviso(String mensagem);
    }

    private EstadoJogo estado;
    private Tabuleiro modelo;
    private int meuId;
    private Modo modo = Modo.MOVER;
    private List<Posicao> legais = List.of();
    private Cerca preview;
    private boolean previewValida;
    private JogadaListener listener;

    public TabuleiroPanel() {
        setPreferredSize(new Dimension(Geometria.LADO, Geometria.LADO));
        setOpaque(false);
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { atualizarPreview(e.getX(), e.getY()); }
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) { alternarOrientacao(); return; }
                clique(e.getX(), e.getY());
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    public void setListener(JogadaListener l) { this.listener = l; }
    public void setMeuId(int id) { this.meuId = id; recomputarLegais(); }
    public void setModo(Modo m) { this.modo = m; this.preview = null; repaint(); }
    public Modo getModo() { return modo; }

    public void setEstado(EstadoJogo e) {
        this.estado = e;
        this.modelo = Tabuleiro.aPartirDe(e.getPosicoes(), e.getCercas());
        recomputarLegais();
    }

    private void recomputarLegais() {
        this.legais = (estado != null && modelo != null && meuId > 0)
                ? modelo.movimentosValidos(meuId) : List.of();
        repaint();
    }

    private void atualizarPreview(int x, int y) {
        if (estado == null || modelo == null) { preview = null; repaint(); return; }
        preview = cercaCandidata(x, y);
        previewValida = (preview == null) || modelo.podeColocarCerca(preview);
        repaint();
    }

    private Cerca cercaCandidata(int x, int y) {
        if (modo == Modo.CERCA_H) return Geometria.cercaHorizontalEm(x, y);
        if (modo == Modo.CERCA_V) return Geometria.cercaVerticalEm(x, y);
        return null;
    }

    private void clique(int x, int y) {
        if (estado == null || modelo == null || listener == null) return;
        if (modo == Modo.MOVER) {
            Posicao casa = Geometria.casaEm(x, y);
            if (casa == null) return;
            if (legais.contains(casa)) listener.onMover(casa);
            else listener.onAviso("Movimento inválido para (" + casa.linha() + "," + casa.coluna() + ").");
        } else {
            Cerca c = cercaCandidata(x, y);
            if (c == null) return;
            if (modelo.podeColocarCerca(c)) {
                listener.onColocarCerca(c);
                setModo(Modo.MOVER);
            } else {
                listener.onAviso("Cerca inválida: sobreposição ou bloqueia um caminho.");
            }
        }
    }

    private void alternarOrientacao() {
        if (modo == Modo.CERCA_H) setModo(Modo.CERCA_V);
        else if (modo == Modo.CERCA_V) setModo(Modo.CERCA_H);
    }

    // Acesso para inspeção.
    Cerca getPreview() { return preview; }
    boolean isPreviewValida() { return previewValida; }
    EstadoJogo getEstado() { return estado; }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(EstiloUI.FUNDO_TABULEIRO);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

        desenharCasas(g);
        desenharCercas(g);
        desenharPreview(g);
        desenharPeoes(g);
        desenharCoordenadas(g);

        g.dispose();
    }

    private void desenharCasas(Graphics2D g) {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                var rec = Geometria.rectDaCasa(r, c);
                Color cor = corMeta(r, c);
                if (casaDestaque(r, c)) cor = EstiloUI.CASA_DESTAQUE;
                g.setColor(cor);
                g.fillRoundRect(rec.x + 1, rec.y + 1, rec.width - 2, rec.height - 2, 6, 6);
                g.setColor(EstiloUI.BORDA_CASA);
                g.drawRoundRect(rec.x + 1, rec.y + 1, rec.width - 2, rec.height - 2, 6, 6);
            }
        }
    }

    private Color corMeta(int r, int c) {
        if (r == 0) return new Color(0xFFF1F1);
        if (r == 8) return new Color(0xF1F6FF);
        if (c == 0) return new Color(0xF2FBF3);
        return new Color(0xFFF9EC);
    }

    private boolean casaDestaque(int r, int c) {
        return legais != null && legais.stream().anyMatch(p -> p.linha() == r && p.coluna() == c);
    }

    private void desenharCercas(Graphics2D g) {
        if (estado == null) return;
        g.setColor(EstiloUI.CERCA);
        for (Cerca c : estado.getCercas()) desenharParede(g, c);
    }

    private void desenharPreview(Graphics2D g) {
        if (preview == null) return;
        g.setComposite(java.awt.AlphaComposite.getInstance(
                java.awt.AlphaComposite.SRC_OVER, 0.45f));
        g.setColor(previewValida ? EstiloUI.VERDE_OK : EstiloUI.VERMELHO_ERRADO);
        desenharParede(g, preview);
        g.setComposite(java.awt.AlphaComposite.SrcOver);
    }

    private void desenharPeoes(Graphics2D g) {
        if (estado == null) return;
        for (int id = 1; id <= 4; id++) {
            Posicao p = estado.getPosicao(id);
            var c = Geometria.centroDaCasa(p.linha(), p.coluna());
            int raio = Geometria.CELULA / 3;
            g.setColor(EstiloUI.COR_PEAO[id - 1]);
            g.fillOval(c.x - raio, c.y - raio, raio * 2, raio * 2);
            if (estado.getJogadorDaVez() == id) {
                g.setColor(EstiloUI.BADGE_VEZ_BORDA);
                g.setStroke(new java.awt.BasicStroke(3f));
                g.drawOval(c.x - raio - 4, c.y - raio - 4, raio * 2 + 8, raio * 2 + 8);
                g.setStroke(new java.awt.BasicStroke(1f));
            }
            g.setColor(Color.WHITE);
            g.setFont(EstiloUI.FONTE_PEAO);
            String numero = String.valueOf(id);
            var fm = g.getFontMetrics();
            int tx = c.x - fm.stringWidth(numero) / 2;
            int ty = c.y + fm.getAscent() / 2 - 2;
            g.drawString(numero, tx, ty);
        }
    }

    private void desenharParede(Graphics2D g, Cerca c) {
        int r = c.base().linha();
        int col = c.base().coluna();
        int t = EstiloUI.ESPESSURA_CERCA;
        if (c.orientacao() == Orientacao.HORIZONTAL) {
            int y = Geometria.MARGEM + (r + 1) * Geometria.CELULA - t / 2;
            int x = Geometria.MARGEM + col * Geometria.CELULA;
            g.fillRoundRect(x, y, 2 * Geometria.CELULA, t, 4, 4);
        } else {
            int x = Geometria.MARGEM + (col + 1) * Geometria.CELULA - t / 2;
            int y = Geometria.MARGEM + r * Geometria.CELULA;
            g.fillRoundRect(x, y, t, 2 * Geometria.CELULA, 4, 4);
        }
    }

    private void desenharCoordenadas(Graphics2D g) {
        g.setFont(EstiloUI.FONTE_ROTULO);
        g.setColor(EstiloUI.TEXTO_SECUNDARIO);
        for (int i = 0; i < 9; i++) {
            var col = Geometria.rectDaCasa(0, i);
            g.drawString(String.valueOf(i), col.x + col.width / 2 - 3, Geometria.MARGEM - 4);
            var lin = Geometria.rectDaCasa(i, 0);
            g.drawString(String.valueOf(i), Geometria.MARGEM - 14, lin.y + lin.height / 2 + 4);
        }
    }
}

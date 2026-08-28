package client.ui;

import common.Cerca;
import common.EstadoJogo;
import common.Orientacao;
import common.Posicao;
import engine.Tabuleiro;

import javax.swing.JPanel;
import java.awt.Dimension;
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
}

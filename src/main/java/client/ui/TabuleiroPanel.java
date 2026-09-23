package client.ui;

import common.Cerca;
import common.EstadoJogo;
import common.Orientacao;
import common.Posicao;
import engine.Tabuleiro;

import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * Tabuleiro 9x9 desenhado à mão (Java2D): casas com sulcos onde as cercas encaixam, faixas
 * coloridas marcando a meta de cada jogador, pontos nas jogadas possíveis, destaque da
 * última jogada, preview de cerca e avisos sobrepostos (espera e vitória).
 */
public class TabuleiroPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    public enum Modo { MOVER, CERCA_H, CERCA_V }

    public interface JogadaListener {
        void onMover(Posicao destino);
        void onColocarCerca(Cerca cerca);
        void onAviso(String mensagem);
    }

    private transient EstadoJogo estado;
    private transient EstadoJogo anterior;
    private transient Tabuleiro modelo;
    private int meuId;
    private Modo modo = Modo.MOVER;
    private transient List<Posicao> legais = List.of();
    private transient Cerca preview;
    private boolean previewValida;
    private transient Posicao hover;
    private transient JogadaListener listener;
    private transient Consumer<Modo> aoMudarModo = m -> { };
    private boolean interativo = true;

    public TabuleiroPanel() {
        setPreferredSize(new Dimension(Geometria.LADO, Geometria.LADO));
        setMinimumSize(getPreferredSize());
        setOpaque(false);
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { atualizarPonteiro(e.getX(), e.getY()); }
            @Override public void mouseExited(MouseEvent e) { preview = null; hover = null; repaint(); }
            // Age no "apertar": um clique com o mouse tremendo 1 px não se perde
            // (mouseClicked só dispara se apertar e soltar exatamente no mesmo pixel).
            @Override public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) { alternarOrientacao(); return; }
                if (e.getButton() == MouseEvent.BUTTON1) clique(e.getX(), e.getY());
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    public void setListener(JogadaListener l) { this.listener = l; }
    public void setMeuId(int id) { this.meuId = id; recomputarLegais(); }
    public void setModo(Modo m) {
        this.modo = m;
        this.preview = null;
        aoMudarModo.accept(m);
        repaint();
    }
    public void setAoMudarModo(Consumer<Modo> c) { this.aoMudarModo = c == null ? m -> { } : c; }
    /** false no modo automático: o tabuleiro só exibe, não aceita cliques. */
    public void setInterativo(boolean interativo) { this.interativo = interativo; recomputarLegais(); }
    public Modo getModo() { return modo; }

    public void alternarOrientacao() {
        if (modo == Modo.CERCA_H) setModo(Modo.CERCA_V);
        else if (modo == Modo.CERCA_V) setModo(Modo.CERCA_H);
    }

    public void setEstado(EstadoJogo e) {
        if (e == null) return;
        // O mesmo estado pode ser reaplicado (ex.: ao descobrir o próprio id): não apaga a última jogada.
        if (e != this.estado) this.anterior = this.estado;
        this.estado = e;
        this.modelo = Tabuleiro.aPartirDe(e.getPosicoes(), e.getCercas());
        this.preview = null;
        recomputarLegais();
    }

    /** É a minha vez numa partida em andamento? */
    private boolean minhaVez() {
        return estado != null && meuId > 0
                && estado.getStatus() == EstadoJogo.Status.EM_ANDAMENTO
                && estado.getJogadorDaVez() == meuId;
    }

    /** Jogadas possíveis: só na vez deste jogador e só se o tabuleiro for interativo. */
    private void recomputarLegais() {
        this.legais = (interativo && modelo != null && minhaVez())
                ? modelo.movimentosValidos(meuId) : List.of();
        setCursor(Cursor.getPredefinedCursor(minhaVez() && interativo ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    private void atualizarPonteiro(int x, int y) {
        hover = Geometria.casaEm(x, y);
        if (!interativo || modelo == null || !minhaVez()) {
            preview = null;
        } else {
            preview = cercaCandidata(x, y);
            previewValida = preview != null && modelo.podeColocarCerca(preview);
        }
        repaint();
    }

    private Cerca cercaCandidata(int x, int y) {
        if (modo == Modo.CERCA_H) return Geometria.cercaHorizontalEm(x, y);
        if (modo == Modo.CERCA_V) return Geometria.cercaVerticalEm(x, y);
        return null;
    }

    private void clique(int x, int y) {
        if (!interativo || estado == null || modelo == null || listener == null) return;
        if (estado.getStatus() != EstadoJogo.Status.EM_ANDAMENTO) {
            listener.onAviso(estado.getStatus() == EstadoJogo.Status.AGUARDANDO
                    ? "A partida ainda não começou: aguardando os 4 jogadores."
                    : "A partida já terminou.");
            return;
        }
        if (!minhaVez()) {
            listener.onAviso("Aguarde sua vez: agora joga " + estado.getNome(estado.getJogadorDaVez())
                    + " (Jogador " + estado.getJogadorDaVez() + ").");
            return;
        }
        if (modo == Modo.MOVER) {
            Posicao casa = Geometria.casaEm(x, y);
            if (casa == null) return;
            if (legais.contains(casa)) {
                listener.onMover(casa);
            } else {
                listener.onAviso("Movimento inválido para (" + casa.linha() + "," + casa.coluna()
                        + "). Clique numa casa marcada com um ponto.");
            }
        } else {
            Cerca c = cercaCandidata(x, y);
            if (c == null) return;
            if (estado.getCercasRestantes(meuId) <= 0) {
                listener.onAviso("Você já usou suas 5 cercas.");
            } else if (modelo.podeColocarCerca(c)) {
                listener.onColocarCerca(c);
                setModo(Modo.MOVER);
            } else {
                listener.onAviso("Cerca inválida: sobrepõe/cruza outra ou fecharia todo o caminho de alguém.");
            }
        }
    }

    // Acesso para inspeção/testes.
    Cerca getPreview() { return preview; }
    boolean isPreviewValida() { return previewValida; }
    EstadoJogo getEstado() { return estado; }
    List<Posicao> getLegais() { return legais; }

    // ------------------------------------------------------------------ desenho

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        EstiloUI.suavizar(g);
        desenharMoldura(g);
        desenharFaixasDeMeta(g);
        desenharCasas(g);
        desenharUltimoMovimento(g);
        desenharJogadasPossiveis(g);
        desenharCercas(g);
        desenharPreview(g);
        desenharPeoes(g);
        desenharCoordenadas(g);
        desenharAviso(g);
        g.dispose();
    }

    private void desenharMoldura(Graphics2D g) {
        g.setColor(EstiloUI.MOLDURA);
        g.fillRoundRect(0, 0, Geometria.LADO, Geometria.LADO, 22, 22);
        g.setColor(EstiloUI.BORDA);
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(0, 0, Geometria.LADO - 1, Geometria.LADO - 1, 22, 22);
    }

    /** Faixa na borda de chegada de cada jogador, na cor dele (J1 topo, J2 base, J3 esquerda, J4 direita). */
    private void desenharFaixasDeMeta(Graphics2D g) {
        int m = Geometria.MARGEM;
        int tam = 9 * Geometria.CELULA;
        int esp = 4;
        int dist = 8;
        g.setColor(corMeta(1));
        g.fillRoundRect(m + 4, m - dist - esp, tam - 8, esp, esp, esp);
        g.setColor(corMeta(2));
        g.fillRoundRect(m + 4, m + tam + dist, tam - 8, esp, esp, esp);
        g.setColor(corMeta(3));
        g.fillRoundRect(m - dist - esp, m + 4, esp, tam - 8, esp, esp);
        g.setColor(corMeta(4));
        g.fillRoundRect(m + tam + dist, m + 4, esp, tam - 8, esp, esp);
    }

    private Color corMeta(int id) {
        Color c = EstiloUI.COR_PEAO[id - 1];
        boolean ativo = estado == null || estado.isAtivo(id);
        return ativo ? c : EstiloUI.comAlfa(c, 70);
    }

    private void desenharCasas(Graphics2D g) {
        int s = EstiloUI.SULCO;
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                Rectangle rec = Geometria.rectDaCasa(r, c);
                int x = rec.x + s;
                int y = rec.y + s;
                int w = rec.width - 2 * s;
                int h = rec.height - 2 * s;
                boolean emHover = hover != null && hover.linha() == r && hover.coluna() == c
                        && modo == Modo.MOVER && legais.contains(hover);
                Color topo = emHover ? EstiloUI.CASA_HOVER.brighter() : EstiloUI.CASA_BRILHO;
                Color base = emHover ? EstiloUI.CASA_HOVER : EstiloUI.CASA;
                g.setPaint(new GradientPaint(x, y, topo, x, y + h, base));
                g.fillRoundRect(x, y, w, h, 10, 10);
                g.setColor(EstiloUI.comAlfa(Color.WHITE, 14));
                g.drawRoundRect(x, y, w - 1, h - 1, 10, 10);
            }
        }
    }

    /** Marca de onde o peão saiu na última jogada (contorno tracejado na cor dele). */
    private void desenharUltimoMovimento(Graphics2D g) {
        if (estado == null || anterior == null) return;
        for (int id = 1; id <= 4; id++) {
            Posicao de = anterior.getPosicao(id);
            if (de.equals(estado.getPosicao(id))) continue;
            Rectangle rec = Geometria.rectDaCasa(de.linha(), de.coluna());
            int s = EstiloUI.SULCO + 5;
            g.setColor(EstiloUI.comAlfa(EstiloUI.COR_PEAO[id - 1], 150));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{5f, 5f}, 0f));
            g.drawRoundRect(rec.x + s, rec.y + s, rec.width - 2 * s, rec.height - 2 * s, 10, 10);
            g.setStroke(new BasicStroke(1f));
        }
    }

    /** Pontos (na cor do jogador) nas casas para onde ele pode ir. */
    private void desenharJogadasPossiveis(Graphics2D g) {
        if (legais == null || legais.isEmpty() || modo != Modo.MOVER) return;
        Color cor = meuId >= 1 && meuId <= 4 ? EstiloUI.COR_PEAO[meuId - 1] : EstiloUI.VERDE_OK;
        for (Posicao p : legais) {
            Point c = Geometria.centroDaCasa(p.linha(), p.coluna());
            boolean emHover = p.equals(hover);
            int r = emHover ? 11 : 7;
            g.setColor(EstiloUI.comAlfa(cor, emHover ? 90 : 45));
            g.fillOval(c.x - r - 5, c.y - r - 5, 2 * (r + 5), 2 * (r + 5));
            g.setColor(EstiloUI.comAlfa(cor, 220));
            g.fillOval(c.x - r, c.y - r, 2 * r, 2 * r);
        }
    }

    private void desenharCercas(Graphics2D g) {
        if (estado == null) return;
        List<Cerca> cercas = estado.getCercas();
        int novas = anterior == null ? 0 : Math.max(0, cercas.size() - anterior.getCercas().size());
        for (int i = 0; i < cercas.size(); i++) {
            int dono = estado.getDonoCerca(i);
            Color cor = dono >= 1 && dono <= 4 ? EstiloUI.COR_PEAO[dono - 1] : EstiloUI.CERCA;
            boolean recente = i >= cercas.size() - novas;
            desenharParede(g, cercas.get(i), cor, recente, 1f);
        }
    }

    private void desenharPreview(Graphics2D g) {
        if (preview == null) return;
        desenharParede(g, preview, previewValida ? EstiloUI.VERDE_OK : EstiloUI.VERMELHO_ERRADO, false, 0.6f);
    }

    private void desenharParede(Graphics2D g, Cerca c, Color cor, boolean brilho, float opacidade) {
        Rectangle r = retanguloDaParede(c);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacidade));
        if (brilho) {
            g2.setColor(EstiloUI.comAlfa(cor, 70));
            g2.fillRoundRect(r.x - 4, r.y - 4, r.width + 8, r.height + 8, 14, 14);
        }
        g2.setColor(EstiloUI.comAlfa(Color.BLACK, 110));
        g2.fillRoundRect(r.x + 1, r.y + 2, r.width, r.height, 8, 8);
        boolean horizontal = c.orientacao() == Orientacao.HORIZONTAL;
        g2.setPaint(horizontal
                ? new GradientPaint(r.x, r.y, cor.brighter(), r.x, r.y + r.height, cor.darker())
                : new GradientPaint(r.x, r.y, cor.brighter(), r.x + r.width, r.y, cor.darker()));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g2.dispose();
    }

    static Rectangle retanguloDaParede(Cerca c) {
        int r = c.base().linha();
        int col = c.base().coluna();
        int t = EstiloUI.ESPESSURA_CERCA;
        int s = EstiloUI.SULCO;
        if (c.orientacao() == Orientacao.HORIZONTAL) {
            int y = Geometria.MARGEM + (r + 1) * Geometria.CELULA - t / 2;
            int x = Geometria.MARGEM + col * Geometria.CELULA + s;
            return new Rectangle(x, y, 2 * Geometria.CELULA - 2 * s, t);
        }
        int x = Geometria.MARGEM + (col + 1) * Geometria.CELULA - t / 2;
        int y = Geometria.MARGEM + r * Geometria.CELULA + s;
        return new Rectangle(x, y, t, 2 * Geometria.CELULA - 2 * s);
    }

    private void desenharPeoes(Graphics2D g) {
        if (estado == null) return;
        for (int id = 1; id <= 4; id++) {
            Posicao p = estado.getPosicao(id);
            Point c = Geometria.centroDaCasa(p.linha(), p.coluna());
            int raio = Geometria.CELULA / 3;
            Color cor = EstiloUI.COR_PEAO[id - 1];
            boolean ativo = estado.isAtivo(id);
            boolean daVez = estado.getStatus() == EstadoJogo.Status.EM_ANDAMENTO && estado.getJogadorDaVez() == id;
            Graphics2D g2 = (Graphics2D) g.create();
            if (!ativo) g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            if (daVez) {
                g2.setColor(EstiloUI.comAlfa(EstiloUI.DESTAQUE, 60));
                g2.fillOval(c.x - raio - 9, c.y - raio - 9, 2 * raio + 18, 2 * raio + 18);
                g2.setColor(EstiloUI.DESTAQUE);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawOval(c.x - raio - 5, c.y - raio - 5, 2 * raio + 10, 2 * raio + 10);
            }
            // sombra
            g2.setColor(EstiloUI.comAlfa(Color.BLACK, 120));
            g2.fillOval(c.x - raio + 2, c.y - raio + 4, 2 * raio, 2 * raio);
            // corpo com volume (luz vindo de cima-esquerda)
            g2.setPaint(new RadialGradientPaint(new Point(c.x - raio / 3, c.y - raio / 3), raio * 1.4f,
                    new float[]{0f, 0.55f, 1f},
                    new Color[]{EstiloUI.misturar(cor, Color.WHITE, 0.45), cor, cor.darker()}));
            g2.fillOval(c.x - raio, c.y - raio, 2 * raio, 2 * raio);
            g2.setColor(EstiloUI.comAlfa(Color.WHITE, 90));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawOval(c.x - raio, c.y - raio, 2 * raio, 2 * raio);
            if (id == meuId) {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(c.x - raio + 3, c.y - raio + 3, 2 * raio - 6, 2 * raio - 6);
            }
            g2.setColor(Color.WHITE);
            g2.setFont(EstiloUI.FONTE_PEAO);
            String numero = String.valueOf(id);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(numero, c.x - fm.stringWidth(numero) / 2, c.y + (fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    private void desenharCoordenadas(Graphics2D g) {
        g.setFont(EstiloUI.FONTE_ROTULO);
        g.setColor(EstiloUI.TEXTO_SECUNDARIO);
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < 9; i++) {
            String t = String.valueOf(i);
            Rectangle col = Geometria.rectDaCasa(0, i);
            g.drawString(t, col.x + (col.width - fm.stringWidth(t)) / 2, 13);
            Rectangle lin = Geometria.rectDaCasa(i, 0);
            g.drawString(t, 6, lin.y + (lin.height + fm.getAscent()) / 2 - 2);
        }
    }

    /** Aviso centralizado sobre o tabuleiro: sala de espera ou resultado final. */
    private void desenharAviso(Graphics2D g) {
        if (estado == null) {
            desenharCaixaAviso(g, "Conectando...", "Aguardando o servidor", EstiloUI.TEXTO_SECUNDARIO);
            return;
        }
        if (estado.getStatus() == EstadoJogo.Status.AGUARDANDO) {
            desenharCaixaAviso(g, "Aguardando jogadores", estado.getRegistrados() + " de 4 conectados",
                    EstiloUI.DESTAQUE);
        } else if (estado.getStatus() == EstadoJogo.Status.FINALIZADO) {
            int v = estado.getVencedor();
            if (v == 0) {
                desenharCaixaAviso(g, "Partida encerrada", "Todos saíram", EstiloUI.TEXTO_SECUNDARIO);
            } else {
                String titulo = v == meuId ? "Você venceu!" : estado.getNome(v) + " venceu!";
                desenharCaixaAviso(g, titulo, "Jogador " + v + " · " + EstiloUI.NOME_COR[v - 1],
                        EstiloUI.COR_PEAO[v - 1]);
            }
        }
    }

    private void desenharCaixaAviso(Graphics2D g, String titulo, String subtitulo, Color cor) {
        g.setColor(EstiloUI.comAlfa(EstiloUI.FUNDO_JANELA, 150));
        g.fillRoundRect(0, 0, Geometria.LADO, Geometria.LADO, 22, 22);
        g.setFont(EstiloUI.FONTE_AVISO);
        FontMetrics ft = g.getFontMetrics();
        g.setFont(EstiloUI.FONTE_NORMAL);
        FontMetrics fs = g.getFontMetrics();
        int largura = Math.max(ft.stringWidth(titulo), fs.stringWidth(subtitulo)) + 64;
        int altura = 96;
        int x = (Geometria.LADO - largura) / 2;
        int y = (Geometria.LADO - altura) / 2;
        g.setColor(EstiloUI.comAlfa(Color.BLACK, 120));
        g.fillRoundRect(x + 3, y + 5, largura, altura, 20, 20);
        g.setColor(EstiloUI.CARTAO);
        g.fillRoundRect(x, y, largura, altura, 20, 20);
        g.setColor(cor);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, largura - 1, altura - 1, 20, 20);
        g.setFont(EstiloUI.FONTE_AVISO);
        g.drawString(titulo, x + (largura - ft.stringWidth(titulo)) / 2, y + 44);
        g.setColor(EstiloUI.TEXTO_SECUNDARIO);
        g.setFont(EstiloUI.FONTE_NORMAL);
        g.drawString(subtitulo, x + (largura - fs.stringWidth(subtitulo)) / 2, y + 72);
    }
}

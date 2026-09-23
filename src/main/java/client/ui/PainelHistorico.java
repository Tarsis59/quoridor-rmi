package client.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Lista das últimas jogadas da partida (mais recente em cima), com a cor de quem jogou. */
public class PainelHistorico extends JPanel {
    private static final long serialVersionUID = 1L;
    static final int MAX_ITENS = 9;
    private static final int ALTURA_ITEM = 22;

    private final transient Deque<Historico.Evento> eventos = new ArrayDeque<>();
    private final Lista lista = new Lista();

    public PainelHistorico() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        add(PainelLateral.titulo("HISTÓRICO"));
        add(Box.createVerticalStrut(4));
        lista.setAlignmentX(LEFT_ALIGNMENT);
        add(lista);
    }

    public void adicionar(List<Historico.Evento> novos) {
        for (Historico.Evento ev : novos) {
            eventos.addFirst(ev);
            while (eventos.size() > MAX_ITENS) eventos.removeLast();
        }
        lista.repaint();
    }

    List<Historico.Evento> itens() { return new ArrayList<>(eventos); }

    private final class Lista extends JPanel {
        private static final long serialVersionUID = 1L;

        Lista() {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            Dimension d = new Dimension(290, MAX_ITENS * ALTURA_ITEM + 16);
            setPreferredSize(d);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, d.height));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            EstiloUI.suavizar(g);
            g.setColor(EstiloUI.CARTAO);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g.setColor(EstiloUI.BORDA);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g.setFont(EstiloUI.FONTE_PEQUENA);
            FontMetrics fm = g.getFontMetrics();
            if (eventos.isEmpty()) {
                g.setColor(EstiloUI.TEXTO_SECUNDARIO);
                g.drawString("As jogadas aparecem aqui.", 12, 8 + (ALTURA_ITEM + fm.getAscent()) / 2);
            }
            int y = 8;
            int i = 0;
            for (Historico.Evento ev : eventos) {
                int id = ev.idJogador();
                int cy = y + ALTURA_ITEM / 2;
                g.setColor(id >= 1 && id <= 4 ? EstiloUI.COR_PEAO[id - 1] : EstiloUI.TEXTO_SECUNDARIO);
                g.fillOval(12, cy - 4, 8, 8);
                // mais antigas vão esmaecendo
                int alfa = Math.max(110, 255 - i * 18);
                g.setColor(EstiloUI.comAlfa(i == 0 ? EstiloUI.TEXTO_PRINCIPAL : EstiloUI.TEXTO_SECUNDARIO, alfa));
                g.drawString(cortar(ev.texto(), fm, getWidth() - 40), 28, cy + (fm.getAscent() - fm.getDescent()) / 2);
                y += ALTURA_ITEM;
                i++;
            }
            g.dispose();
        }

        private String cortar(String s, FontMetrics fm, int max) {
            if (fm.stringWidth(s) <= max) return s;
            String r = s;
            while (r.length() > 1 && fm.stringWidth(r + "…") > max) r = r.substring(0, r.length() - 1);
            return r + "…";
        }
    }
}

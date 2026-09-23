package client.ui;

import common.EstadoJogo;
import common.Posicao;
import engine.Tabuleiro;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/** Coluna com um cartão por jogador: cor, nome, passos até a meta, 5 cercas e selo de estado. */
public class PainelJogadores extends JPanel {
    private static final long serialVersionUID = 1L;
    private final List<Cartao> cartoes = new ArrayList<>();

    public PainelJogadores() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        add(PainelLateral.titulo("JOGADORES"));
        add(Box.createVerticalStrut(6));
        for (int id = 1; id <= 4; id++) {
            Cartao c = new Cartao(id);
            cartoes.add(c);
            add(c);
            if (id < 4) add(Box.createVerticalStrut(8));
        }
    }

    public void atualizar(EstadoJogo e, int meuId) {
        Tabuleiro t = Tabuleiro.aPartirDe(e.getPosicoes(), e.getCercas());
        for (int id = 1; id <= 4; id++) {
            cartoes.get(id - 1).atualizar(e, meuId, t.distanciaMinimaAteAlvo(id));
        }
        repaint();
    }

    // Acesso para testes.
    JLabel badge(int idJogador) { return cartoes.get(idJogador - 1).selo; }
    JPanel linha(int idJogador) { return cartoes.get(idJogador - 1); }
    JLabel nome(int idJogador) { return cartoes.get(idJogador - 1).nome; }
    JLabel meta(int idJogador) { return cartoes.get(idJogador - 1).detalhe; }
    int cercasDesenhadas(int idJogador) { return cartoes.get(idJogador - 1).cercas; }

    /** Cartão de um jogador (fundo, avatar e cercas desenhados à mão; textos em JLabel). */
    static final class Cartao extends JPanel {
        private static final long serialVersionUID = 1L;
        private static final int AVATAR = 34;
        private final int id;
        final JLabel nome = new JLabel();
        final JLabel detalhe = new JLabel();
        final JLabel selo = new JLabel();
        int cercas = 5;
        private boolean daVez;
        private boolean voce;
        private boolean ativo = true;

        Cartao(int id) {
            this.id = id;
            setLayout(new BorderLayout(10, 0));
            setOpaque(false);
            setBackground(EstiloUI.CARTAO);
            setBorder(BorderFactory.createEmptyBorder(10, 12 + AVATAR + 10, 10, 12));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
            setPreferredSize(new Dimension(290, 78));
            setAlignmentX(LEFT_ALIGNMENT);

            nome.setText("Jogador " + id);
            nome.setFont(EstiloUI.FONTE_NEGRITO);
            nome.setForeground(EstiloUI.TEXTO_PRINCIPAL);
            detalhe.setFont(EstiloUI.FONTE_PEQUENA);
            detalhe.setForeground(EstiloUI.TEXTO_SECUNDARIO);
            detalhe.setText(EstiloUI.META[id - 1]);
            // Folga à direita: o texto nunca perde a última letra por diferença de métrica de fonte.
            detalhe.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
            nome.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
            JPanel textos = new JPanel();
            textos.setOpaque(false);
            textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
            textos.add(nome);
            textos.add(Box.createVerticalStrut(2));
            textos.add(detalhe);
            add(textos, BorderLayout.CENTER);

            selo.setFont(EstiloUI.FONTE_ROTULO);
            selo.setHorizontalAlignment(JLabel.CENTER);
            selo.setOpaque(false);
            add(selo, BorderLayout.EAST);
        }

        void atualizar(EstadoJogo e, int meuId, int distancia) {
            boolean emAndamento = e.getStatus() == EstadoJogo.Status.EM_ANDAMENTO;
            daVez = emAndamento && e.getJogadorDaVez() == id;
            voce = meuId == id;
            ativo = e.isAtivo(id);
            cercas = e.getCercasRestantes(id);
            boolean venceu = e.getStatus() == EstadoJogo.Status.FINALIZADO && e.getVencedor() == id;

            nome.setText(e.getNome(id) + (voce ? " (você)" : ""));
            nome.setForeground(ativo ? EstiloUI.TEXTO_PRINCIPAL : EstiloUI.TEXTO_SECUNDARIO);
            Posicao p = e.getPosicao(id);
            String passos = distancia == Integer.MAX_VALUE ? "?" : String.valueOf(distancia);
            detalhe.setText((distancia == 0 ? "chegou!" : "faltam " + passos + (distancia == 1 ? " passo" : " passos"))
                    + " · " + cercas + (cercas == 1 ? " cerca" : " cercas"));
            detalhe.setToolTipText("Posição (" + p.linha() + "," + p.coluna() + ") · meta: " + EstiloUI.META[id - 1]);
            if (venceu) configurarSelo("VENCEU", EstiloUI.COR_PEAO[id - 1]);
            else if (daVez) configurarSelo("VEZ", EstiloUI.DESTAQUE);
            else if (!ativo) configurarSelo("SAIU", EstiloUI.TEXTO_SECUNDARIO);
            else configurarSelo("", null);
            setBackground(voce ? EstiloUI.VOCE_FUNDO : daVez ? EstiloUI.VEZ_FUNDO : EstiloUI.CARTAO);
            repaint();
        }

        private void configurarSelo(String texto, Color cor) {
            selo.setText(texto);
            selo.setForeground(cor == null ? EstiloUI.TEXTO_SECUNDARIO : cor);
            selo.setBorder(texto.isEmpty() ? null : BorderFactory.createEmptyBorder(3, 8, 3, 8));
            selo.putClientProperty("cor", cor);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            EstiloUI.suavizar(g);
            int w = getWidth();
            int h = getHeight();
            Color cor = EstiloUI.COR_PEAO[id - 1];
            g.setColor(getBackground());
            g.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
            g.setColor(daVez ? EstiloUI.DESTAQUE : voce ? EstiloUI.comAlfa(cor, 160) : EstiloUI.BORDA);
            g.setStroke(new BasicStroke(daVez ? 1.8f : 1f));
            g.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);
            // barra lateral na cor do jogador
            g.setColor(ativo ? cor : EstiloUI.comAlfa(cor, 80));
            g.fillRoundRect(0, 10, 4, h - 20, 4, 4);

            // avatar redondo com o número
            int ax = 14;
            int ay = 12;
            g.setColor(ativo ? cor : EstiloUI.comAlfa(cor, 80));
            g.fillOval(ax, ay, AVATAR, AVATAR);
            g.setColor(Color.WHITE);
            g.setFont(EstiloUI.FONTE_PEAO);
            FontMetrics fm = g.getFontMetrics();
            String n = String.valueOf(id);
            g.drawString(n, ax + (AVATAR - fm.stringWidth(n)) / 2, ay + (AVATAR + fm.getAscent() - fm.getDescent()) / 2);

            // inventário: 5 cercas (cheias = disponíveis, vazadas = usadas)
            int bx = ax - 1;
            int by = ay + AVATAR + 8;
            for (int i = 0; i < 5; i++) {
                int x = bx + i * 7;
                if (i < cercas) {
                    g.setColor(ativo ? cor : EstiloUI.comAlfa(cor, 80));
                    g.fillRoundRect(x, by, 5, 12, 3, 3);
                } else {
                    g.setColor(EstiloUI.BORDA);
                    g.drawRoundRect(x, by, 4, 11, 3, 3);
                }
            }

            // pílula do selo
            Color corSelo = (Color) selo.getClientProperty("cor");
            if (corSelo != null && !selo.getText().isEmpty()) {
                var b = selo.getBounds();
                g.setColor(EstiloUI.comAlfa(corSelo, 40));
                g.fillRoundRect(b.x, b.y + (b.height - 20) / 2, b.width, 20, 20, 20);
                g.setColor(EstiloUI.comAlfa(corSelo, 160));
                g.drawRoundRect(b.x, b.y + (b.height - 20) / 2, b.width - 1, 19, 20, 20);
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }
}

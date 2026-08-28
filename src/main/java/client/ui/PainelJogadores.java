package client.ui;

import common.EstadoJogo;
import common.Posicao;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

public class PainelJogadores extends JPanel {
    private final List<JPanel> linhas = new ArrayList<>();
    private final List<JLabel> badges = new ArrayList<>();
    private final List<JLabel> metadados = new ArrayList<>();
    private final List<JPanel> dots = new ArrayList<>();

    public PainelJogadores() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(EstiloUI.FUNDO_JANELA);
        JLabel titulo = new JLabel("JOGADORES");
        titulo.setFont(EstiloUI.FONTE_ROTULO);
        titulo.setForeground(EstiloUI.TEXTO_SECUNDARIO);
        titulo.setBorder(BorderFactory.createEmptyBorder(4, 4, 10, 4));
        add(titulo);
        for (int id = 1; id <= 4; id++) {
            JPanel linha = criarLinha(id);
            linhas.add(linha);
            add(linha);
        }
    }

    private JPanel criarLinha(int id) {
        JPanel linha = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        linha.setBorder(BorderFactory.createMatteBorder(0, 6, 0, 0, EstiloUI.COR_PEAO[id - 1]));

        JPanel dot = new JPanel();
        dot.setPreferredSize(new Dimension(14, 14));
        dot.setBackground(EstiloUI.COR_PEAO[id - 1]);
        dots.add(dot);
        linha.add(dot);

        JLabel nome = new JLabel("Jogador " + id);
        nome.setFont(EstiloUI.FONTE_NORMAL);
        linha.add(nome);

        JLabel meta = new JLabel();
        meta.setFont(EstiloUI.FONTE_ROTULO);
        meta.setForeground(EstiloUI.TEXTO_SECUNDARIO);
        metadados.add(meta);
        linha.add(meta);

        JLabel badge = new JLabel();
        badge.setFont(EstiloUI.FONTE_ROTULO);
        badge.setForeground(EstiloUI.BADGE_VEZ_TEXTO);
        badge.setOpaque(true);
        badges.add(badge);
        linha.add(badge);

        linha.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        return linha;
    }

    public void atualizar(EstadoJogo e, int meuId) {
        for (int id = 1; id <= 4; id++) {
            JPanel linha = linhas.get(id - 1);
            boolean vez = e.getJogadorDaVez() == id;
            boolean voce = meuId == id;
            linha.setBackground(vez ? EstiloUI.BADGE_VEZ_FUNDO
                    : voce ? EstiloUI.VOCE_FUNDO : EstiloUI.FUNDO_JANELA);
            linha.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 6, 0, 0, EstiloUI.COR_PEAO[id - 1]),
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(vez ? EstiloUI.BADGE_VEZ_BORDA
                                    : voce ? EstiloUI.VOCE_BORDA : Color.WHITE),
                            BorderFactory.createEmptyBorder(5, 7, 5, 7))));
            Posicao p = e.getPosicao(id);
            metadados.get(id - 1).setText("(" + p.linha() + "," + p.coluna() + ") · "
                    + e.getCercasRestantes(id) + " cercas");
            badges.get(id - 1).setText(vez ? "VEZ" : "");
            badges.get(id - 1).setBackground(vez ? EstiloUI.BADGE_VEZ_BORDA : EstiloUI.FUNDO_JANELA);
        }
    }

    // Acesso para testes.
    JLabel badge(int idJogador) { return badges.get(idJogador - 1); }
    JPanel linha(int idJogador) { return linhas.get(idJogador - 1); }
}

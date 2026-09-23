package client.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

/** Utilitários visuais compartilhados pelos painéis da lateral. */
final class PainelLateral {
    private PainelLateral() {}

    /** Rótulo de seção em caixa alta, discreto (ex.: "JOGADORES", "HISTÓRICO"). */
    static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(EstiloUI.FONTE_ROTULO);
        l.setForeground(EstiloUI.TEXTO_SECUNDARIO);
        l.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));
        l.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        return l;
    }
}

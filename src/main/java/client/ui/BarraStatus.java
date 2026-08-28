package client.ui;

import javax.swing.JLabel;
import javax.swing.BorderFactory;

public class BarraStatus extends JLabel {
    public BarraStatus() {
        setFont(EstiloUI.FONTE_NORMAL);
        setForeground(EstiloUI.TEXTO_PRINCIPAL);
        setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    }

    public void setMensagem(String texto) {
        setText(texto);
        setForeground(EstiloUI.TEXTO_PRINCIPAL);
    }

    public void setErro(String texto) {
        setText(texto);
        setForeground(EstiloUI.ERRO);
    }

    String getTexto() { return getText(); }
}

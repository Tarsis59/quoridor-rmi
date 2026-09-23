package client.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** Rodapé com a mensagem atual (dica, vez de quem, erro de jogada) e um marcador colorido. */
public class BarraStatus extends JLabel {
    private static final long serialVersionUID = 1L;
    private Color marcador = EstiloUI.TEXTO_SECUNDARIO;

    public BarraStatus() {
        setFont(EstiloUI.FONTE_NORMAL);
        setForeground(EstiloUI.TEXTO_PRINCIPAL);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 34, 12, 16));
    }

    public void setMensagem(String texto) {
        setMensagem(texto, EstiloUI.VERDE_OK);
    }

    /** Mensagem normal com o marcador numa cor (ex.: a cor de quem joga agora). */
    public void setMensagem(String texto, Color corMarcador) {
        setText(texto);
        setForeground(EstiloUI.TEXTO_PRINCIPAL);
        marcador = corMarcador == null ? EstiloUI.TEXTO_SECUNDARIO : corMarcador;
        repaint();
    }

    public void setErro(String texto) {
        setText(texto);
        setForeground(EstiloUI.ERRO);
        marcador = EstiloUI.ERRO;
        repaint();
    }

    String getTexto() { return getText(); }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        EstiloUI.suavizar(g);
        g.setColor(EstiloUI.SUPERFICIE);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(EstiloUI.BORDA);
        g.drawLine(0, 0, getWidth(), 0);
        g.setColor(EstiloUI.comAlfa(marcador, 60));
        g.fillOval(12, getHeight() / 2 - 8, 16, 16);
        g.setColor(marcador);
        g.fillOval(16, getHeight() / 2 - 4, 8, 8);
        g.dispose();
        super.paintComponent(g0);
    }
}

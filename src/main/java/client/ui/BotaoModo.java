package client.ui;

import javax.swing.BorderFactory;
import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** Botão de alternância desenhado no tema (pílula), com o atalho de teclado ao lado do texto. */
public class BotaoModo extends JToggleButton {
    private static final long serialVersionUID = 1L;
    private final String atalho;

    public BotaoModo(String texto, String atalho, boolean selecionado) {
        super(texto, selecionado);
        this.atalho = atalho;
        setFont(EstiloUI.FONTE_NEGRITO);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setRolloverEnabled(true);
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        FontMetrics fa = getFontMetrics(EstiloUI.FONTE_ROTULO);
        return new Dimension(fm.stringWidth(getText()) + fa.stringWidth(atalho) + 44, 34);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        EstiloUI.suavizar(g);
        int w = getWidth();
        int h = getHeight();
        boolean sel = isSelected();
        boolean ativo = isEnabled();
        boolean sobre = getModel().isRollover() && ativo;
        Color fundo = sel ? EstiloUI.DESTAQUE : sobre ? EstiloUI.CASA_BRILHO : EstiloUI.CARTAO;
        g.setColor(fundo);
        g.fillRoundRect(0, 0, w - 1, h - 1, h, h);
        g.setColor(sel ? EstiloUI.DESTAQUE : EstiloUI.BORDA);
        g.drawRoundRect(0, 0, w - 1, h - 1, h, h);

        Color texto = !ativo ? EstiloUI.comAlfa(EstiloUI.TEXTO_SECUNDARIO, 120)
                : sel ? EstiloUI.FUNDO_JANELA : EstiloUI.TEXTO_PRINCIPAL;
        g.setFont(getFont());
        FontMetrics fm = g.getFontMetrics();
        FontMetrics fa = g.getFontMetrics(EstiloUI.FONTE_ROTULO);
        int total = fm.stringWidth(getText()) + 8 + fa.stringWidth(atalho) + 8;
        int x = (w - total) / 2;
        int base = (h + fm.getAscent() - fm.getDescent()) / 2;
        g.setColor(texto);
        g.drawString(getText(), x, base);

        // "tecla" do atalho
        int kx = x + fm.stringWidth(getText()) + 8;
        int kw = fa.stringWidth(atalho) + 8;
        g.setColor(sel ? EstiloUI.comAlfa(EstiloUI.FUNDO_JANELA, 40) : EstiloUI.comAlfa(Color.WHITE, 18));
        g.fillRoundRect(kx, h / 2 - 8, kw, 16, 6, 6);
        g.setFont(EstiloUI.FONTE_ROTULO);
        g.setColor(sel ? EstiloUI.comAlfa(EstiloUI.FUNDO_JANELA, 200) : EstiloUI.TEXTO_SECUNDARIO);
        g.drawString(atalho, kx + 4, (h + fa.getAscent() - fa.getDescent()) / 2);
        g.dispose();
    }
}

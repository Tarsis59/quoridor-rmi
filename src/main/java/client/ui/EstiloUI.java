package client.ui;

import java.awt.Color;
import java.awt.Font;

/** Tema visual "Moderno Plano" — paleta, fontes e medidas centralizadas. */
public final class EstiloUI {
    public static final Color FUNDO_JANELA = new Color(0xEEF1F6);
    public static final Color FUNDO_TABULEIRO = new Color(0xE9EDF2);
    public static final Color CASA = Color.WHITE;
    public static final Color BORDA_CASA = new Color(0xDFE4EA);
    public static final Color CASA_DESTAQUE = new Color(0xEAF7EC);
    public static final Color CERCA = new Color(0x334155);
    public static final Color TOPO = new Color(0x2B3442);
    public static final Color TITULO = new Color(0xF7C948);
    public static final Color TEXTO_PRINCIPAL = new Color(0x2B3442);
    public static final Color TEXTO_SECUNDARIO = new Color(0x7B8794);
    public static final Color BADGE_VEZ_FUNDO = new Color(0xFFF7E0);
    public static final Color BADGE_VEZ_BORDA = new Color(0xF3D16B);
    public static final Color BADGE_VEZ_TEXTO = new Color(0x6B5200);
    public static final Color VOCE_FUNDO = new Color(0xEEF6FF);
    public static final Color VOCE_BORDA = new Color(0xBCD9F7);
    public static final Color ERRO = new Color(0xDC2626);
    public static final Color VERDE_OK = new Color(0x22C55E);
    public static final Color VERMELHO_ERRADO = new Color(0xEF4444);

    public static final Color[] COR_PEAO = {
            new Color(0xEF4444), // J1 vermelho
            new Color(0x3B82F6), // J2 azul
            new Color(0x22C55E), // J3 verde
            new Color(0xF59E0B)  // J4 âmbar
    };
    public static final String[] NOME_COR = {"Vermelho", "Azul", "Verde", "Âmbar"};

    public static final int CELULA = 52;
    public static final int MARGEM = 12;
    public static final int LADO = 2 * MARGEM + 9 * CELULA;
    public static final int ESPESSURA_CERCA = 8;

    public static final Font FONTE_TITULO = new Font("Dialog", Font.BOLD, 16);
    public static final Font FONTE_NORMAL = new Font("Dialog", Font.PLAIN, 13);
    public static final Font FONTE_PEAO = new Font("Dialog", Font.BOLD, 18);
    public static final Font FONTE_ROTULO = new Font("Dialog", Font.BOLD, 10);

    private EstiloUI() {}
}

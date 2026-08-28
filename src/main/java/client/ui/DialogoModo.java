package client.ui;

import javax.swing.JOptionPane;
import java.awt.Component;

/** Diálogo inicial: escolha entre jogar Manual ou Automático (bots ilustrativos). */
public final class DialogoModo {
    public static final String MANUAL = "manual";
    public static final String AUTO = "auto";

    private DialogoModo() {}

    /** Retorna "manual", "auto" ou null se a janela for fechada. */
    public static String perguntar(Component parent) {
        String[] opcoes = { "Modo Manual", "Modo Automático (bots)" };
        int escolha = JOptionPane.showOptionDialog(parent,
                "Como deseja jogar neste processo?",
                "Quoridor — escolha o modo",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, opcoes, opcoes[0]);
        if (escolha == JOptionPane.CLOSED_OPTION) return null;
        return escolha == 1 ? AUTO : MANUAL;
    }
}

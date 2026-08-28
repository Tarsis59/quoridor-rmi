package common;

import java.io.Serializable;

public class JogadaInvalidaException extends Exception implements Serializable {
    private static final long serialVersionUID = 1L;

    public JogadaInvalidaException(String mensagem) {
        super(mensagem);
    }
}

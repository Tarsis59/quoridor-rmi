package client;

import common.EstadoJogo;

/** Guarda o último estado recebido do servidor e acorda quem estiver esperando por um novo. */
public final class CaixaEstado {
    private EstadoJogo estado;

    public synchronized void publicar(EstadoJogo novo) {
        estado = novo;
        notifyAll();
    }

    public synchronized EstadoJogo atual() { return estado; }

    public synchronized EstadoJogo aguardarDiferenteDe(EstadoJogo anterior, long prazoMs)
            throws InterruptedException {
        long limite = System.currentTimeMillis() + prazoMs;
        while (estado == anterior) {
            long falta = limite - System.currentTimeMillis();
            if (falta <= 0) break;
            wait(falta);
        }
        return estado;
    }
}

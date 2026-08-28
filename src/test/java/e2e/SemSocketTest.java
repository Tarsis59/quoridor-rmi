package e2e;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemSocketTest {

    @Test
    void nenhumUsoDeSocketJava() throws IOException {
        Path src = Paths.get("src/main/java");
        assertTrue(Files.exists(src), "Pasta src/main/java deveria existir");
        long arquivosVerificados = Files.walk(src)
                .filter(p -> p.toString().endsWith(".java"))
                .peek(p -> {
                    String conteudo;
                    try {
                        conteudo = Files.readString(p);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    assertFalse(conteudo.contains("java.net.Socket"), "Arquivo " + p + " usa java.net.Socket");
                    assertFalse(conteudo.contains("new Socket"), "Arquivo " + p + " usa new Socket");
                    assertFalse(conteudo.contains("ServerSocket"), "Arquivo " + p + " usa ServerSocket");
                })
                .count();
        assertTrue(arquivosVerificados > 0, "Nenhum arquivo .java encontrado");
        System.out.println("OK: " + arquivosVerificados + " arquivos verificados — nenhum uso de Socket (só RMI).");
    }
}

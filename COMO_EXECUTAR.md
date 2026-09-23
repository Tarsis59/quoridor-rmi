# Como Executar — Quoridor Distribuído (RMI)

Guia **passo a passo, completo e sem erros** para rodar o jogo de **duas formas**:

1. **No terminal** (modo texto, como na primeira demonstração)
2. **Na interface gráfica visual** (janela Swing)

> Sempre que tiver dúvida, volte aqui. Cada comando abaixo já foi testado e funciona.

---

## Pré-requisitos

| Item | Obrigatório | Como conferir |
|------|-------------|---------------|
| **JDK 17+** (testado com 21 Temurin) | Sim | `java -version` |
| **Maven 3.9+** | Sim | `mvn -version` |

> Se `java` ou `mvn` não for reconhecido, instale o JDK 21 (Temurin) e o Maven e adicione `bin` de cada um ao **PATH** do Windows.

---

## Passo 0 — Compilar o projeto (fazer 1 vez)

Abra um terminal **na pasta do projeto** (`PROJETO JOGO SD`) e rode:

**Git Bash (ou terminal Unix):**
```bash
MAVEN_OPTS="-Xmx512m" mvn -q clean package -Dmaven.test.skip=true
```

**PowerShell (Windows):**
```powershell
$env:MAVEN_OPTS="-Xmx512m"; mvn -q clean package -Dmaven.test.skip=true
```

O que acontece:
- Compila todo o código em `target/classes` (é o que o jogo usa).
- `-Dmaven.test.skip=true` **pula os testes** — mais rápido para só jogar.
- `MAVEN_OPTS="-Xmx512m"` evita erro de memória nesta máquina.

Ao terminar, deve aparecer **`BUILD SUCCESS`** (ou nenhum erro). Se aparecer `OutOfMemoryError`, é porque faltou o `MAVEN_OPTS`.

> Para validar tudo (unitários + sem-socket + partida E2E completa), veja a seção **"Rodando os testes"** no fim.

---

# Forma 1 — Jogar no terminal (modo texto)

Você vai usar **5 terminais/janelas separados**: 1 servidor + 4 jogadores.

## Passo 1.1 — Subir o servidor (Terminal 1)

```bash
java -cp target/classes server.ServerMain
```

Aguardado no console:
```
[SERVIDOR] Registry RMI criado na porta 1099
[SERVIDOR] Serviço 'QuoridorServer' disponível em rmi://localhost:1099/QuoridorServer
[SERVIDOR] Aguardando 4 jogadores...
```

> ⚠️ **Nunca suba dois servidores ao mesmo tempo.** Se der erro de porta em uso, veja "Problemas comuns" no fim.

## Passo 1.2 — Abrir os 4 jogadores (Terminais 2 a 5)

Em **4 terminais separados**, um comando em cada:

```bash
java -cp target/classes client.ClientMain --nome Jogador1
```

```bash
java -cp target/classes client.ClientMain --nome Jogador2
```

```bash
java -cp target/classes client.ClientMain --nome Jogador3
```

```bash
java -cp target/classes client.ClientMain --nome Jogador4
```

> 🚀 **A partida começa automaticamente quando o 4º jogador entra.** Antes disso, os clientes ficam em "Aguardando o início da partida (precisa de 4 jogadores)...".

## Passo 1.3 — Como jogar no terminal

Quando **for a sua vez**, o console mostra `Sua vez (Jogador X). Comando:` e exibe `<< VEZ` ao lado do seu peão. Digite:

| Comando | Exemplo | O que faz |
|---------|---------|-----------|
| `mover cima` | `mover cima` | Move 1 casa para cima |
| `mover baixo` | `mover baixo` | Move 1 casa para baixo |
| `mover esquerda` | `mover esquerda` | Move 1 casa para a esquerda |
| `mover direita` | `mover direita` | Move 1 casa para a direita |
| `mover <linha> <coluna>` | `mover 3 4` | Move para uma casa específica (use para pulos diagonais) |
| `cerca <linha> <coluna> <h\|v>` | `cerca 4 4 h` | Coloca uma cerca horizontal/vertical |
| `tabuleiro` | `tabuleiro` | Redesenha o tabuleiro |
| `ajuda` | `ajuda` | Mostra os comandos |
| `sair` | `sair` | Sai do cliente |

- **Linha e coluna vão de 0 a 8** (casa) e, para cerca, a **base vai de 0 a 7**.
- `h` = cerca **abaixo** das casas (l,c) e (l,c+1); `v` = cerca **à direita** das casas (l,c) e (l+1,c).
- Na sua vez o console mostra a lista de **movimentos válidos** — basta escolher um deles.
- Também dá para **pular** sobre um adversário: `mover cima` (ou a direção) quando ele estiver na casa vizinha. Se atrás dele houver cerca, borda ou outro peão, o pulo é **diagonal**: use `mover <linha> <coluna>`.
- Se a jogada for inválida, aparece `[JOGADA INVÁLIDA] ...` e continua sendo a sua vez.

## Passo 1.4 — Exemplo de primeira jogada

| Jogador | Posição inicial | Meta (vencer) |
|---------|-----------------|---------------|
| Jogador 1 | `(8,4)` | chegar na **linha 0** (topo) |
| Jogador 2 | `(0,4)` | chegar na **linha 8** (base) |
| Jogador 3 | `(4,8)` | chegar na **coluna 0** (esquerda) |
| Jogador 4 | `(4,0)` | chegar na **coluna 8** (direita) |

Cada jogador começa com **5 cercas** (regra oficial do Quoridor para 4 jogadores: as 20 cercas do jogo são divididas entre os 4).

Exemplo: se você é o **Jogador 1** (em `(8,4)`), digite `mover cima` para ir a `(7,4)`.

## Bônus — Modo automático no terminal (bôts jogam sozinhos)

Se quiser ver uma partida inteira jogar sozinha **no terminal** (sem digitar nada):

```bash
java -cp target/classes client.ClientMain --nome Bot1 --bot
java -cp target/classes client.ClientMain --nome Bot2 --bot
java -cp target/classes client.ClientMain --nome Bot3 --bot
java -cp target/classes client.ClientMain --nome Bot4 --bot
```

(um comando em cada terminal, com o servidor já rodando). Os 4 bots jogam até alguém vencer e cada terminal imprime `FIM vencedor=...`.

---

# Forma 2 — Jogar na interface gráfica (visual)

Você vai usar **5 terminais/janelas separados**: 1 servidor + 4 janelas gráficas.

## Passo 2.1 — Subir o servidor (Terminal 1)

```bash
java -cp target/classes server.ServerMain
```

## Passo 2.2 — Abrir as 4 janelas em MODO AUTOMÁTICO (Terminais 2 a 5)

Em **4 terminais separados**, um comando em cada (obrigatório `--gui` + `--modo auto`):

```bash
java -cp target/classes client.ClientMain --gui --modo auto --nome Bot1
```

```bash
java -cp target/classes client.ClientMain --gui --modo auto --nome Bot2
```

```bash
java -cp target/classes client.ClientMain --gui --modo auto --nome Bot3
```

```bash
java -cp target/classes client.ClientMain --gui --modo auto --nome Bot4
```

Abrirão **4 janelas** exibindo a mesma partida em tempo real. Os bots jogam em um ritmo lento (~1 jogada a cada 1,5 s) para você acompanhar cada jogada, cada peão e cada cerca colorida. Ao final, uma janela avisa o vencedor e o tabuleiro final continua na tela até você fechar.

## Passo 2.3 — MODO MANUAL (você joga por cliques)

Para **você** jogar com cliques, use `--modo manual` (ou só `--gui`, que pergunta na abertura):

```bash
java -cp target/classes client.ClientMain --gui --modo manual --nome Jogador1
```

**Como jogar por cliques:**
1. Quando **for a sua vez**, as **casas destino legais** ficam destacadas — basta clicar em uma para mover.
2. A barra de ferramentas alterna entre **Mover**, **Cerca H** e **Cerca V**.
3. Movendo o mouse sobre o tabuleiro, aparece um **preview da cerca** (verde = válida, vermelho = inválida).
4. **Clique direito** alterna a orientação H ↔ V rapidamente.
5. A validação final é sempre do servidor; erros aparecem na barra de status inferior.

## Passo 2.4 — Entendendo a janela (cores)

- **Peões**: Jogador 1 = vermelho, Jogador 2 = azul, Jogador 3 = verde, Jogador 4 = âmbar.
- **Cercas coloridas**: cada cerca no tabuleiro tem a **cor do jogador que a colocou** — dá para saber quem montou cada barreira.
- **Painel lateral (JOGADORES)**: cada linha tem uma **barra vertical na cor do jogador**, além de posição, cercas restantes, badge `VEZ` (dourado) e o realce azul "você".
- O topo da janela mostra **qual jogador você é** e o **modo** (Manual/Automático).

---

# Tabela de argumentos (flags)

| Flag | Aplicável a | Padrão | Descrição |
|------|-------------|--------|-----------|
| `--porta <n>` | servidor e cliente | `1099` | Porta do RMI Registry |
| `--host <host>` | cliente | `localhost` | Host do servidor (útil para outra máquina) |
| `--nome <nome>` | cliente | `Jogador` | Nome exibido na partida |
| `--bot` | cliente | — | Modo automático no terminal (IA) |
| `--gui` | cliente | — | Abre a interface gráfica (Swing) |
| `--modo <manual\|auto>` | cliente | diálogo na abertura | Modo da interface gráfica |

Exemplos:
- Porta diferente: `java -cp target/classes server.ServerMain --porta 2000` e, nos clientes, `... --porta 2000`.
- Servidor em outra máquina: `java -cp target/classes client.ClientMain --host 192.168.0.10 --nome Jogador1`.

---

# Rodando os testes (validação completa)

```bash
# Git Bash
MAVEN_OPTS="-Xmx512m" mvn test

# PowerShell
$env:MAVEN_OPTS="-Xmx512m"; mvn test
```

A suíte cobre:
- **Unitários da engine** (movimento, pulos, cercas, cruzamento, caminho garantido, 5 cercas por jogador, vitória, turnos, desconexão/W.O.).
- **Servidor** (sessão com token — ninguém joga no lugar de outro —, queda detectada por ping, W.O.).
- **Bot e console** (estratégia do bot, partidas simuladas até o fim, interpretação de comandos).
- **SemSocketTest**: garante que nenhum arquivo usa `java.net.Socket` (requisito da disciplina — só RMI).
- **E2E completo**: sobe 1 servidor + 4 bots em processos separados e joga uma partida inteira até alguém vencer; e outra em que um cliente é derrubado no meio e a partida continua.
- **Testes da GUI** (geometria, cliques, painel, status).

Deve terminar com `BUILD SUCCESS` e sem falhas.

---

# Problemas comuns e soluções (para nunca errar)

| Sintoma | Causa | Solução |
|---------|-------|---------|
| `[CLIENTE] Servidor não encontrado...` | Servidor não está de pé ainda, ou porta errada | Suba o **servidor primeiro**; confira `--porta` |
| `Port already in use: 1099` / `ExportException` | Sobrou um servidor rodando | Mate o processo na porta 1099 (ver abaixo) e rode de novo |
| `java` não é reconhecido | Java fora do PATH | Instale o JDK 17+ (Temurin) e ajuste o PATH |
| `OutOfMemoryError` no build | RAM baixa | Use `MAVEN_OPTS="-Xmx512m"` sempre |
| `Não é a sua vez: agora joga o Jogador X` | Você tentou jogar fora da sua vez | Espere o console mostrar `<< VEZ` na sua linha |
| Cerca recusada | Cerca sobrepõe/cruza outra ou **bloquearia o caminho de alguém** (regra do BFS) | Escolha outra posição/orientação |
| `Você não possui mais cercas` | Já usou as 5 cercas | Só resta mover o peão |
| `Sala cheia` | Já há 4 jogadores na partida | Reinicie o servidor para uma nova partida |
| Um jogador fechou a janela | Queda detectada pelo servidor (ping) | Nada a fazer: a vez dele é pulada; se sobrar 1, ele vence por W.O. |
| Partida não começa | Menos de 4 clientes conectados | Abra exatamente **4 jogadores**, um por terminal |

### Matar um servidor preso na porta 1099 (PowerShell)

```powershell
Get-NetTCPConnection -LocalPort 1099 -State Listen |
  ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

Ou, se souber o PID: `taskkill /F /PID <numero>`.

---

# Cola rápida (resumo)

```bash
# 1. Compilar
$env:MAVEN_OPTS="-Xmx512m"; mvn -q clean package -Dmaven.test.skip=true   # (PowerShell)

# 2. Servidor (Terminal 1)
java -cp target/classes server.ServerMain

# 3a. Jogar no TERMINAL (Terminais 2 a 5)
java -cp target/classes client.ClientMain --nome Jogador1
java -cp target/classes client.ClientMain --nome Jogador2
java -cp target/classes client.ClientMain --nome Jogador3
java -cp target/classes client.ClientMain --nome Jogador4

# 3b. Jogar na GUI automática (Terminais 2 a 5)
java -cp target/classes client.ClientMain --gui --modo auto --nome Bot1
java -cp target/classes client.ClientMain --gui --modo auto --nome Bot2
java -cp target/classes client.ClientMain --gui --modo auto --nome Bot3
java -cp target/classes client.ClientMain --gui --modo auto --nome Bot4

# 3c. Jogar na GUI manual (você clica)
java -cp target/classes client.ClientMain --gui --modo manual --nome Jogador1
```

> Lembrete: **1 servidor + 4 clientes**, cada um em um terminal. A partida começa sozinha no 4º jogador.

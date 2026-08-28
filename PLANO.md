# Quoridor Distribuído (RMI) — Sistemas Distribuídos

> Plano completo do projeto: do design à entrega. Tecnologia: **Java RMI puro** (`java.rmi`).

---

## 1. Escolha tecnológica e por quê

Recomendo **Java RMI puro** (`java.rmi`). Motivos:

- É a implementação "canônica" de RMI, sem libs externas, sem gerar stubs manualmente (desde Java 5+ isso é automático).
- Suporta nativamente **callback**: o cliente também pode expor um objeto remoto para o servidor chamar de volta — essencial aqui, porque em RMI puro o cliente só chama o servidor, mas o servidor precisa avisar os outros 3 jogadores quando alguém move ou coloca uma cerca. Sem socket e sem callback, os outros clientes nunca saberiam que é a vez deles.
- Zero dependência de rede/serialização externa — tudo `Serializable` padrão do Java.
- É o que a banca normalmente espera quando o enunciado diz "RMI" explicitamente antes de "RPC".

Se preferir outra linguagem (Python + Pyro5, C# + .NET Remoting/gRPC, etc.), a arquitetura é praticamente a mesma — troca-se apenas a sintaxe. Detalhado em Java por ser o caminho mais direto e com menos armadilhas para RMI com callback.

---

## 2. Arquitetura

```
                          ┌─────────────────────────────┐
                          │   RMI Registry (porta 1099) │
                          └─────────────┬───────────────┘
                                        │ bind / lookup
                          ┌─────────────▼───────────────┐
                          │        GameServerImpl       │
                          │  Estado · regras · turnos   │
                          └─────────────┬───────────────┘
                 chamada RMI (move/wall) │ ▲ callback (broadcast de estado)
          ┌───────────┬──────────┬───────┴───────┬───────────┐
          ▼           ▼          ▼               ▼           ▼
  ┌────────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
  │ Cliente 1  │ │ Cliente 2│ │ Cliente 3│ │ Cliente 4│ │   ...    │
  │ Jogador 1  │ │ Jogador 2│ │ Jogador 3│ │ Jogador 4│ │          │
  │ + callback │ │ +callback│ │ +callback│ │ +callback│ │          │
  └────────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘
```

- O servidor cria o **RMI Registry embutido** no próprio processo (`LocateRegistry.createRegistry(1099)`), evitando depender do utilitário externo `rmiregistry`, que costuma dar dor de cabeça em demonstração.
- Cada cliente, ao iniciar, faz `lookup` do servidor e chama `registrar(callback, nome)` passando seu próprio objeto remoto (a interface de callback) — é isso que permite o servidor "empurrar" atualizações.
- Toda chamada de jogo (`mover`, `colocarCerca`) é uma chamada **RMI síncrona cliente → servidor**. O servidor valida, atualiza o estado e, se válido, notifica todos os 4 clientes via callback (`aoAtualizarEstado`).
- Isso satisfaz **"não pode usar Socket"**: RMI usa sockets internamente de forma transparente, mas você nunca escreve `new Socket(...)` — é isso que a disciplina cobra.

---

## 3. Modelagem do domínio

Pacote `common` (compartilhado entre server e client — precisa estar no classpath dos dois):

```
common/
  Posicao.java              // record/classe: int linha, int coluna
  Cerca.java                // Posicao base + Orientacao (HORIZONTAL/VERTICAL)
  EstadoJogo.java           // snapshot serializável: posições dos 4 peões,
                            // lista de cercas colocadas, cercas restantes por
                            // jogador, jogador da vez, status (AGUARDANDO/
                            // EM_ANDAMENTO/FINALIZADO), vencedor
  GameServer.java           // interface Remote (chamadas do cliente → servidor)
  ClientCallback.java       // interface Remote (chamadas do servidor → cliente)
  JogadaInvalidaException.java // exception customizada, deve ser Serializable
```

### GameServer (interface remota do servidor)

```java
public interface GameServer extends Remote {
    int registrar(ClientCallback callback, String nomeJogador) throws RemoteException;
    void mover(int idJogador, Posicao destino) throws RemoteException, JogadaInvalidaException;
    void colocarCerca(int idJogador, Cerca cerca) throws RemoteException, JogadaInvalidaException;
    EstadoJogo obterEstado() throws RemoteException; // útil para sincronizar/reconectar
}
```

### ClientCallback (interface remota do cliente, implementada por cada cliente)

```java
public interface ClientCallback extends Remote {
    void aoIniciarJogo(EstadoJogo estado) throws RemoteException;
    void aoAtualizarEstado(EstadoJogo estado) throws RemoteException;
    void aoFinalizarJogo(int idVencedor) throws RemoteException;
}
```

> **Padrão de erro:** erros de jogada (movimento inválido, cerca bloqueando todos os caminhos) voltam como exception direta na chamada do próprio jogador — não precisa de callback pra isso, é mais simples e imediato. O callback serve só para sincronizar os outros 3 clientes depois de uma jogada válida.

---

## 4. O algoritmo crítico (é aqui que a maioria perde pontos)

Duas regras do Quoridor são as que costumam ser mal implementadas:

### 4.1 Pulo sobre peão adjacente

Ao mover, se a casa de destino direto está ocupada por outro peão:

1. Verifique a casa **atrás** do peão adversário (mesma direção). Se estiver livre e sem cerca no caminho → **pulo permitido**.
2. Se essa casa estiver bloqueada (cerca ou borda do tabuleiro), tente as **duas casas perpendiculares** ao lado do adversário.
3. Se ambas também bloqueadas → **movimento não permitido** nessa direção.

### 4.2 Cerca não pode eliminar o último caminho de ninguém

Depois de simular a colocação da cerca (antes de confirmar), rode uma busca em largura (**BFS**) a partir da posição de cada um dos 4 peões até qualquer casa da respectiva borda de destino:

```java
boolean temCaminho(EstadoTabuleiro tabuleiroSimulado, Posicao origem, Predicate<Posicao> ehDestino) {
    Queue<Posicao> fila = new LinkedList<>();
    Set<Posicao> visitados = new HashSet<>();
    fila.add(origem); visitados.add(origem);
    while (!fila.isEmpty()) {
        Posicao atual = fila.poll();
        if (ehDestino.test(atual)) return true;
        for (Posicao vizinho : vizinhosAlcancaveis(tabuleiroSimulado, atual)) {
            if (visitados.add(vizinho)) fila.add(vizinho);
        }
    }
    return false;
}
```

Se qualquer um dos 4 jogadores ficar sem caminho após a simulação, a jogada de cerca é **rejeitada** (`JogadaInvalidaException`) — mesmo que a cerca "só" bloqueie o caminho de um adversário. Deve ser testado com casos extremos (cercas quase formando um cercadinho completo).

### 4.3 Destinos de cada jogador (variante 4 jogadores)

| Jogador | Posição inicial | Linha/coluna de chegada |
|---------|-----------------|--------------------------|
| Jogador 1 (embaixo)  | linha 8, coluna 4 | chega na **linha 0** (topo) |
| Jogador 2 (topo)     | linha 0, coluna 4 | chega na **linha 8** (base) |
| Jogador 3 (direita)  | linha 4, coluna 8 | chega na **coluna 0** |
| Jogador 4 (esquerda) | linha 4, coluna 0 | chega na **coluna 8** |

---

## 5. Passo a passo de implementação (ordem recomendada)

Ambiente: projeto **Maven simples** (sem frameworks pesados), **JDK 17+**, um único módulo com 3 pacotes (`common`, `server`, `client`) é suficiente — não precisa de multi-módulo.

1. **Modelagem pura (sem rede):** implemente `Tabuleiro`, `Peao`, `Cerca` e as regras de movimento/cerca como classes Java comuns, testáveis isoladamente com JUnit antes de tocar em RMI. *Essa é a parte que decide sua nota — resolva 100% aqui primeiro.*
2. **Testes unitários da engine:** casos de movimento simples, pulo reto, pulo lateral, movimento bloqueado por cerca, colocação de cerca válida/inválida, cerca que fecharia caminho (deve ser rejeitada), condição de vitória para cada um dos 4 lados.
3. **Definir as interfaces remotas** (`GameServer`, `ClientCallback`) e as classes `Serializable` do modelo de transporte (`EstadoJogo`, `Posicao`, `Cerca`).
4. **Implementar `GameServerImpl`:** encapsula a engine, controla a fila de turnos (só aceita jogada do `idJogador` da vez), mantém a lista de callbacks registrados, e após cada jogada válida chama `aoAtualizarEstado` em todos os callbacks.
5. **`ServerMain`:** cria o registry embutido, faz `bind`, aguarda os 4 jogadores se registrarem antes de liberar o início (`aoIniciarJogo` disparado quando o 4º cliente registra).
6. **Implementar `ClientCallbackImpl`** no lado cliente — só recebe o snapshot e atualiza a UI local.
7. **UI do cliente** (modo texto/console primeiro): desenhar o tabuleiro 9x9 em caracteres (ex.: `.` casas livres, letras/números para peões, `--`/`|` para cercas), loop de leitura de comando (`mover cima`, `cerca 3 4 h`, etc.), tratamento visível da exception de jogada inválida.
8. **`ClientMain`:** faz `lookup` do servidor, se registra passando seu callback, entra no loop de UI.
9. **Testes de integração manual:** 1 terminal para o servidor + 4 terminais para os clientes, todos na mesma máquina (localhost), simulando uma partida completa até alguém vencer.
10. **Casos de borda finais:** jogador tenta jogar fora de turno (deve ser rejeitado no servidor, nunca confiar só no cliente); cliente desconecta no meio do jogo (trate `RemoteException` no callback e apenas remova/pause o jogador, sem derrubar o servidor).
11. **Documentação:** README com instruções de execução, breve relatório (arquitetura, como RMI foi usado, decisões de design, prints/vídeo do funcionamento).

---

## 6. Estrutura de pastas sugerida

```
quoridor-distribuido/
├── pom.xml
├── src/main/java/
│   ├── common/   (interfaces remotas + modelos serializáveis)
│   ├── engine/   (regras do jogo — puro, sem RMI)
│   ├── server/   (GameServerImpl, ServerMain)
│   └── client/   (ClientCallbackImpl, ConsoleUI, ClientMain)
├── src/test/java/engine/   (testes JUnit da engine)
├── README.md
└── docs/relatorio.md
```

---

## 7. Como rodar localmente (para o vídeo/demo)

```bash
mvn clean package
java -cp target/classes server.ServerMain
# em outros 4 terminais:
java -cp target/classes client.ClientMain --nome Jogador1
java -cp target/classes client.ClientMain --nome Jogador2
java -cp target/classes client.ClientMain --nome Jogador3
java -cp target/classes client.ClientMain --nome Jogador4
```

---

## 8. Plano de testes

| O quê                  | Tipo      | Foco |
|------------------------|-----------|------|
| Movimentos ortogonais  | Unitário  | 1 casa, todas direções, borda do tabuleiro |
| Pulo sobre peão        | Unitário  | pulo reto, pulo lateral, pulo bloqueado por ambos os lados |
| Colocação de cerca     | Unitário  | sobreposição, cruzamento, cerca fora do tabuleiro |
| Caminho garantido      | Unitário  | cerca que isolaria um jogador deve ser rejeitada |
| Condição de vitória    | Unitário  | um caso por lado (4 jogadores) |
| Ordem de turnos        | Integração | jogador fora da vez é rejeitado pelo servidor |
| Broadcast de estado    | Integração | os 3 clientes não-ativos recebem o novo estado após jogada |
| Partida completa       | Manual/E2E | 1 servidor + 4 clientes, do início ao fim |

---

## 9. Checklist final de entrega

- [ ] Engine com regras 100% cobertas por testes unitários
- [ ] RMI sem nenhum uso de `java.net.Socket`
- [ ] 4 processos cliente distintos + 1 processo servidor, comprovado em vídeo/print
- [ ] Callback funcionando (sincronização entre os 4 clientes)
- [ ] README com passo a passo de execução
- [ ] Relatório curto explicando a arquitetura e decisões
- [ ] Código versionado (git) com histórico de commits coerente com as fases acima

---

*Fonte: plano original do trabalho — Quoridor Distribuído (RMI) para a disciplina de Sistemas Distribuídos.*

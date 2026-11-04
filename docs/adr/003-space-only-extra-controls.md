# [ADR-003] Concentrar os controles extras na tecla Espaço

* **Status:** Aceito
* **Data:** 2026-11-04

## Contexto e Problema

Além do movimento do braço, o jogo precisa permitir iniciar, consultar instruções, pausar e retomar sem exigir controles simultâneos ou combinações de teclas. A interação deve ser simples para quem está usando o braço para jogar.

Foram consideradas teclas distintas, mouse e uma única tecla com ações condicionadas ao estado e à duração do pressionamento.

## Decisão

Decidimos usar somente **Espaço** para iniciar e para as funcionalidades extras. A duração e o estado atual determinam a ação:

* Na tela inicial, tocar e soltar inicia a preparação; segurar por 0,5 s abre o tutorial. Soltar no tutorial retorna à tela inicial.
* Durante contagem regressiva ou partida, pressionar pausa; pressionar novamente retoma o estado anterior.
* Na tela de Game Over, pressionar repete a fase. Na tela de vitória, segurar reinicia a partida; tocar e soltar abre os resultados.
* Na tela de resultados, tocar e soltar retorna à tela final.

## Consequências

* **Positivo:** Um controle fácil de descobrir, sem exigir mouse ou comandos simultâneos durante o exercício.
* **Negativo:** A ação de Espaço depende do estado atual; o tutorial de pressionamento longo precisa ser comunicado ao jogador.
* **Riscos:** Uma duração de pressionamento mal interpretada pode abrir o tutorial ou reiniciar a partida quando o jogador esperava um toque curto.

## Conformidade (Opcional)

Manter a leitura de teclado em um único InputAdapter no GameController. Novas ações extras devem ser mapeadas para Espaço e descritas junto às instruções exibidas no jogo.

## Referências

* [GameController.java](../../core/src/main/java/games/project/desability_geometry_dash/GameController.java)

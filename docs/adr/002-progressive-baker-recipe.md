# [ADR-002] Organizar a coleta como uma receita de padeiro em fases

* **Status:** Aceito
* **Data:** 2026-11-04

## Contexto e Problema

O jogo precisa dar um objetivo claro ao movimento do braço e aumentar a tarefa gradualmente. Uma coleta sem lista de ingredientes não explicaria por que cada item aparece nem como o jogador avança até preparar o bolo de cenoura.

Foram consideradas rodadas sem objetivos, uma lista fixa para todas as fases e uma receita que cresce com o progresso. A última opção cria metas verificáveis e permite introduzir ingredientes em etapas.

## Decisão

Decidimos representar a atividade como um jogo de padeiro: o jogador move o chef com o braço para coletar os ingredientes pedidos. GameRules.requirements define os ingredientes e quantidades por fase. As fases começam com ovo, farinha, cenoura e açúcar; acrescentam fermento na segunda e chocolate na terceira. A terceira fase encerra a receita e leva à tela final do bolo.

FallingIngredient representa os itens que caem, Chef controla a cesta e as colisões, e GameController coordena objetivos, pontuação e avanço de fase.

## Consequências

* **Positivo:** A receita comunica o objetivo, dá retorno de progresso e permite aumentar a quantidade de itens gradualmente.
* **Negativo:** Regras e balanceamento precisam ser mantidos quando ingredientes ou fases mudarem.
* **Riscos:** Itens extras ou incorretos consomem vidas; a frequência de queda e a velocidade dos itens podem tornar a coleta mais difícil.

## Conformidade (Opcional)

Manter a lista de ingredientes por fase em GameRules e usar GameController para aplicar objetivos e avançar somente quando todas as quantidades forem coletadas.

## Referências

* [GameRules.java](../../core/src/main/java/games/project/desability_geometry_dash/GameRules.java)
* [GameController.java](../../core/src/main/java/games/project/desability_geometry_dash/GameController.java)
* [Chef.java](../../core/src/main/java/games/project/desability_geometry_dash/Chef.java)
* [FallingIngredient.java](../../core/src/main/java/games/project/desability_geometry_dash/FallingIngredient.java)
* [Fase 2: Receita e coleta](../phases/phase-2-baker-recipe/_overview.md)

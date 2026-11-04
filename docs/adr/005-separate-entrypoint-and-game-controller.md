# [ADR-005] Separar o ponto de entrada do controlador da partida

* **Status:** Aceito
* **Data:** 2026-11-04

## Contexto e Problema

Main acumulava o ciclo de vida do libGDX, regras, leitura do sensor, desenho e gerenciamento das fases. A classe ficou difícil de navegar e de manter conforme novas responsabilidades foram adicionadas.

Foram consideradas manter tudo na classe, extrair métodos isolados para utilitários e transferir a coordenação da partida para um controlador, deixando Main como ponto de entrada.

## Decisão

Decidimos manter Main como adaptador do ApplicationListener: cada callback encaminha diretamente o evento ao GameController. O controlador mantém o estado da partida e coordena as entidades e utilitários. Regras, escrita de texto, preparação de texturas e métricas ficam em GameRules, TextWriter, TextureFactory e SensorMotionMetrics.

## Consequências

* **Positivo:** Main fica curto e sem métodos auxiliares privados; cada componente tem uma responsabilidade mais clara.
* **Negativo:** A inicialização da aplicação passa por um objeto controlador adicional, e mudanças no ciclo de vida precisam continuar sendo encaminhadas pelos callbacks.
* **Riscos:** Se um novo callback do ApplicationListener for adicionado ou a interface mudar, Main e GameController devem continuar sincronizados.

## Conformidade (Opcional)

Manter em Main somente os callbacks exigidos pelo libGDX e o encaminhamento direto para GameController. Evitar recolocar regras e desenho nessa classe.

## Referências

* [Main.java](../../core/src/main/java/games/project/desability_geometry_dash/Main.java)
* [GameController.java](../../core/src/main/java/games/project/desability_geometry_dash/GameController.java)
* [Fase 4: Separação arquitetural](../phases/phase-4-main-controller/_overview.md)

# [ADR-004] Coletar métricas de movimento e desempenho

* **Status:** Aceito
* **Data:** 2026-11-04

## Contexto e Problema

O jogo precisa mostrar progresso e resumir a movimentação realizada durante as fases. A distância serial do HC-SR04 já oferece amostras temporizadas que permitem calcular amplitude e velocidade sem adicionar sensores.

Foram consideradas apenas métricas de pontuação, envio das amostras para armazenamento externo e cálculo local durante a partida. O escopo atual requer retorno imediato na tela de resultados e não requer serviço externo.

## Decisão

Decidimos calcular as métricas localmente. GameController mantém pontuação, vidas e recorde; SensorMotionMetrics acumula a distância mínima e máxima por fase, distância percorrida e duração dos trechos de movimento. A amplitude apresentada como ROM é máximo menos mínimo. A velocidade de pico guarda a maior velocidade observada; a velocidade média global divide a distância acumulada pela duração acumulada. UltrassonicSensorReader rejeita leituras acima de 50 cm antes de atualizar a amostra, e SensorMotionMetrics aplica a mesma validação antes de acumular distância.

O movimento só é acumulado quando uma amostra válida difere da anterior em pelo menos 1 cm. O recorde de pontuação é salvo nas preferências locais do libGDX. A tela de resultados apresenta pontuação, ROM de cada fase, pico e média de velocidade.

## Consequências

* **Positivo:** O jogador recebe um resumo do desempenho sem equipamento ou infraestrutura adicionais.
* **Negativo:** As métricas dependem da qualidade e frequência das leituras do HC-SR04; o cálculo local existe apenas durante a partida, com exceção do recorde.
* **Riscos:** Pausas, leituras inválidas e ruído do sensor afetam a precisão da duração e da velocidade estimadas.

## Conformidade (Opcional)

Manter o processamento das amostras em SensorMotionMetrics e encerrar o trecho atual nas transições de fase e de partida. Exibir as unidades nas mensagens de resultados.

## Referências

* [SensorMotionMetrics.java](../../core/src/main/java/games/project/desability_geometry_dash/SensorMotionMetrics.java)
* [GameController.java](../../core/src/main/java/games/project/desability_geometry_dash/GameController.java)
* [Fase 3: Métricas da partida](../phases/phase-3-gameplay-metrics/_overview.md)

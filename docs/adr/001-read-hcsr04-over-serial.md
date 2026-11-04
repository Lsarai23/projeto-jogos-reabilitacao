# [ADR-001] Ler o HC-SR04 pela porta serial

* **Status:** Aceito
* **Data:** 2026-11-04

## Contexto e Problema

O jogo precisa transformar o movimento horizontal do braço em uma posição no eixo X, sem depender de teclado ou câmera. O HC-SR04 mede o tempo de ida e volta do pulso ultrassônico; o Arduino converte esse tempo em centímetros e envia as amostras ao computador.

Foram consideradas a entrada por teclado, o rastreamento por câmera e a leitura de um sensor ultrassônico conectado ao Arduino. O escopo atual requer um único eixo de movimento e utiliza hardware disponível no projeto.

## Decisão

Decidimos usar o HC-SR04 ligado ao Arduino e transmitir as distâncias por serial a 9600 baud. O sketch usa os pinos Trigger 13 e Echo 12 e calcula a distância como tempoEmMicrossegundos × 0.01723, considerando o percurso de ida e volta do som. A variável de distância é inteira, então o serial envia centímetros inteiros. A amostra é enviada a cada 100 ms.

No computador, UltrassonicSensorReader lê a porta serial em uma thread separada, converte cada linha em um inteiro e associa a ela um timestamp monotônico. Distâncias acima de 50 cm são rejeitadas antes de atualizar a amostra disponível ao jogo; somente o instante do aviso é guardado. Durante a calibração, o controlador reinicia a contagem de cinco segundos e pede para manter o braço no campo de visão. Durante a partida, pausa e aguarda uma nova distância válida antes da contagem de três segundos para retomar. Chef normaliza apenas a distância aceita na faixa calibrada.

## Consequências

* **Positivo:** Reaproveita o hardware disponível, oferece controle de um eixo sem contato e separa a leitura serial do ciclo de renderização.
* **Negativo:** Exige Arduino, sensor, conexão serial e configuração correta da porta (atualmente COM3); não mede outros eixos e limita o movimento aceito a 50 cm.
* **Riscos:** Leituras podem variar com a superfície, o ângulo ou a distância. Uma porta fechada ou uma linha inválida interrompe a entrada útil do jogo.

## Conformidade (Opcional)

Manter a conversão de tempo para centímetros no sketch e o protocolo serial numérico em 9600 baud. A leitura Java deve continuar fora da thread de renderização e fornecer timestamp junto da distância.

## Referências

* [Sketch Arduino](../../ultrassonic_reader/ultrassonic_reader.ino)
* [UltrassonicSensorReader.java](../../core/src/main/java/games/project/desability_geometry_dash/UltrassonicSensorReader.java)
* [Chef.java](../../core/src/main/java/games/project/desability_geometry_dash/Chef.java)
* [Fase 1: Sensor HC-SR04](../phases/phase-1-hcsr04/_overview.md)

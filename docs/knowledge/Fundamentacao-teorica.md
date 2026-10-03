# Síntese de Embasamento Científico (com Citações ABNT)

## 1. Fundamentação do Movimento Horizontal (Eixo X)
* **Projeção em Plano 2D**: O uso de interfaces exibidas em **monitores 2D normais** para tarefas de alcance (*reaching tasks*) no plano transverso/horizontal demonstra que o controle visuomotor no eixo mediolateral (eixo X) é intuitivo, eficaz e evita compensações posturais (PASQUINI et al., 2026).
* **Eficácia de Tarefas de Alcance Plano (*Reaching Tasks*)**: O treino repetitivo de alcance no plano horizontal (*motion on a plane*) comprova que exercícios focados no deslocamento plano do membro superior geram ganhos motores estatisticamente significativos na escala clínica *Fugl-Meyer* (FM) em pacientes pós-AVC (CHA et al., 2021).
* **Conforto Visual e Prevenção de Enjoo**: Jogos sérios exibidos em telas 2D convencionais evitam os sintomas de desorientação e tontura (*cybersickness*) frequentemente provocados por óculos de realidade virtual imersiva (HMDs) em pacientes com comprometimento motor (AFYOUNI; MURAD; EINEA, 2020).

---

## 2. Vantagem Econômica e Técnica do Sensor Ultrassônico (1 DoF)
* **Alto Custo dos Sistemas Tradicionais**: Sistemas comerciais de rastreamento baseados em trajes vestíveis ou marcadores corporais (*tracking suits* como OptiTrack ou Vicon) possuem custo elevado, longa etapa de preparação (*setup*) e causam desconforto ao paciente (AFYOUNI; MURAD; EINEA, 2020; CHA et al., 2021).
* **Limitações de Câmeras e Sensores Ópticos**: Dispositivos de mercado como o *Leap Motion* possuem volume de rastreamento restrito a um pequeno cone de visão (CHA et al., 2021), enquanto sensores de profundidade (*Kinect*) exigem alto poder computacional e sofrem falhas de oclusão visual quando há rigidez muscular, espasticidade ou sobreposição de membros (AFYOUNI; MURAD; EINEA, 2020).
* **Justificativa do Sensor Ultrassônico (1 DoF)**: O sensor de proximidade ultrassônico oferece uma interface sem contato (*markerless*) de **ultra-baixo custo**, com baixo consumo computacional e independência em relação à postura da mão ou fechamento dos dedos (AFYOUNI; MURAD; EINEA, 2020; CHA et al., 2021; PASQUINI et al., 2026).

---

## 3. Calibração e Mecanismo Facilitador de Ganho (*Gain/ROM Scaling*)
* **Amplificação Visual do Movimento (*Gain*)**: A aplicação de um fator de amplificação visual (*Gain* = 2) sobre o deslocamento físico do membro reduz a Amplitude de Movimento (*Range of Motion - ROM*) necessária em aproximadamente 50%, atuando como um **mecanismo facilitador** para que pacientes com maior limitação motora consigam atingir os alvos na tela (PASQUINI et al., 2026).
* **Adaptação à Capacidade do Usuário**: A calibração prévia do espaço de alcance útil do paciente e o ajuste dinâmico da dificuldade previnem a frustração e mantêm o engajamento durante a terapia domiciliar (AFYOUNI; MURAD; EINEA, 2020; CHA et al., 2021; PASQUINI et al., 2026).
* **Aplicação Prática no Jogo**: Durante a etapa de calibração, o sistema registra o menor e o maior valor de distância lidos pelo ultrassom (ROM do usuário); em seguida, aplica um **fator de escala/ganho em software**, garantindo que o movimento físico limitado do braço consiga deslocar o personagem por toda a largura da tela (AFYOUNI; MURAD; EINEA, 2020; PASQUINI et al., 2026).

---

## 4. Métricas Recomendadas para o Jogo de Coleta de Objetos
As seguintes métricas quantitativas e qualitativas foram extraídas da literatura para acompanhar a evolução do paciente:

1. **Taxa de Objetos Coletados / Taxa de Sucesso (*Collected Targets - CT / Success Rate*)**: Razão entre a quantidade de objetos coletados e o total de objetos lançados na partida (AFYOUNI; MURAD; EINEA, 2020; PASQUINI et al., 2026).
2. **Amplitude de Movimento Real (*Range of Motion - ROM*)**: Variação da distância física real (mínima e máxima em centímetros) registrada pelo sensor ultrassônico a cada sessão (AFYOUNI; MURAD; EINEA, 2020; CHA et al., 2021).
3. **Pico e Média de Velocidade (*Peak Velocity / SPEED*)**: Taxa de variação da distância no tempo (\\(\Delta d / \Delta t\\)) nos movimentos de aproximação e afastamento, avaliando o ganho de agilidade motora (AFYOUNI; MURAD; EINEA, 2020; CHA et al., 2021; PASQUINI et al., 2026).
4. **Comprimento de Trajetória Normalizado (*Normalized Path Length - NPL*)**: Razão entre a distância percorrida pelo personagem no eixo X e a menor distância direta até o objeto, identificando hesitações ou oscilações do braço (PASQUINI et al., 2026).
5. **Suavidade do Movimento (*Spectral Arc Length - SPARC*)**: Métrica cinemática que mensura a fluidez do movimento e a ausência de tremores ou interrupções bruscas durante o deslocamento (PASQUINI et al., 2026).
6. **Tempo de Reação e Execução (*TIME / Reaction Time*)**: Intervalo decorrido entre a queda do objeto na tela e o início/conclusão do movimento físico de alinhamento pelo paciente (AFYOUNI; MURAD; EINEA, 2020; PASQUINI et al., 2026).
7. **Escala de Usabilidade do Sistema (*System Usability Scale - SUS*)**: Questionário padronizado aplicado para avaliar o conforto, facilidade de uso e aceitação do sistema pelo usuário (AFYOUNI; MURAD; EINEA, 2020; CHA et al., 2021).

---

## Referências Bibliográficas (Norma ABNT)

AFYOUNI, Imad; MURAD, Abdullah; EINEA, Anas. Adaptive Rehabilitation Bots in Serious Games. **Sensors**, v. 20, n. 24, p. 7037, 2020.

CHA, Kuan et al. A novel upper-limb tracking system in a virtual environment for stroke rehabilitation. **Journal of NeuroEngineering and Rehabilitation**, v. 18, n. 1, p. 1-13, 2021.

PASQUINI, Guido et al. A serious game for assessing upper-limb visuomotor adaptation in children with cerebral palsy during reaching tasks in virtual reality. **Scientific Reports**, v. 16, n. 1, p. 1-14, 2026.

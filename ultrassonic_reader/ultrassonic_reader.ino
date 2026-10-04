// C++ code
//
int distancia = 0;

//Retorna para o retorno do pulso, em microssegundos
long readUltrasonicDistance(int triggerPin, int echoPin)
{
  pinMode(triggerPin, OUTPUT);  // Limpa o trigger
  digitalWrite(triggerPin, LOW);
  delayMicroseconds(2);
  // Liga o pino trigger por 10 microssegundos
  digitalWrite(triggerPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(triggerPin, LOW);
  pinMode(echoPin, INPUT);
  // Faz a leitura do pino Echo (que recebe o pulso de retorno) e calcula o tempo em microssegundos
  return pulseIn(echoPin, HIGH);
}

void setup()
{
  Serial.begin(9600);
}

void loop()
{
  //velocidade do som no ar = 343 m/s = 0,0343 cm/microssegundos
  //considerando que readUltrasonicDistance retorna tempo ida + volta:
  // 0,0343/2 = 0,01723 cm/microssegundos
  // 0,01723 cm/microssegundos * x microssegundos = distancia em cm
  distancia = 0.01723 * readUltrasonicDistance(13, 12);
  Serial.println(distancia);
  delay(100); // Espera 1000 milisegundos
}

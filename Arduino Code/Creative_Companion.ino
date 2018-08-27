#include <Tle493d_w2b6.h>

Tle493d_w2b6 Tle493dMagnetic3DSensor = Tle493d_w2b6();
void setup() {
  Serial.begin(9600);


  pinMode (0, OUTPUT);
  pinMode (1, OUTPUT);
  pinMode (2, OUTPUT);
  pinMode (3, OUTPUT);
  pinMode (4, OUTPUT);
  pinMode (5, OUTPUT);
  pinMode (8, OUTPUT);
  pinMode (9, OUTPUT);
  digitalWrite(0, LOW);
  digitalWrite(1, LOW);
  digitalWrite(2, LOW);
  digitalWrite(3, LOW);
  digitalWrite(4, LOW);
  digitalWrite(5, LOW);
  digitalWrite(8, LOW);
  digitalWrite(9, LOW);

  while (!Serial);
  Tle493dMagnetic3DSensor.begin();
  Tle493dMagnetic3DSensor.begin();

  Tle493dMagnetic3DSensor.enableTemp();
  //  Tle493dMagnetic3DSensor.setAccessMode(AccessMode_e.FASTMODE);
  for (int i = 0; i < 100; i++) {
    //orr  starDance(60);
  }
}

void loop() {
  Tle493dMagnetic3DSensor.updateData();
  // Serial.print(Tle493dMagnetic3DSensor.getNorm());
  //Serial.print(" ; ");
  //  Serial.print(Tle493dMagnetic3DSensor.getAzimuth());
  //  Serial.print(" ; ");
  //  Serial.println(Tle493dMagnetic3DSensor.getPolar());
  //  delay(100);

  float angle = Tle493dMagnetic3DSensor.getAzimuth() * 57.3;
  int angle1 = angle;
  float Norm = Tle493dMagnetic3DSensor.getNorm();

  if (Norm > 60) {
    Serial.print("9"); // indicates pressing
    starDance(1);
  }

  if (angle1 < 30 && angle1 > -30) {
    //12 oclock pin
    turnoffallexcept(5);
    Serial.println(pin_number_to_wheelno(5));

    angle1 = (int)Tle493dMagnetic3DSensor.getAzimuth() * 57.3;
  }
  else if (angle1 < 60 && angle1 >= 30) {
    //10 oclock pin
    angle1 = (int)Tle493dMagnetic3DSensor.getAzimuth() * 57.3;

    turnoffallexcept(4);
    Serial.println(pin_number_to_wheelno(4));



  }
  else if (angle1 < 120 && angle1 >= 60) {
    //9 oclock pin
    angle1 = (int)Tle493dMagnetic3DSensor.getAzimuth() * 57.3;

    turnoffallexcept(1);
    Serial.println(pin_number_to_wheelno(1));



  }
  else if (angle1 < 150 && angle1 >= 120) {
    //8 oclock pin
    angle1 = (int)Tle493dMagnetic3DSensor.getAzimuth() * 57.3;

    turnoffallexcept(0);
    Serial.println(pin_number_to_wheelno(0));



  }
  else if (angle1 < -150 || angle1 >= 150) {
    //6 oclock pin
    angle1 = (int)Tle493dMagnetic3DSensor.getAzimuth() * 57.3;

    turnoffallexcept(8);
    Serial.println(pin_number_to_wheelno(8));



  }
  else if (angle1 <= -120 && angle1 > -150) {
    //1 oclock pin
    angle1 = (int)Tle493dMagnetic3DSensor.getAzimuth() * 57.3;

    turnoffallexcept(9);
    Serial.println(pin_number_to_wheelno(9));



  }
  else if (angle1 <= -30 && angle1 > -60) {
    //3 oclock pin
    angle1 = (int)Tle493dMagnetic3DSensor.getAzimuth() * 57.3;

    turnoffallexcept(2);
    Serial.println(pin_number_to_wheelno(2));




  }
  else if (angle1 <= -60 && angle1 > -120) {
    //5 oclock pin
    angle1 = (int)Tle493dMagnetic3DSensor.getAzimuth() * 57.3;

    turnoffallexcept(3);
    Serial.println(pin_number_to_wheelno(3));





  }
  delay(20);
  Serial.flush();

  //Serial.println(angle1);
  //delay(500);

  // analogWrite (8,angle1);
  //}
  // if (Norm>150){
  //      Serial.println("Pressed");

  //}
}

void turnoffallexcept(uint8_t  pinnumber) {
  digitalWrite(0, LOW);
  digitalWrite(1, LOW);
  digitalWrite(2, LOW);
  digitalWrite(3, LOW);
  digitalWrite(4, LOW);
  digitalWrite(5, LOW);
  digitalWrite(8, LOW);
  digitalWrite(9, LOW);
  // Serial.println(pinnumber);
  digitalWrite(pinnumber, HIGH);
}
void starDance(int d_time) {
  turnoffallexcept(0);
  delay(d_time);
  turnoffallexcept(1);
  delay(d_time);
  turnoffallexcept(4);
  delay(d_time);
  turnoffallexcept(5);
  delay(d_time);
  turnoffallexcept(2);
  delay(d_time);
  turnoffallexcept(3);
  delay(d_time);
  turnoffallexcept(9);
  delay(d_time);
  turnoffallexcept(8);
  delay(d_time);
}

int pin_number_to_wheelno(int pin) {
  switch (pin) {
    case 0:
      return 2;
    case 1:
      return 3;
    case 2:
      return 6;
    case 3:
      return 7;
    case 4:
      return 4;
    case 5:
      return 5;
    case 8:
      return 1;
    case 9:
      return 0;

  }
}

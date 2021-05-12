#include <SoftwareSerial.h>
#include <TinyGPS.h>
#include <DHT.h>

SoftwareSerial BTSerial = SoftwareSerial(8, 10); // TX, RX, VCC 3.3

TinyGPS tgps;
SoftwareSerial gps = SoftwareSerial (2, 3); // TX, RX

DHT thermo (4, DHT21); // TX yellow, GND black, VCC red

void setup() {

  Serial.begin(9600);

//  BLE
  BTSerial.begin(9600);

//  GPS
  gps.begin(9600);

//  Thermo
  thermo.begin();
}

void loop() {

  char gps_result[50] = "";
  char thermo_result[50] = "";

//  GPS
  bool newData = false;
  unsigned long chars;
  unsigned short sentences, failed;

  gps.listen();
  for (unsigned long start = millis(); millis() - start < 1000;)
  {
    while (gps.available())
    {
      char c = gps.read();
      if (tgps.encode(c)) // Did a new valid sentence come in?
        newData = true;
    }
  }
  
  if (newData)
  {
    float flat, flon;
    char temp[20];
    unsigned long age;
    tgps.f_get_position(&flat, &flon, &age);

    strcat(gps_result, "GPS ");

    // Check if any reads invalid value and exit early (to try again).
    if (flat == TinyGPS::GPS_INVALID_F_ANGLE || flon == TinyGPS::GPS_INVALID_F_ANGLE) {
      return;
    }
    dtostrf(flat, 3, 9, temp);
    strcat(gps_result, temp);

    strcat(gps_result, "/");

    dtostrf(flon, 3, 9, temp);
    strcat(gps_result, temp);
  }
  
//  Thermo
  float h = thermo.readHumidity();
  float t = thermo.readTemperature(); // Read temperature as Celsius (the default)
//  float f = thermo.readTemperature(true); // Read temperature as Fahrenheit (isFahrenheit = true)
  
  // Check if any reads failed and exit early (to try again).
  if (isnan(h) || isnan(t)) {
    return;
  }
  
//  float hif = thermo.computeHeatIndex(f, h); // Compute heat index in Fahrenheit (the default)
  // Compute heat index in Celsius (isFahreheit = false)
  float hic = thermo.computeHeatIndex(t, h, false);

  char tempt[15];

  strcat(thermo_result, "Thermo ");

  dtostrf(h, 2, 1, tempt);
  strcat(thermo_result, tempt);

  strcat(thermo_result, "/");

  dtostrf(t, 2, 1, tempt);
  strcat(thermo_result, tempt);
  
  BTSerial.println(gps_result);
  BTSerial.println(thermo_result);
  Serial.println(gps_result);
  Serial.println(thermo_result);
}

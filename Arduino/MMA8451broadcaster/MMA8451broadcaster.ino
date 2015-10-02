/**************************************************************************/
/*!
    @file     Adafruit_MMA8451.h
    @author   K. Townsend (Adafruit Industries)
    @license  BSD (see license.txt)

    This is an example for the Adafruit MMA8451 Accel breakout board
    ----> https://www.adafruit.com/products/2019

    Adafruit invests time and resources providing this open source code,
    please support Adafruit and open-source hardware by purchasing
    products from Adafruit!

    @section  HISTORY

    v1.0  - First release
*/
/**************************************************************************/

#include <Wire.h>
#include <Adafruit_MMA8451.h>
#include <Adafruit_Sensor.h>

// Packet definitions
const char PACKET_START = '{';
const char PACKET_END   = '}';
const char PACKET_DELIM = '\t';

const char PACKET_TYPE_ACCEL_G = 'G';

const int BAUD_RATE = 19200;

// Delay between samples
const int SAMPLE_PERIOD_US = 50; // us


// Accelerometer object
Adafruit_MMA8451 mma = Adafruit_MMA8451();

void setup(void) {
  Serial.begin(BAUD_RATE);
  
  Serial.println("Adafruit MMA8451 test!");
  

  if (! mma.begin()) {
    Serial.println("Couldnt start");
    while (1);
  }
  Serial.println("MMA8451 found!");
  
  mma.setRange(MMA8451_RANGE_2_G);
  
  Serial.print("Range = "); Serial.print(2 << mma.getRange());  
  Serial.println("G");
  
}

void loop() {
  // Read the 'raw' data in 14-bit counts
  mma.read();
  unsigned long currTime = millis();
  
  // Send packet
//  Serial.print(PACKET_START); Serial.print(PACKET_TYPE_ACCEL_G); Serial.print(PACKET_DELIM); Serial.print(mma.x_g); Serial.print(PACKET_DELIM); Serial.print(mma.y_g); Serial.print(PACKET_DELIM); Serial.print(mma.z_g); Serial.println(PACKET_END);
  Serial.print(PACKET_START); /*Serial.print(PACKET_TYPE_ACCEL_G);*/ Serial.write((byte*) &currTime, sizeof(unsigned long)); Serial.write((byte*) &mma.x_g, sizeof(float)); Serial.write((byte*) &mma.y_g, sizeof(float)); Serial.write((byte*) &mma.z_g, sizeof(float));

  // Delay to not flood Android device
  delayMicroseconds(SAMPLE_PERIOD_US);
}

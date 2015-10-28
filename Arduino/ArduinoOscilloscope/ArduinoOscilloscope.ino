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

// Packet definitions
const char PACKET_START = '{';
const char PACKET_END   = '}';
const char PACKET_DELIM = '\t';

const char PACKET_TYPE_ACCEL_G = 'G';
const char PACKET_TYPE_OSCOPE = 'O';

const int CH_A_PIN = 0;
const int CH_B_PIN = 1;
const int CH_C_PIN = 2;
const int ADC_RANGE = 1024 /* ADC range */ / 5 /* Volt range */;

const unsigned int BAUD_RATE = 57600;

// Delay between samples
const int SAMPLE_PERIOD_US = 0; // us


void setup(void) {
  Serial.begin(BAUD_RATE);
}

void loop() {
  // Read the 'raw' data in 14-bit counts
//  mma.read();

    unsigned long currTime = millis();
    float chA = (float) analogRead(CH_A_PIN) / ADC_RANGE;
    float chB = (float) analogRead(CH_B_PIN) / ADC_RANGE;
    float chC = (float) analogRead(CH_C_PIN) / ADC_RANGE;

  
  // Send packet
  Serial.print(PACKET_START); Serial.write((byte*) &currTime, sizeof(unsigned long)); Serial.write((byte*) &chA, sizeof(float)); Serial.write((byte*) &chB, sizeof(float)); Serial.write((byte*) &chC, sizeof(float));

  // Delay to not flood Android device
  delayMicroseconds(SAMPLE_PERIOD_US);
  
}

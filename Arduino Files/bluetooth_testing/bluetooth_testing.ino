// Basic Bluetooth sketch HC-05_02_9600+ECHO
// Connect the HC-05 module and communicate using the serial monitor
//
// The HC-05 defaults to communication mode when first powered on.
// The default baud rate for communication mode is 9600. 
//Your module may have a different speed.
//
 
#include <SoftwareSerial.h>
SoftwareSerial BTserial(2, 3); // RX | TX
// Connect the HC-05 TX to Arduino pin 2 RX. 
// Connect the HC-05 RX to Arduino pin 3 TX through a voltage divider.
 
char c = ' ';
int FSR_Pin0 = A0; 
int FSRReading0;
String btSerialOut = "";

void setup() 
{
    Serial.begin(9600);
    Serial.println("Arduino is ready");
 
    // HC-05 default serial speed for communication mode is 9600
    BTserial.begin(9600);  
    Serial.println("BTserial started at 9600");
}
 
void loop()
{
    FSRReading0 = analogRead(FSR_Pin0);
    
    if(FSRReading0 < 10) { // add leading zeros
      btSerialOut = String("000" + String(FSRReading0));
    } else if(FSRReading0 < 100) {
      btSerialOut = String("00" + String(FSRReading0));
    } else if(FSRReading0 < 1000) {
      btSerialOut = String("0" + String(FSRReading0));
    } else { btSerialOut = String(FSRReading0); }

    Serial.println("FSR0:");
    Serial.println(btSerialOut);
    
    BTserial.println(btSerialOut);
    if(BTserial.available()){
      Serial.write(BTserial.read());
    }
//    // Keep reading from HC-05 and send to Arduino Serial Monitor
//    if (BTserial.available())
//    {  
//        c = BTserial.read();
//    }
//
    delay(100);
}


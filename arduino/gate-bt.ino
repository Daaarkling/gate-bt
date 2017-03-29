#include <SoftwareSerial.h>

// Security
const String SECURE_KEY = "mpc";
const char SECURE_SPLITTER = ':';

// Commands for controling gate
const String CMD_OPEN = "1";
const String CMD_CLOSE = "0";

// GATE_OPEN & GATE_CLOSE pin 4 and 5
const int GATE_OPEN = 4;
const int GATE_CLOSE = 5;
 
// Create serial port pin 2 and 3 under name Bluetooth
SoftwareSerial Bluetooth =  SoftwareSerial(2, 3);


void setup() {

  // Set pins to output
  pinMode(GATE_OPEN, OUTPUT);
  pinMode(GATE_CLOSE, OUTPUT);

  // Turn off
  digitalWrite(GATE_OPEN, HIGH);  
  digitalWrite(GATE_CLOSE, HIGH);  
  
  // Initialize serial port to baud rate 9600
  Bluetooth.begin(9600);
  
  // let's wait
  delay(500);
}
 
void loop() {
  if (Bluetooth.available()) {
    
    // Read input string sent by connected device
    String input = Bluetooth.readString();

    // Split input into 2 parts
    String key = split(input, SECURE_SPLITTER, 0);
    String operation = split(input, SECURE_SPLITTER, 1);

    // First part is secure key 
    if (key != SECURE_KEY) {
      Bluetooth.println("Error: secure key does not match!");
      return;
    }
    
    if (operation == CMD_OPEN) {
        
      digitalWrite(GATE_CLOSE, HIGH);
      digitalWrite(GATE_OPEN, LOW);
      delay(2000);
      digitalWrite(GATE_OPEN, HIGH);
      
      // Send back the answer
      Bluetooth.print("OK ");
      Bluetooth.println(CMD_OPEN);
    }  
   
    if (operation == CMD_CLOSE) {  
      
      digitalWrite(GATE_OPEN, HIGH);
      digitalWrite(GATE_CLOSE, LOW);
      delay(2000);
      digitalWrite(GATE_CLOSE, HIGH);
      
      Bluetooth.print("OK ");
      Bluetooth.println(CMD_CLOSE);
    }
  }
 
}

String split(String data, char separator, int index){
    int found = 0;
    int strIndex[] = { 0, -1 };
    int maxIndex = data.length() - 1;

    for (int i = 0; i <= maxIndex && found <= index; i++) {
        if (data.charAt(i) == separator || i == maxIndex) {
            found++;
            strIndex[0] = strIndex[1] + 1;
            strIndex[1] = (i == maxIndex) ? i+1 : i;
        }
    }
    return found > index ? data.substring(strIndex[0], strIndex[1]) : "";
}

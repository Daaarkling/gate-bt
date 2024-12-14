#include <SoftwareSerial.h>
#include <EEPROM.h>

/*
 * Gate BT
 * -------
 * 
 * App for manipulating electric gate via ble (Bluetooth Low Energy)
 * 
 * Commands:
 *  m:secure_key:1                   -> open gate
 *  m:secure_key:0                   -> close gate
 *  c:master_key:new_secure_key      -> change secure key
 *  
 * Answers:
 *  ok:m:1                           -> open gate ok
 *  ok:m:0                           -> close gate ok
 *  ok:c:secure_key                  -> secure key was change
 *  err:m:value                      -> unknown value of motion command
 *  err:secure                       -> wrong secure key
 *  err:master                       -> wrong master key
 *  err:length                       -> length of secure key is equal to 0 or greater than 10
 */

// Security
const int MAX_SECURE_KEY_SIZE = 10;
const String MASTER_KEY = "12345";
String SECURE_KEY = "0000";

// Commands for controling gate
const String CMD_MOTION = "m";
const String CMD_CHANGE = "c";
const String VALUE_OPEN = "1";
const String VALUE_CLOSE = "0";

// GATE_OPEN & GATE_CLOSE pin 4 and 5
const int GATE_OPEN = 4;
const int GATE_CLOSE = 5;

// Create serial port pin 2 and 3 under name Bluetooth
SoftwareSerial Bluetooth =  SoftwareSerial(8, 9);

void setup() {

  // Set pins to output
  pinMode(GATE_OPEN, OUTPUT);
  pinMode(GATE_CLOSE, OUTPUT);

  // Turn off relay (reverse logic)
  digitalWrite(GATE_OPEN, HIGH);  
  digitalWrite(GATE_CLOSE, HIGH);  

  pinMode(LED_BUILTIN_RX, OUTPUT);
  pinMode(LED_BUILTIN_TX, OUTPUT);
  digitalWrite(LED_BUILTIN_RX, HIGH);  
  digitalWrite(LED_BUILTIN_TX, HIGH);  
  
  // Initialize serial port to baud rate 9600
  Bluetooth.begin(9600);

  // Load/store SECURE_KEY from EEPROM
  loadSecureKey();
}
 
void loop() {
  if (Bluetooth.available()) {
    // Read input string sent by connected device
    String input = Bluetooth.readString();

    // Split input into 3 parts
    String cmd = split(input, ':', 0);
    String key = split(input, ':', 1);
    String value = split(input, ':', 2);

    // If cmd part is 'c' -> change secure key
    if(cmd == CMD_CHANGE) {
      if(key == MASTER_KEY) {
        
        boolean result = changeSecureKey(value);
        if(result){
          Bluetooth.print("ok:c:" + value);
        } else {
          Bluetooth.print("err:length");
        }
      } else {
        Bluetooth.print("err:master");
      }
    }
    
    // If cmd part is 'm' -> open/close gate
    if(cmd == CMD_MOTION) {
      if (key != SECURE_KEY) {
        Bluetooth.print("err:secure");

        return;
      }
      
      if (value == VALUE_OPEN) {
        openGate(2000);
        Bluetooth.print("ok:m:" + value);
      } else if (value == VALUE_CLOSE) {  
        closeGate(2000);
        Bluetooth.print("ok:m:" + value);
      } else {
        Bluetooth.print("err:m:" + value);
      }
    }
  }
}

void loadSecureKey() {
  // Read form memory
  String secureKey = readStringFromEEPROM(0);

  // If nothing found, store default value into memory
  if(secureKey == "" || secureKey.length() + 1 > MAX_SECURE_KEY_SIZE) {
    changeSecureKey(SECURE_KEY);
  } else {
    SECURE_KEY = secureKey;
  }
}

boolean changeSecureKey(String key) {
  if(key.length() == 0 || key.length() + 1 > MAX_SECURE_KEY_SIZE) {
    return false;
  }

  writeStringToEEPROM(0, key);
  
  return true;
}

void openGate(int delayTime) {
  digitalWrite(GATE_CLOSE, HIGH);
  digitalWrite(GATE_OPEN, LOW);
  delay(delayTime);
  digitalWrite(GATE_OPEN, HIGH);
}

void closeGate(int delayTime) {
  digitalWrite(GATE_OPEN, HIGH);
  digitalWrite(GATE_CLOSE, LOW);
  delay(delayTime);
  digitalWrite(GATE_CLOSE, HIGH);
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

void writeStringToEEPROM(int addrOffset, const String &strToWrite){
  byte len = strToWrite.length();
  EEPROM.write(addrOffset, len);
  for (int i = 0; i < len; i++)
  {
    EEPROM.write(addrOffset + 1 + i, strToWrite[i]);
  }
}

String readStringFromEEPROM(int addrOffset){
  int newStrLen = EEPROM.read(addrOffset);
  char data[newStrLen + 1];
  for (int i = 0; i < newStrLen; i++)
  {
    data[i] = EEPROM.read(addrOffset + 1 + i);
  }
  data[newStrLen] = '\0';

  return String(data);
}


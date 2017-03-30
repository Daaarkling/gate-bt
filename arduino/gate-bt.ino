#include <SoftwareSerial.h>
#include <EEPROM.h>

// Absolute min and max eeprom addresses. Actual values are hardware-dependent.
// These values can be changed e.g. to protect eeprom cells outside this range.
const int EEPROM_MIN_ADDR = 0;
const int EEPROM_MAX_ADDR = 11;

// Security
const int MAX_SECURE_KEY_SIZE = 10;
char buf[MAX_SECURE_KEY_SIZE];
const String MASTER_KEY = "12345";
const char SECURE_SPLITTER = ':';
String SECURE_KEY = "mpc";


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

  // Load/store SECURE_KEY from EEPROM
  loadSecureKey();
}
 
void loop() {
  if (Bluetooth.available()) {

    // Read input string sent by connected device
    String input = Bluetooth.readString();

    // Split input into 2 parts
    String key = split(input, SECURE_SPLITTER, 0);
    String operation = split(input, SECURE_SPLITTER, 1);

    // If first part is master key, change secure key
    if(key == MASTER_KEY) {
      boolean result = changeSecureKey(operation);
      if(result){
        Bluetooth.print("reset:" + operation);
      } else {
        Bluetooth.print("error:length");
      }
      return;
    }
    
    // If first part is not master key or secure key, error 
    if (key != SECURE_KEY) {
      Bluetooth.print("error:secure");
      return;
    }
    
    if (operation == CMD_OPEN) {
      openGate(2000);
      Bluetooth.print("ok:" + CMD_OPEN);
    }  
   
    if (operation == CMD_CLOSE) {  
      closeGate(2000);
      Bluetooth.print("ok:" + CMD_CLOSE);
    }
  }
}

void loadSecureKey() {
  
  // Read form memory
  eeprom_read_string(0, buf, MAX_SECURE_KEY_SIZE);

  // If nothing found, store default value into memory
  if(strcmp(buf, "") == 0) {
    changeSecureKey(SECURE_KEY);
  } else {
    SECURE_KEY = String(buf);
  }
}

boolean changeSecureKey(String key) {
  
  char keyChar[MAX_SECURE_KEY_SIZE];

  if(key.length() + 1 > MAX_SECURE_KEY_SIZE) {
    return false;
  }
  
  key.toCharArray(keyChar, key.length() + 1); //convert string to char array
  strcpy(buf, keyChar);
  eeprom_write_string(0, buf);

  SECURE_KEY = key;
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

// Returns true if the address is between the
// minimum and maximum allowed values, false otherwise.
//
// This function is used by the other, higher-level functions
// to prevent bugs and runtime errors due to invalid addresses.
boolean eeprom_is_addr_ok(int addr) {
  return ((addr >= EEPROM_MIN_ADDR) && (addr <= EEPROM_MAX_ADDR));
}

// Writes a sequence of bytes to eeprom starting at the specified address.
// Returns true if the whole array is successfully written.
// Returns false if the start or end addresses aren't between
// the minimum and maximum allowed values.
// When returning false, nothing gets written to eeprom.
boolean eeprom_write_bytes(int startAddr, const byte* array, int numBytes) {
  // counter
  int i;
  // both first byte and last byte addresses must fall within
  // the allowed range 
  if (!eeprom_is_addr_ok(startAddr) || !eeprom_is_addr_ok(startAddr + numBytes)) {
    return false;
  }
  for (i = 0; i < numBytes; i++) {
    EEPROM.write(startAddr + i, array[i]);
  }
  return true;
}
  
// Writes a string starting at the specified address.
// Returns true if the whole string is successfully written.
// Returns false if the address of one or more bytes fall outside the allowed range.
// If false is returned, nothing gets written to the eeprom.
boolean eeprom_write_string(int addr, const char* string) {
  
  int numBytes; // actual number of bytes to be written
  //write the string contents plus the string terminator byte (0x00)
  numBytes = strlen(string) + 1;
  return eeprom_write_bytes(addr, (const byte*)string, numBytes);
}

// Reads a string starting from the specified address.
// Returns true if at least one byte (even only the string terminator one) is read.
// Returns false if the start address falls outside the allowed range or declare buffer size is zero.
// 
// The reading might stop for several reasons:
// - no more space in the provided buffer
// - last eeprom address reached
// - string terminator byte (0x00) encountered.
boolean eeprom_read_string(int addr, char* buffer, int bufSize) {
  byte ch; // byte read from eeprom
  int bytesRead; // number of bytes read so far
  if (!eeprom_is_addr_ok(addr)) { // check start address
    return false;
  }
  
  if (bufSize == 0) { // how can we store bytes in an empty buffer ?
    return false;
  }
  // is there is room for the string terminator only, no reason to go further
  if (bufSize == 1) {
    buffer[0] = 0;
    return true;
  }
  bytesRead = 0; // initialize byte counter
  ch = EEPROM.read(addr + bytesRead); // read next byte from eeprom
  buffer[bytesRead] = ch; // store it into the user buffer
  bytesRead++; // increment byte counter
  // stop conditions:
  // - the character just read is the string terminator one (0x00)
  // - we have filled the user buffer
  // - we have reached the last eeprom address
  while ( (ch != 0x00) && (bytesRead < bufSize) && ((addr + bytesRead) <= EEPROM_MAX_ADDR) ) {
    // if no stop condition is met, read the next byte from eeprom
    ch = EEPROM.read(addr + bytesRead);
    buffer[bytesRead] = ch; // store it into the user buffer
    bytesRead++; // increment byte counter
  }
  // make sure the user buffer has a string terminator, (0x00) as its last byte
  if ((ch != 0x00) && (bytesRead >= 1)) {
    buffer[bytesRead - 1] = 0;
  }
  return true;
}

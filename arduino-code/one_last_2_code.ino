#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLEAdvertising.h>

// This UUID should match what your app might be looking for, but the app is currently looking for any iBeacon.
static BLEUUID beaconUUID("12345678-1234-1234-1234-1234567890ab");

void setup() {
    Serial.begin(115200);

    BLEDevice::init("CampusQuiet_Beacon");
    BLEServer *pServer = BLEDevice::createServer();
    BLEAdvertising *pAdvertising = pServer->getAdvertising();

    // Create an advertisement data object for the metadata
    BLEAdvertisementData advertisementData;
    advertisementData.setFlags(0x06); // General Discoverable & BR/EDR Not Supported

    // This character array will hold our complete iBeacon packet
    // It is structured as follows:
    // - 2 bytes: Apple's Manufacturer ID (0x4C00)
    // - 2 bytes: iBeacon type (0x0215)
    // - 16 bytes: UUID
    // - 2 bytes: Major
    // - 2 bytes: Minor
    // - 1 byte: TX Power (RSSI at 1 meter)
    char manufacturer_data[27] = {0};

    // Apple's Manufacturer ID
    manufacturer_data[0] = 0x4C;
    manufacturer_data[1] = 0x00;

    // iBeacon type
    manufacturer_data[2] = 0x02;
    manufacturer_data[3] = 0x15;

    // Copy the UUID bytes into the packet
    memcpy(&manufacturer_data[4], beaconUUID.getNative()->uuid.uuid128, 16);

    // Major value (Set to 1)
    manufacturer_data[20] = 0x00;
    manufacturer_data[21] = 0x01;

    // Minor value (Set to 1)
    manufacturer_data[22] = 0x00;
    manufacturer_data[23] = 0x01;

    // TX Power (Calibrated to -59 dBm)
    manufacturer_data[24] = 0xC5;

    // Add the complete packet to the manufacturer data field.
    // The correct payload length is 25 bytes.
    advertisementData.setManufacturerData(String(manufacturer_data, 25));

    // Set the advertisement data
    pAdvertising->setAdvertisementData(advertisementData);

    // Start advertising
    pAdvertising->start();

    Serial.println("✅ iBeacon advertising started...");
}

void loop() {
    // The BLE stack handles advertising in the background, so the loop can be empty.
    delay(1000);
}
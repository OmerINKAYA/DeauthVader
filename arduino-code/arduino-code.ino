#include <SoftwareSerial.h>
#include <TinyGPS++.h>
#include <NewPing.h>
#include <Servo.h>

// --- Motor pinleri ---
#define ENA 5
#define ENB 6
#define IN1 8
#define IN2 9
#define IN3 10
#define IN4 11

// --- Değişken Gecikme Süreleri (Varsayılan Değerler) ---
uint32_t ileriDelay = 300;   // Varsayılan ileri gitme süresi (ms)
uint32_t sagaDonDelay = 400; // Varsayılan sağa dönme süresi (ms)
uint32_t solaDonDelay = 400; // Varsayılan sola dönme süresi (ms)

// --- GPS/Bluetooth ---
static const int RXPin = A3;
static const int TXPin = A2;
static const uint32_t GPSBaud = 9600;
TinyGPSPlus gps;
SoftwareSerial gpsSerial(RXPin, TXPin);

// --- Fonksiyon prototipleri ---
void ileriGit();
void geriGit();
void solaDon();
void sagaDon();
void dur();
void kalibreliHizVer(int hiz);
void handleSettingCommand(); // Ayar komutlarını işleyen fonksiyon
bool checkObstacles();       // Engel kontrolü yapan fonksiyon
void processMovementCommand(char command); // Hareket komutlarını işleyen yeni fonksiyon

// --- Sensör pinleri ---
#define SAG_IR_PIN 4 // Sağ ön IR sensör çıkışı
#define SOL_IR_PIN 7 // Sol ön IR sensör çıkışı
#define TRIGGER_PIN A0
#define ECHO_PIN A1
//#define SERVO_PIN 3
#define MAX_DISTANCE 100
NewPing sonar(TRIGGER_PIN, ECHO_PIN, MAX_DISTANCE);

// --- Servo ---
/*Servo myservo;
int pos = 0;*/
bool otonomMod = false;  // ⚠️ Default: Manuel mod

// --- Robot Durumları (State Machine) ---
enum RobotState {
    STATE_IDLE,             // Boşta, komut bekliyor
    STATE_FORWARD,          // İleri gidiyor (süreli)
    STATE_BACKWARD,         // Geri gidiyor (süresiz)
    STATE_TURN_LEFT,        // Sola dönüyor (süreli)
    STATE_TURN_RIGHT,       // Sağa dönüyor (süreli)
    STATE_OBSTACLE_STOP     // Engel algılandı, durduruldu
};

RobotState currentState = STATE_IDLE; // Başlangıç durumu
unsigned long movementStartTime = 0; // Süreli hareketin başlangıç zamanı (millis())
bool isStoppedByObstacle = false; // Engel nedeniyle durdurulup durdurulmadığını izler

void setup() {
    Serial.begin(9600);
    gpsSerial.begin(GPSBaud);

    // Motorlar
    pinMode(ENA, OUTPUT); pinMode(ENB, OUTPUT);
    pinMode(IN1, OUTPUT); pinMode(IN2, OUTPUT);
    pinMode(IN3, OUTPUT); pinMode(IN4, OUTPUT);
    dur();

    // IR Sensör pinleri
    pinMode(SAG_IR_PIN, INPUT); // IR sensörleri INPUT olarak ayarla (Dahili pull-up kullanmıyorsanız external pull-up gerekebilir)
    pinMode(SOL_IR_PIN, INPUT);

    // Servo başlat
    /*myservo.attach(SERVO_PIN);
    for (pos = 90; pos <= 180; pos++) { myservo.write(pos); delay(10); }
    for (pos = 180; pos >= 0; pos--) { myservo.write(pos); delay(10); }
    for (pos = 0; pos <= 90; pos++) { myservo.write(pos); delay(10); }*/

    Serial.println(F("🔧 Sistem Başlatıldı."));
    Serial.println(F("🔧 [DEBUG] Varsayılan Gecikmeler:"));
    Serial.print(F("   İleri: ")); Serial.print(ileriDelay); Serial.println(F(" ms"));
    Serial.print(F("   Sağa Dön: ")); Serial.print(sagaDonDelay); Serial.println(F(" ms"));
    Serial.print(F("   Sola Dön: ")); Serial.print(solaDonDelay); Serial.println(F(" ms"));
}

void loop() {
    // --- A) GPS'den veri oku ve işle ---
    while (gpsSerial.available() > 0) {
        gps.encode(gpsSerial.read());
    }

    if (gps.location.isUpdated()) { // Bu kontrolü daha seyrek yapmak için bir zamanlayıcı eklenebilir
        Serial.print(F("Latitude: "));
        Serial.println(gps.location.lat(), 6);
        Serial.print(F("Longitude: "));
        Serial.println(gps.location.lng(), 6);
        Serial.print(F("Satellites: "));
        Serial.println(gps.satellites.value());

        // Diğer GPS bilgileri (uydu, tarih, saat) buraya eklenebilir.
      if (gps.date.isValid()) {
        char dateStr[11];
        sprintf(dateStr, "%02d/%02d/%02d", gps.date.day(), gps.date.month(), gps.date.year() % 100);
        Serial.print(F("Date: ")); 
        Serial.println(dateStr);
      }

      if (gps.time.isValid()) {
        char timeStr[9];
        sprintf(timeStr, "%02d:%02d:%02d", gps.time.hour(), gps.time.minute(), gps.time.second());
        Serial.print(F("Time: ")); 
        Serial.println(timeStr);
      }
    }

    if(Serial.available() > 0) {
        char gelen = Serial.peek();

        if (gelen == '#') {
            Serial.read(); // '#' karakterini buffer'dan oku ve atla
            handleSettingCommand(); // Ayar komutunu işle (Engel olsa bile ayar komutu işlenebilir, ancak engel yokken buradayız)
        }else if (gelen == 'A' || gelen == 'a') {
            Serial.read();
            otonomMod = true;
            Serial.println("[MODE] Otonom mod AKTİF");
        } else if (gelen == 'M' || gelen == 'm') {
            Serial.read();
            otonomMod = false;
            dur();
            Serial.println("[MODE] Manuel mod AKTİF");
        }

        // --- B) Engel kontrolü ---
        // Her loop iterasyonunda engel kontrolü yapılır, durum ne olursa olsun.
        else if (checkObstacles()) {
            if (currentState != STATE_OBSTACLE_STOP) { // Eğer henüz engel durumuna geçmediyse
                Serial.println(F("[ENGEL] Engel algılandı! Robot durduruluyor."));
                dur(); // Motorları durdur
                currentState = STATE_OBSTACLE_STOP; // Durumu güncelle
                isStoppedByObstacle = true; // Bayrağı set et
                // Engel durumuna girildiğinde buffer'daki hareket komutlarını temizle
                unsigned long clearStart = millis();
                while(Serial.available() > 0 && (Serial.peek() != '\n') && (millis() - clearStart < 50) ) Serial.read();
                if(Serial.available() > 0 && Serial.peek() == '\n') Serial.read(); // '\n' varsa onu da temizle

            }
            // Engel varken robot STATE_OBSTACLE_STOP durumunda kalır ve aşağıdaki komut işleme bloğunda hareket komutları yoksayılır.

        } else { // Engel YOK
            if (currentState == STATE_OBSTACLE_STOP) { // Engel durumu yeni bittiyse
                Serial.println(F("[ENGEL] Engel kalktı. Yeni komut bekleniyor."));
                currentState = STATE_IDLE; // Durumu boşa al, yeni komut bekle
                isStoppedByObstacle = false; // Bayrağı sıfırla
            }

            // Engel yokken veya engel kalktıktan sonra: Bluetooth'tan (Serial) komutları işle
            if (Serial.available() > 0) {
                char peekChar = Serial.peek(); // İlk karaktere bak
                // Hareket komutları ('F', 'B', 'L', 'R', 'S') sadece STATE_IDLE durumundayken veya sadece 'S' komutu işlenir
                if (currentState == STATE_IDLE && peekChar != '\n' && peekChar != '\r') {
                    char gelen = Serial.read(); // Komutu oku
                    Serial.print(F("[DEBUG] BT hareket komutu alındı (IDLE durumunda): ")); Serial.println(gelen);
                    processMovementCommand(gelen); // Hareket komutunu işle

                } else if (peekChar != '\n' && peekChar != '\r') {
                    // IDLE durumunda değilken (hareket ediyor veya engel kalkmasını bekliyor) gelen hareket komutlarını yoksay
                    char gelen = Serial.read(); // Komutu oku
                    Serial.print(F("[UYARI] Robot '"));
                    Serial.print(gelen);
                    Serial.print(F("' komutu alındı ama robot IDLE veya OBSTACLE_STOP durumunda değil (şu anki durum: "));
                    Serial.print(currentState); // Durum numarasını bas
                    Serial.println(F("). Komut yoksayılıyor."));
                    // Komut sonrası buffer'ı temizle
                    unsigned long clearStart = millis();
                    while(Serial.available() > 0 && (Serial.peek() != '\n') && (millis() - clearStart < 50) ) Serial.read();
                    if(Serial.available() > 0 && Serial.peek() == '\n') Serial.read(); // '\n' varsa onu da temizle
                }
                else {
                    // Eğer peekChar '\n' veya '\r' ise, sadece buffer'dan oku ve atla.
                    Serial.read();
                    // Serial.println(F("[DEBUG] Satır sonu karakteri atlandı.")); // İsteğe bağlı debug mesajı
                }
            }
        }
    }


    // --- C) Durum Makinesi - Mevcut Duruma Göre Hareket Et ---
    unsigned long currentTime = millis();
    switch (currentState) {
        case STATE_FORWARD:
            // Belirtilen süre geçtiyse dur
            if (currentTime - movementStartTime >= ileriDelay) {
                Serial.println(F("[DEBUG] İleri hareket süresi doldu. Duruluyor."));
                dur();
                currentState = STATE_IDLE;
            }
            // Engel kontrolü loop'un başında yapılıyor, burada tekrar gerek yok.
            break;

        case STATE_BACKWARD:
            // Geri giderken engel kontrolü loop'un başında yapılır.
            // 'S' komutu veya engel durumu bu durumu sonlandırır.
            break;

        case STATE_TURN_LEFT:
            // Belirtilen süre geçtiyse dur
            if (currentTime - movementStartTime >= solaDonDelay) {
                Serial.println(F("[DEBUG] Sola dönüş süresi doldu. Duruluyor."));
                dur();
                currentState = STATE_IDLE;
            }
             // Engel kontrolü loop'un başında yapılıyor.
            break;

        case STATE_TURN_RIGHT:
            // Belirtilen süre geçtiyse dur
            if (currentTime - movementStartTime >= sagaDonDelay) {
                Serial.println(F("[DEBUG] Sağa dönüş süresi doldu. Duruluyor."));
                dur();
                currentState = STATE_IDLE;
            }
             // Engel kontrolü loop'un başında yapılıyor.
            break;

        case STATE_IDLE:
            // Boşta dururken motorlar zaten durmuş olmalı.
            break;

        case STATE_OBSTACLE_STOP:
             // Engel varken burada bekler. Motorlar durdurulmuş olmalı.
             // Engelin kalkması loop'un başındaki engel kontrolü tarafından algılanır ve durum IDLE'a geçer.
            break;
    }

    // --- Otonom Mod: Engelden Kaçma ---
    if (otonomMod) {
        unsigned int distance = sonar.ping_cm();
        int Right_Value = digitalRead(SAG_IR_PIN);
        int Left_Value = digitalRead(SOL_IR_PIN);

        Serial.print("[AUTO] Distance: "); Serial.println(distance);
        Serial.print("[AUTO] RIGHT: "); Serial.println(Right_Value);
        Serial.print("[AUTO] LEFT : "); Serial.println(Left_Value);

        if ((Right_Value == 1) && (distance >= 10 && distance <= 35) && (Left_Value == 1)) {
            ileriGit(80);
        } else if ((Right_Value == 1) && (distance > 35 && distance <= 60) && (Left_Value == 1)) {
            ileriGit(100);
        } else if ((Right_Value == 1) && (Left_Value == 0)) {
            solaDon(100);
        } else if ((Right_Value == 0) && (Left_Value == 1)) {
            sagaDon(100);
        } else if ((Right_Value == 1) && (Left_Value == 1)) {
            dur();
        } else if (distance >= 0 && distance < 15) {
            dur();
        }
    }

}


// *** YENİ FONKSİYON: Gelen hareket komutunu işler ve durumu ayarlar ***
void processMovementCommand(char command) {
     // Engel olup olmadığını zaten loop'un başında kontrol ettik ve duruma göre buraya geldik.
     // Bu fonksiyon sadece STATE_IDLE iken çağrılmalı.
     if (currentState != STATE_IDLE) {
         Serial.println(F("[HATA] processMovementCommand fonksiyonu IDLE durumunda değilken çağrıldı!"));
         return; // Hata durumu, işlem yapma
     }

     switch (command) {
         case 'F': case 'f':
             Serial.println(F("[DEBUG] Komut: İleri (Süreli) - Başlatılıyor"));
             ileriGit(100);
             movementStartTime = millis(); // Zamanlayıcıyı başlat
             currentState = STATE_FORWARD; // Durumu ileri olarak ayarla
             break;
         case 'B': case 'b':
             Serial.println(F("[DEBUG] Komut: Geri (Süresiz) - Başlatılıyor"));
             geriGit(100);
             currentState = STATE_BACKWARD; // Durumu geri olarak ayarla
             // Süresiz olduğu için zamanlayıcı başlatmaya gerek yok
             break;
         case 'L': case 'l':
             Serial.println(F("[DEBUG] Komut: Sola Dön (Süreli) - Başlatılıyor"));
             solaDon(100);
             movementStartTime = millis(); // Zamanlayıcıyı başlat
             currentState = STATE_TURN_LEFT; // Durumu sola dön olarak ayarla
             break;
         case 'R': case 'r':
             Serial.println(F("[DEBUG] Komut: Sağa Dön (Süreli) - Başlatılıyor"));
             sagaDon(100);
             movementStartTime = millis(); // Zamanlayıcıyı başlat
             currentState = STATE_TURN_RIGHT; // Durumu sağa dön olarak ayarla
             break;
         case 'S': case 's':
             // 'S' komutu artık main loop'ta doğrudan işleniyor. Buraya gelmemeli.
             // Ancak defensive programming için burada da durdurabiliriz.
             Serial.println(F("[DEBUG] Komut: Dur - İşleniyor (processMovementCommand)"));
             dur();
             currentState = STATE_IDLE;
             isStoppedByObstacle = false;
             break;
         case 'H': case 'h':
            ileriGit(100);
            break;
         case 'J': case 'j':
            geriGit(100);
            break;
         case 'K': case 'k':
            solaDon(100);
            break;
         case 'N': case 'n':
            sagaDon(100);
            break;
         default:
             Serial.print(F("[DEBUG] Tanımsız hareket komutu: '")); Serial.print(command); Serial.println(F("'"));
             // Tanımsız komutta durdurmak istenirse:
             // dur();
             // currentState = STATE_IDLE;
             // isStoppedByObstacle = false;
             break;
     }
}


// *** Engel kontrolü yapar ***
// Sensör LOW okuyorsa engel var demektir (modülünüze göre HIGH olabilir)
bool checkObstacles() {
    // Dahili pull-up kullanmıyorsanız, IR sensör pinlerine harici pull-up dirençleri bağlayın
    // veya pinMode(PIN, INPUT_PULLUP); kullanın.
    // Varsayılan IR modülleri genellikle engel varken LOW verir.
    bool sagEngel = digitalRead(SAG_IR_PIN) == LOW; // Sağ sensör LOW mu?
    bool solEngel = digitalRead(SOL_IR_PIN) == LOW; // Sol sensör LOW mu?

    // Debug için sensör durumlarını görmek isterseniz yorum satırını kaldırın
    // static unsigned long lastDebug = 0;
    // if (millis() - lastDebug > 100) { // Çok sık basmamak için
    //    Serial.print(F("[DEBUG] IR Kontrol: Sağ=")); Serial.print(digitalRead(SAG_IR_PIN)); // Raw değer
    //    Serial.print(F(", Sol=")); Serial.print(digitalRead(SOL_IR_PIN));   // Raw değer
    //    Serial.print(F(" | Engel Algılandı: ")); Serial.println(sagEngel || solEngel);
    //    lastDebug = millis();
    // }

    return sagEngel || solEngel; // Herhangi biri engel algılarsa true dön
}


// *** Ayar komutlarını işler (Bu fonksiyon delay kullanmadığı için millis'e çevirmeye gerek yok) ***
void handleSettingCommand() {
    // '#' karakteri zaten ana loop'ta okundu. Sırada Tip (I,R,L) var.
    unsigned long startTime = millis();
    while (Serial.available() == 0 && millis() - startTime < 100); // kısa bekleme
    if (Serial.available() > 0) {
        char identifier = Serial.read();
        long newValue = Serial.parseInt(); // Sayısal değeri oku. '\n' buffer'da kalır.

        Serial.print(F("[DEBUG] Ayar Komutu: Tip='")); Serial.print(identifier);
        Serial.print(F("', Okunan Ham Değer=")); Serial.println(newValue);

        // Değerin geçerli olup olmadığını kontrol et (pozitif ve makul bir aralıkta)
        if (newValue > 0 && newValue <= 30000) { // Örn: max 30 saniye delay
            uint32_t newDelayVal = (uint32_t)newValue;
            bool updated = false;
            switch (identifier) {
                case 'I': case 'i':
                    ileriDelay = newDelayVal;
                    Serial.print(F("  -> Yeni ileriDelay: ")); Serial.println(ileriDelay);
                    updated = true;
                    break;
                case 'R': case 'r':
                    sagaDonDelay = newDelayVal;
                    Serial.print(F("  -> Yeni sagaDonDelay: ")); Serial.println(sagaDonDelay);
                    updated = true;
                    break;
                case 'L': case 'l':
                    solaDonDelay = newDelayVal;
                    Serial.print(F("  -> Yeni solaDonDelay: ")); Serial.println(solaDonDelay);
                    updated = true;
                    break;
                default:
                    Serial.print(F("[UYARI] Tanımsız ayar tipi: '")); Serial.print(identifier);
                    Serial.println(F("'. Değer atanmadı."));
                    break;
            }
            // if (updated) { Serial.println(F("   Ayar başarıyla güncellendi.")); }
        } else {
            if (newValue <= 0) {
                Serial.println(F("[UYARI] Ayar için geçersiz (<=0) veya parse edilemeyen değer alındı."));
            } else { // newValue > 30000
                Serial.println(F("[UYARI] Ayar değeri çok büyük (max 30000ms)."));
            }
        }

        // *** ÖNEMLİ: BUFFER TEMİZLEME ***
        // Serial.parseInt() sonrasında buffer'da kalan '\n' veya diğer artıkları temizle.
        unsigned long clearBufferStartTime = millis();
        while (Serial.available() > 0 && (millis() - clearBufferStartTime < 50)) { // Kısa bir timeout (50ms)
            char tempChar = Serial.read();
            if (tempChar == '\n') {
                // Serial.println(F("[DEBUG] Ayar komutu sonrası '\\n' temizlendi."));
                break; // '\n' bulundu ve tüketildi, işlem tamam.
            }
        }

    } else {
        Serial.println(F("[HATA] handleSettingCommand: '#' sonrası tip karakteri bekleniyordu, buffer boş."));
        unsigned long clearBufferStartTime = millis();
        while (Serial.available() > 0 && (millis() - clearBufferStartTime < 50)) Serial.read();
    }
}


// --- Motor Fonksiyonları ---
void ileriGit(int hiz) {
    digitalWrite(IN1, LOW);  digitalWrite(IN2, HIGH);
    digitalWrite(IN3, LOW);  digitalWrite(IN4, HIGH);
    kalibreliHizVer(hiz);
}

void geriGit(int hiz) {
    digitalWrite(IN1, HIGH); digitalWrite(IN2, LOW);
    digitalWrite(IN3, HIGH); digitalWrite(IN4, LOW);
    kalibreliHizVer(hiz);
}

void solaDon(int hiz) {
    digitalWrite(IN1, LOW);  digitalWrite(IN2, HIGH);
    digitalWrite(IN3, HIGH); digitalWrite(IN4, LOW);
    kalibreliHizVer(hiz);
}

void sagaDon(int hiz) {
    digitalWrite(IN1, HIGH); digitalWrite(IN2, LOW);
    digitalWrite(IN3, LOW);  digitalWrite(IN4, HIGH);
    kalibreliHizVer(hiz);
}

void dur() {
    kalibreliHizVer(0);
}

// Hız kalibrasyon fonksiyonu (Değişiklik yok)
void kalibreliHizVer(int hiz) {
    int minA = 110; // Sol motor için minimum PWM (deneyerek bulun)
    int minB = 135; // Sağ motor için minimum PWM (deneyerek bulun)
    int maxA = 235;
    int maxB = 255;
    int pwmA, pwmB;

    if (hiz == 0) {
        pwmA = 0;
        pwmB = 0;
    } else {
        pwmA = map(hiz, 0, 100, minA, maxA);
        pwmB = map(hiz, 0, 100, minB, maxB);
    }

    pwmA = constrain(pwmA, 0, 255);
    pwmB = constrain(pwmB, 0, 255);

    analogWrite(ENA, pwmA);
    analogWrite(ENB, pwmB);
}
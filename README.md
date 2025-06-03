# DeauthVader 🚗💥

DeauthVader, çok amaçlı bir Arduino tabanlı akıllı araç projesidir. Bluetooth üzerinden kontrol edilebilen bu araç; GPS takip, çizimle rota belirleme, engelden kaçınma, nesne takip etme ve Wi-Fi deauthentication saldırısı gibi yeteneklerle donatılmıştır.

## 🚀 Özellikler

- 🔄 **Free Ride**: Serbest dolaşım modu
- 🧠 **Obstacle Avoiding**: IR sensör ile engel algıladığında otomatik durma
- 📍 **GPS Tracking**: Harita üzerinde gidilen noktaların çizilmesi (API Key gerekli)
- 🧲 **Follow Me**: Ultrasonik sensör ile öndeki nesneyi takip etme
- 🖐️ **Hand-Drawn Path**: Android uygulamasındaki çizime göre rota takibi
- 📶 **Wi-Fi Deauthentication**: ESP8266 ile WPA2 destekli modemlere karşı deauth saldırısı ([@spacehuhntech/esp8266_deauther](https://github.com/spacehuhntech/esp8266_deauther) kullanıldı)
- 🎮 **Bluetooth Kontrol**: Android uygulaması ile hareket komutları gönderme
- ⚙️ **Kalibrasyon Desteği**: Sağa/sola dönüş süreleri için manuel ms ayarı

## 🔧 Donanım Listesi

- 🧠 Arduino UNO
- 🦿 4× 6V 250rpm DC Motor
- 🔌 L298N Motor Sürücü
- 🔋 2× 3.7V 18650 Pil + Pil Yatağı
- 👁️ 2× IR Sensör
- 🌍 Neo-7M GPS Modülü
- 🧭 HC-SR05 Ultrasonik Sensör
- 📡 ESP8266 NodeMCU (Wi-Fi / Deauth için)
- 📲 HC-06 Bluetooth Modülü

## 👀 Gizli Güç

DeauthVader, masum görünümünün altında bir **modem deauth silahı** taşır. ESP8266 ile geleneksel WPA2 modemlere gizlice saldırabilir. Tabii ki sadece eğitim ve test amaçlı kullanılması tavsiye edilir 😉

## 👥 Contributors

- [@OmerINKAYA](https://github.com/OmerINKAYA)  - Car Assembly, GPS Tracking, Free Ride Mode, Movement Calibration, Obstacle Avoidance
- [@selimhocaoglu](https://github.com/selimhocaoglu)  - Hand-Driven Path Execution, Free Ride Mode, Obstacle Avoidance
- [@EmirKaraaslan](https://github.com/EmirKaraaslan)  - Follow-Me Mode, Android App UI Design 
- [@UygarTatar](https://github.com/UygarTatar)  - Car Assembly, Follow-Me Mode

## 📝 License

This project is licensed under the [MIT License](LICENSE).

---

> Proje ilerleyen zamanda uygulama görüntüleri ve demo videoları ile güncellenecektir.

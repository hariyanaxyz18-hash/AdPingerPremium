# AdPingerMulti — Anonymous Premium Root / Non-Root

Versi ini memakai satu APK/project untuk perangkat **root maupun non-root**.

## Mode
- **Non-root:** seluruh fungsi utama berjalan tanpa root.
- **Root:** aplikasi mendeteksi keberadaan `su`/Magisk/KernelSU dan menampilkan status ROOT DEVICE. Root tidak diwajibkan.
- Tidak ada perintah root yang dijalankan otomatis dan tidak ada modifikasi sistem.

## Premium Anonymous theme
UI memakai tema gelap premium dengan aksen hijau, panel rounded, dan label `ANONYMOUS WEB CONTROL`.

## Auto WebView
Timer 1/5/15/30/60 menit memilih URL dari sumber konfigurasi dan mengirimkannya ke Activity. WebView hanya melakukan auto-open ketika Activity sedang terlihat; Android membatasi pemaksaan Activity ke foreground dari background.

Gunakan hanya URL halaman yang Anda miliki atau berhak menguji. Jangan gunakan mekanisme ini untuk menghasilkan klik, impresi, atau traffic iklan palsu.

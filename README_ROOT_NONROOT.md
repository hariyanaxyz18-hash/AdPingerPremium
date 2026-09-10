# Root / Non-Root

- Non-root: aplikasi tetap berjalan normal. Auto WebView bekerja langsung bila MainActivity sedang aktif.
- Magisk/root: saat Auto WebView dijalankan, aplikasi melakukan permintaan `su -c id`. Magisk akan menampilkan dialog Superuser bila izin belum diberikan.
- Jika akses root disetujui, service memakai perintah `am start` yang tetap (tanpa memasukkan URL remote ke shell) untuk membuka MainActivity dari background. URL dikirim melalui SharedPreferences/broadcast dan kemudian dimuat oleh WebView.
- Aplikasi tidak membutuhkan modul Magisk khusus.

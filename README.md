# AdPinger Premium — Root + Non-Root

Android Studio/GitHub-ready project for WebView testing of websites you own or are authorized to test.

## One URL file only

**`urls.txt` at the repository root is the only URL list you need to edit.**

During every Android build, Gradle automatically copies the root `urls.txt` into the APK as `app/src/main/assets/urls.txt`.

Format: one URL per line. Blank lines and lines beginning with `#` are ignored.

`OWN_URLS.txt` is not used and has been removed.

## Device support

- Non-root: works without root.
- Root: detects common `su`/Magisk/KernelSU environments; root is optional.

## GitHub Actions

The workflow builds a debug APK on pushes to `main` and from **Actions → Build APK → Run workflow**.

## Termux upload

```bash
pkg update -y
pkg install git unzip gh -y
termux-setup-storage
cd ~/storage/downloads
unzip AdPingerMulti-DbzBanten-GITHUB-FINAL.zip -d AdPingerPremium
cd AdPingerPremium
git init
git branch -M main
git add .
git commit -m "Initial Android project"
git remote add origin https://github.com/USERNAME/REPOSITORY.git
git push -u origin main
```

Replace `USERNAME/REPOSITORY` with your repository.

## Updating URLs

Edit only `urls.txt`, then commit and push:

```bash
git add urls.txt
git commit -m "Update URL list"
git push
```

A new GitHub Actions build will include the updated list.

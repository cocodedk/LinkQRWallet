# Link QR Wallet

Link QR Wallet is an Android app that saves your web links on your phone and shows each one as a QR code. Add a link, scan a QR code to save the link inside it, and browse your saved links without an internet connection.

## Download
<!-- cocode-apps:install:start -->
- Coming to F-Droid
- [Download the Android installation file (APK) from GitHub](https://github.com/cocodedk/LinkQRWallet/releases/latest/download/LinkQRWallet.apk)
- [Add the app to Obtainium, an app that keeps it up to date](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/cocodedk/LinkQRWallet)
<!-- cocode-apps:install:end -->

## Features
- Add a link by typing or pasting it, or by sharing it from another app. If you leave the title empty, the app looks up the page title after you save.
- See each link as a QR code and share the QR code as a picture.
- Scan a QR code to save the link inside it.
- Search by title, web address or domain, and sort by title, domain or date added.
- Open or copy a saved link.
- Browse saved links and show their QR codes without an internet connection.

## Privacy
Your saved links stay on your phone. The app has no account, no ads and no tracking. It uses the camera only to scan QR codes, and the internet only to look up page titles. When you save a link and have not typed a title, the app opens that web address once to read the page's title. Nothing is sent to any website before you save. The website can see your phone's IP address. If Android backup is turned on, Android may include your saved links in your own backup. Read the [privacy policy](https://qr.cocode.dk/privacy/).

## Build
```bash
./gradlew assembleDebug
```

## Contributing
Local setup, git hooks, and the build and test commands are in [CONTRIBUTING.md](CONTRIBUTING.md). Bugs and ideas go to the [issues page](https://github.com/cocodedk/LinkQRWallet/issues).

## License
MIT. See [LICENSE](LICENSE).

## About
Made by Babak Bandpey  
bb@cocode.dk  
https://cocode.dk

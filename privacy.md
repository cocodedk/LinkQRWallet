# Privacy Policy — Link QR Wallet

**App:** Link QR Wallet (`com.cocode.linkqrwallet`)
**Developer:** CoCode.dk — Babak Bandpey
**Last updated:** 14 July 2026

> The canonical, always-current version of this policy is published at
> **https://cocodedk.github.io/LinkQRWallet/privacy.html**

**Link QR Wallet keeps your saved links on your own device.** There are no accounts, no
sign-in, no analytics, no advertising, and no third-party tracking. The app never sends
your saved links or your usage to us or to any server we control. The one time the app
reaches the internet is described in [Internet access](#internet-access) below.

## Links you save

When you add a link — by pasting a URL, scanning a QR code, or sharing a link into the
app — it is stored in a private database (`linkqrwallet.db`) on your device. Each entry
can include the URL, a title, the domain, an optional note, a favourite flag, and the
time it was created or updated. This data lives only on your phone. It is not synced to
any cloud service run by us, and it is removed when you delete the entry, clear the app's
data, or uninstall the app.

## Camera (QR scanning)

The **camera** permission is used only while the scan screen is open, so you can point
your camera at a QR code. The camera preview is analysed **on your device** by an
on-device barcode scanner (Google ML Kit, bundled in the app) to read the text encoded
in the QR code. Camera frames are processed in memory and are **never saved as photos
and never uploaded**. Only the decoded text is kept, and only if you choose to save it
as a link. If a scanned code is not a valid public `http`/`https` address, it is
rejected and nothing is stored.

## Internet access

The **internet** permission is used for a single purpose: when you add a link, the app
can fetch that page's **title** so your library is easier to read. To do this it makes a
direct request from your device to the exact website you saved — the same site the link
points to — and reads only the page title. No other page content is stored.

- The request goes **straight to the destination website**. It does not pass through any
  server operated by us, and there is no intermediary, proxy, or analytics service
  involved.
- As with any web browser, the website you fetch will see the request, including your
  device's IP address and a `LinkQRWallet/1.0` identifier. What that site does with the
  request is governed by its own privacy policy.
- Only public web addresses are contacted. The app blocks `file`, `javascript`, `data`,
  and similar schemes, as well as `localhost`, private or reserved IP addresses,
  `.local`, and `.onion` hosts.
- Title fetching is a convenience. If it fails or you skip it, the app falls back to the
  link's domain, and your link is still saved locally.

The app contains **no analytics, crash reporting, advertising, cookies, or advertising
identifiers**, and no third-party SDK transmits your data. Opening a saved link launches
it in your normal browser, which is then governed by that browser's and that site's own
policies.

## Sharing a QR code

When you choose to share a link's QR code, the app writes a QR image to its private cache
and hands it to Android's standard share sheet so you can send it to an app you pick
(messaging, email, and so on). This only happens when you tap Share, and where it goes is
entirely your choice.

## Device backup

If you have enabled Android Auto Backup or Google account backup on your device, the
operating system may include this app's local data in your own personal Google backup.
This is controlled entirely by you and Google — we have no access to it. See
[Google's Privacy Policy](https://policies.google.com/privacy) for details.

## Children

The app does not knowingly collect data from anyone, including children.

## Changes

If this policy changes, the updated version will be posted here and on the website with a
new "last updated" date.

## Contact

Questions about this policy can be sent to **bb@cocode.dk** (CoCode.dk, developer: Babak
Bandpey).

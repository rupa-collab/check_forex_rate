# Check Forex Rate

Android app to track FX (USD/GBP) and metals (Gold/Silver) in INR with alerts, live mode, and on‑demand FX refresh.

## Features
- FX rates: USD and GBP vs base currency (default INR)
- Metals: Gold and Silver (per gram), plus Gold 22K
- Live Mode with foreground updates
- Threshold alerts and daily summary notifications
- On‑demand FX refresh via "Send Live Update Now" to preserve exchangerate.host quota
- Request usage display and API usage tracking

## APIs
- FX: http://api.currencylayer.com/live (API key required)
- Metals: https://api.gold-api.com/price (free endpoints)

## Setup
1. Open the project in Android Studio.
2. Set your FX API key in `local.properties`:
   ```
   EXCHANGE_RATE_API_KEY=your_key_here
   ```
3. Sync Gradle and run on a device/emulator (Android 14+ tested).

## Notes
- FX API calls are only made when you tap **Send Live Update Now**.
- Metals refresh uses the cached USD→base FX rate; run a full FX refresh once after install.

## Build
- Kotlin + Jetpack Compose
- Gradle wrapper included

## License
Private / internal use.

## Authentication
- Signup/Login is required on first launch.
- Email verification is currently skipped.
- Set AUTH_API_BASE_URL in local.properties to point to the backend.

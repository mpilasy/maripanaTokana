# Project: DualWeather Android
**Goal:** Android weather app and widgets featuring simultaneous Metric/Imperial display.

**Package:** `orinasa.njarasoa.maripanatokana`

## Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose (App), Jetpack Glance (Widgets)
- **Data:** Retrofit, OkHttp, Serialization
- **Architecture:** MVVM, Clean Architecture, Hilt (DI)
- **Weather Source:** OpenWeatherMap API

## Core Requirements
- **Dual Units with Toggle:** Every measurement shows both metric and imperial. A °C/°F toggle button swaps which is primary (bold/large) vs secondary (smaller/dimmer). Preference stored in `SharedPreferences("widget_prefs", "metric_primary")`.
- **Widget Focus:** High-priority 4x1 and 4x2 widgets.
- **Visual Style:** Dark theme, translucent cards, modern gradients (matching references).
- **Environment:** Development on Ubuntu Linux, targeting Android devices.

## Developer Context
- Owner is an experienced C#, Java, C++ developer. 
- Prefer concise, technically accurate code.
- Avoid redundant explanations; focus on implementation details and logic.
- Do not suggest or link to video tutorials.

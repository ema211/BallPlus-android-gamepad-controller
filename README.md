# PKBall+ Companion Controller for Android

An Android companion service and hardware interface designed to connect, parse, and map Bluetooth Low Energy (BLE) inputs from a Poké Ball controller into system-level touch and gamepad actions.

## Overview

The application acts as a bridge between low-level BLE controller packets and Android input events. By leveraging Android's `AccessibilityService` and a persistent foreground service, it intercepts hardware telemetry (analog stick axes, button states, battery levels) and translates them into configurable actions or on-screen touch overlays with minimal latency.

## Key Features

* **BLE Telemetry & Packet Decoding:** Real-time parsing of peripheral status packets, including battery level, button presses, and raw stick coordinates.
* **Dual Input Modes:**
  * **Continuous Joystick:** Direct translation of raw 2D analog coordinates for fluid movement.
  * **Mapped D-Pad:** Configurable angular thresholding that partitions analog stick deflection into distinct cardinal directions and diagonal zones.
* **Button Timing Engine:** Configurable press duration (pulse width), hold delay thresholds, and auto-repeat intervals for rapid-fire or long-press emulation.
* **Overlay & Calibration Suite:** Visual joystick radar, peripheral state visualizer, and live axis calibration tools.
* **Low-Overhead Architecture:** Event dispatching decoupled via background services with optional runtime debug logging.

## Tech Stack & Architecture

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose & Material 3
* **Build System:** Gradle (Kotlin DSL)

## Disclaimer
This project is an independent, open-source tool developed for educational and interoperability purposes. It is not affiliated with, endorsed, sponsored, or supported by any company. PKBALL+ and related trademarks belong to their respective owners.

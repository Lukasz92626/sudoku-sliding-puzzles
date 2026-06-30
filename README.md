# Sudoku sliding puzzles

[![Java](https://img.shields.io/badge/Java-orange.svg)](https://www.java.com/)
[![Android](https://img.shields.io/badge/Android-green.svg)](https://developer.android.com/)
[![Android Studio](https://img.shields.io/badge/Android%20Studio-brightgreen.svg)](https://developer.android.com/studio)
[![Gradle](https://img.shields.io/badge/Gradle-blue.svg)](https://gradle.org/)
[![XML](https://img.shields.io/badge/UI-XML-red.svg)](https://developer.android.com/guide/topics/ui/declaring-layout)

An Android game developed in **Java** using **Android Studio** that combines the mechanics of the classic **15-puzzle** with the rules of **Sudoku**.

## Overview

Sliding Sudoku is a logic game played on a **9×9 grid**. Instead of entering numbers, the player rearranges them by sliding tiles into an empty space, similar to the classic 15-puzzle.

The goal is to arrange the board so that every row, column, and 3×3 subgrid contains the digits **0–8** exactly once.

## Game Rules

- The board consists of a **9×9 grid**.
- Digits **1–8** appear **9 times each**.
- Digit **0** appears **8 times**.
- One field is always empty.
- Only a tile adjacent to the empty field can be moved.
- Some tiles are permanently fixed depending on the selected level.
- The puzzle is solved when every:
  - row,
  - column,
  - 3×3 subgrid
  contains the digits **0–8** without repetition.

## Difficulty Levels

The game includes **7 difficulty levels**.

| Level | Movable Area |
|-------|--------------|
| 1 | 3×3 |
| 2 | 4×4 |
| 3 | 5×5 |
| 4 | 6×6 |
| 5 | 7×7 |
| 6 | 8×8 |
| 7 | Entire 9×9 board |

## Features

- Sliding puzzle mechanics combined with Sudoku rules
- 7 difficulty levels
- Device sensor controls for moving tiles
- Lock button to temporarily disable sensor movement
- Screen rotation support
- Tile movement animations
- Android activity lifecycle handling

## Project Structure

The application consists of three main activities:

- **MainActivity** – application entry point
- **LevelChoiceActivity** – difficulty level selection
- **GameActivity** – main gameplay screen

## Android Components Used

This project demonstrates the use of several Android components and concepts:

- Intents
- Activity lifecycle methods
- Sensors
- Animations



## How to Run

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/your-repository.git
   ```

2. Open the project in **Android Studio**.

3. Build and run the application on an Android device or emulator.

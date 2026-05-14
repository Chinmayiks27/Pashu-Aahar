# PashuAahar 

PashuAahar is a specialized Android application designed for Indian dairy farmers to optimize cattle feed, reduce costs, and increase milk yield using scientific nutrition principles and AI-driven insights.

## 🚀 Features

### 1. Balanced Feed Calculator
*   **Scientific Rations:** Calculates precise feed requirements (Dry Matter, Crude Protein, TDN) based on cattle breed (Desi, Jersey, HF), age, weight, and milk yield.
*   **Ingredient Optimization:** Allows farmers to select available local ingredients (Maize, Cottonseed Cake, Wheat Bran, etc.) to generate a custom recipe.
*   **Cost Savings Analysis:** Compares the cost of home-made feed against market-bought feed, showing daily, monthly, and yearly savings.

### 2. AI Farm Advisor (Powered by Google Gemini)
*   **Automated Assessment:** Analyzes the latest feed calculations to provide a 4-point report: Feed Assessment, Top 3 Tips, Health Alerts, and Cost Advice.
*   **Farming Chatbot:** A dedicated chat interface where farmers can ask questions about cattle health, hygiene, and milk production in both English and Kannada.

### 3. Prosperity Tracker & History
*   **Local & Cloud Sync:** Uses Room Database for offline access and Firebase Firestore for cloud backup/sync.
*   **Savings History:** Tracks total money saved over time, helping farmers see the economic impact of balanced feeding.

### 4. Market Price Manager
*   Allows farmers to update local market prices for various grains and cakes to ensure cost calculations remain accurate.

### 5. Veterinary Tips & Education
*   Provides essential tips on balanced diets, clean water, shed hygiene, and vaccination schedules.

## 🔹 Balanced Feed Calculator
- Generates scientifically balanced cattle feed plans
- Calculates:
    - Dry Matter (DM)
    - Crude Protein (CP)
    - Total Digestible Nutrients (TDN)
- Supports cattle breeds:
    - Desi
    - Jersey
    - HF (Holstein Friesian)

---

## 🔹 Smart Ingredient Selection
Farmers can select locally available ingredients such as:
- Maize (Makka)
- Cottonseed Cake (Khal)
- Wheat Bran (Chokar)
- Soy Meal
- Barley
- Millet
- Rice Bran

The app generates customized feed recipes based on selected ingredients.

---

## 🔹 Cost Savings Analysis
- Compares:
    - Homemade feed cost
    - Market feed cost
- Displays:
    - Daily savings
    - Monthly savings
    - Yearly savings

---

## 🔹 AI Farm Advisor (Google Gemini AI)
- AI-powered feed analysis
- Smart farming suggestions
- Veterinary guidance
- Farming chatbot support

---

## 🔹 History & Savings Tracking
- Stores previous calculations
- Tracks total money saved
- Displays daily savings history

---

## 🔹 Veterinary Tips
Provides useful tips related to:
- Balanced diet
- Clean water
- Shed hygiene
- Vaccination
- Animal health

---

## 🔹 Offline + Cloud Support
- Works offline using Room Database
- Cloud synchronization using Firebase Firestore

---

## 🛠 Tech Stack

*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose (Modern declarative UI)
*   **Architecture:** MVVM / Clean Architecture principles
*   **Database:**
    *   **Room:** Local data persistence for cattle profiles and history.
    *   **Firebase Firestore:** Real-time cloud synchronization.
*   **Authentication:** Firebase Auth (Email/Password)
*   **AI Integration:** Google Gemini API (Generative AI for veterinary advice)
*   **Background Tasks:** WorkManager (for periodic data syncing and savings accumulation)
*   **Charts:** MPAndroidChart for visualizing savings trends.

## 📁 Project Structure

*   `MainActivity.kt`: The main entry point and UI navigation host.
*   `AppDatabase.kt`: Room database configuration and DAOs.
*   `FirebaseManager.kt`: Handles all cloud operations (Auth, Firestore sync).
*   `NutritionEngine.kt`: The "brain" of the app containing scientific formulas for cattle nutrition.
*   `GeminiAdvisor.kt`: Interface for the Google Gemini AI integration.
*   `ui/theme/`: Contains Compose theme definitions (Colors, Typography).

## 📥 Installation

1.  Clone the repository.
2.  Add your `google-services.json` to the `app/` folder.
3.  Add your Gemini API Key in `local.properties` or `BuildConfig`.
4.  Build and run using Android Studio.

# 🐄 PashuAahar 

> Smart Cattle Nutrition & Feed Optimization Application for Dairy Farmers

PashuAahar is an intelligent Android application developed to help Indian dairy farmers optimize cattle feed, reduce feeding costs, and improve milk production using scientific nutrition calculations and AI-powered guidance.

The application generates balanced feed recommendations based on cattle breed, age, weight, and milk yield. It also provides cost comparison between homemade and market feed, helping farmers make economical and healthy feeding decisions.

---

# 📱 Output Screenshots

## 🔐 Login Screen
<p align="center">
  <img src="login_screen.jpeg" width="300"/>
</p>

## 📝 Registration Screen
<p align="center">
  <img src="registration_screen.jpeg" width="300"/>
</p>

## 🐄 Cow Profile Screen
<p align="center">
  <img src="cow_profile_screen.jpeg" width="300"/>
</p>

## 🎯 Yield Target Screen
<p align="center">
  <img src="yield_target_screen.jpeg" width="300"/>
</p>

## 🧪 Nutrition Matrix
<p align="center">
  <img src="nutrition_matrix_screen.jpeg" width="300"/>
</p>

## 💰 Savings Result
<p align="center">
  <img src="savings_result_screen.jpeg" width="300"/>
</p>

## 📈 History Screen
<p align="center">
  <img src="history_screen.jpeg" width="300"/>
</p>

## 🤖 AI Advisor
<p align="center">
  <img src="ai_advisor_screen.jpeg" width="300"/>
</p>


# 🚀 Installation

1. Clone the repository
2. Add `google-services.json` inside the app folder
3. Add Gemini API Key in `local.properties`
4. Open project in Android Studio
5. Build and run the application

---

# 👩‍💻 Developed By

**Chinmayi K S**  
Under **MindMatrix Internship**

📧 Email: chinmayiks27@gmail.com

---

# 🌱 Vision

PashuAahar aims to support dairy farmers with smart technology, scientific cattle nutrition, and AI-powered guidance to improve livestock productivity and reduce feeding costs.

---

⭐ Developed for the prosperity of Dairy Farmers ⭐



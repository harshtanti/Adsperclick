# 📞 Customer Support Chat Application

> A scalable, full-featured business communication app developed for **AdsPerClick** — a startup empowering small businesses to boost their online presence through design and marketing solutions.

This application provides an **admin-driven communication ecosystem** where companies, clients, and employees can seamlessly collaborate through real-time chat and calling.

---

## 🚀 Key Features

### 🔒 Role-Based Access
- Admin-exclusive access to create/manage:
  - Services (e.g., Amazon, Flipkart, Meesho)
  - Companies
  - Employees & Clients
- Admin can:
  - Block/unblock users
  - Send broadcast messages
  - Monitor group activity

### 💬 Real-Time Messaging
- Chat with support for **images, videos, and documents**
- **Instant push notifications** using Firebase Cloud Messaging (FCM)

### 📞 Group Calling
- Any member can initiate a group call
- Others receive a ringing notification and can **join seamlessly**

### 📊 Intelligent Chat Monitoring
Visual indicators help track chat responsiveness:
- 🔴 **Red**: Client message **unanswered by employee** for 1 hour  
- 🟣 **Purple**: Employee message **unanswered by client** for 2 hours

### 🔍 Smart Search
- Quickly find **users**, **groups**, **services**, or **companies**

### 🛠 Admin Tools
- Create and manage:
  - Services
  - Client Companies
  - Clients & Employees
- Send broadcast messages to:
  - All users
  - Only employees
  - Only clients

### 👤 Rich User Profiles
- Users and groups can set **custom profile pictures**
- Clean, WhatsApp-style UI design

---

## 🧱 Tech Stack

| Layer              | Technology                                                                 |
|-------------------|-----------------------------------------------------------------------------|
| **Language**       | Kotlin                                                                      |
| **Architecture**   | MVVM                                                                        |
| **Dependency Injection** | Hilt                                                                |
| **Backend**        | NodeJS and Firebase (Auth, Storage, Realtime DB, Crashlytics)         |
| **Communication**  | Firestore + FCM for real-time chat & push notifications                    |
| **Calls**          | Extensible with Agora SDK for group calling                          |

---

## 🔐 Admin Use Cases

- ✅ **Create Services**  
  Add services like Zomato, Flipkart, etc., to the platform.
  
- 🏢 **Onboard Companies**  
  Register client companies with GST details and assigned services.

- 👥 **Manage Users**  
  Create employees and clients using their Aadhar ID & login credentials.

- 📊 **Moderate Conversations**  
  Visual indicators highlight **unanswered chats** for quick admin action.

---

## 📸 Screenshots (Coming Soon)

_Showcase the app’s UI, chat interface, and admin features here._

---

## 👨‍💻 About the Developers

| [**Saumya Kumar Thakur**](mailto:saumyakumarthakurp@gmail.com) | [**Harsh Tanti**](mailto:harshtanti59@gmail.com) |
|:--|:--|
| Android Developer passionate about building robust, scalable, and delightful mobile experiences. Specialized in Kotlin, MVVM, and Firebase-powered apps. | Android Developer focused on crafting intuitive, performance-optimized apps with modern architecture and scalable backend integrations. |

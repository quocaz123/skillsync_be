# SkillSync Platform

Monorepo chứa toàn bộ backend services của SkillSync.

## 📦 Cấu trúc thư mục

```
CAPSTONE/
├── docker-compose.yml              # Kafka + Zookeeper + Kafka UI
├── skillsync/                      # Backend chính (Spring Boot, port 8080)
└── skillsync-notification/         # Email Notification Service (Spring Boot, port 8081)
```

## 🚀 Khởi động môi trường

### 1. Chạy Kafka (bắt buộc)
```bash
docker compose up -d
```

Kafka UI sẽ khả dụng tại: http://localhost:8090

### 2. Chạy skill_be (backend chính)
```bash
cd skillsync
./mvnw spring-boot:run
```

### 3. Chạy notification service
```bash
cd skillsync-notification
./mvnw spring-boot:run
```

## 📧 Kafka Topics

| Topic | Mô tả |
|---|---|
| `skillsync.notification.auth` | Sự kiện Auth (Welcome, Password Reset) |
| `skillsync.notification.session` | Sự kiện Session (Booked, Approved, ...) |
| `skillsync.notification.credit` | Sự kiện Credit (Earned, Spent, Refund) |
| `skillsync.notification.skill` | Sự kiện Skill (Verified, Rejected) |

## ⚙️ Yêu cầu
- Java 21
- Maven 3.9+
- Docker Desktop

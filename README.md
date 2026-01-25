# 💬 Discodeit
Spring Boot 기반 채널형 메시징 백엔드 서버

[![codecov](https://codecov.io/gh/LZHTK/3-sprint-mission/branch/main/graph/badge.svg)](https://codecov.io/gh/LZHTK/3-sprint-mission)
---

## 📌 프로젝트 개요
Discodeit은 채널 중심의 커뮤니케이션 서비스를 제공하는 백엔드 서버입니다.  
REST API, WebSocket(STOMP), SSE를 함께 지원하며 실시간 메시징/알림 기능을 제공합니다.

---

## ✨ 주요 기능
- JWT 인증 + 소셜 로그인(Google/Kakao)
- 채널(공개/비공개) 생성 및 메시지 전송
- 첨부파일 업로드/다운로드
- SSE 알림, WebSocket 실시간 메시지
- Redis/Kafka 기반 확장 구성 지원

---

## ⚙️ 기술 스택
- **Language**: Java 17
- **Framework**: Spring Boot 3.4.0
- **DB**: PostgreSQL, H2(test)
- **ORM**: Spring Data JPA (Hibernate)
- **Cache/Messaging**: Redis, Kafka
- **Auth**: Spring Security, JWT
- **Storage**: Local FS, AWS S3
- **Docs/Monitoring**: Swagger(OpenAPI), Actuator

---

## 🧩 프로파일 기반 설정
- **dev**: 로컬 PostgreSQL + debug 로그
- **prod**: 환경변수 기반 DB + warn/info 로그
- **docker**: Kafka 브로커 연결 설정
- **distributed**: Redis/SSE + Kafka 분산 구성
- **test**: H2 in-memory + ddl-auto(create-drop)

---

## 🧾 패키지 구조
```
com.sprint.mission.discodeit
├── DiscodeitApplication
├── aop
├── common
├── config
├── controller
│   └── api
├── dto
│   ├── data
│   ├── request
│   └── response
├── entity
│   └── base
├── event
│   ├── kafka
│   └── message
├── exception
│   ├── auth
│   ├── binarycontent
│   ├── channel
│   ├── message
│   ├── notification
│   ├── readstatus
│   └── user
├── mapper
├── redis
├── repository
├── security
│   ├── jwt
│   └── websocket
├── service
│   ├── basic
│   └── distributed
└── storage
    ├── local
    └── s3
```
---
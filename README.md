# AI 智能刷题考试平台

面向学习与课程实践的 AI 刷题、练习和在线考试平台。系统采用前后端分离的微服务结构，提供用户认证、题库管理、练习错题、组卷考试、AI 题目生成、在线判题和统一网关能力。

## 技术栈与结构

```text
backend/                 Spring Boot 3 主业务服务（8081）
services/ai-service/     AI 题目生成服务（8083）
services/judge-service/  在线判题服务（8082）
gateway/                 Node.js 网关（8080）
frontend/                Vue 3 + TypeScript + Vite 前端
sql/                     MySQL 初始化脚本
```

后端使用 Java 21、MySQL 8、Redis、MyBatis-Plus 和 JWT；前端使用 Node.js 20+、Vue 3、Element Plus、Pinia 和 Vite。AI 与判题服务通过内部服务令牌与主业务服务通信。

## 本地运行

环境变量至少包括 `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USER`、`DB_PASSWORD`、`JWT_SECRET` 和 `SERVICE_TOKEN`。生产环境必须使用随机密钥覆盖源码中的开发占位值。

```powershell
# 1. 初始化数据库
mysql -u root -p < .\sql\ai_exam.sql

# 2. 启动三个 Java 服务，建议顺序为 judge、ai、backend
mvn.cmd -pl services/judge-service spring-boot:run
mvn.cmd -pl services/ai-service spring-boot:run
mvn.cmd -pl backend spring-boot:run

# 3. 启动网关
Set-Location .\gateway
npm.cmd install
npm.cmd start

# 4. 启动前端（另开终端）
Set-Location .\frontend
npm.cmd ci
npm.cmd run dev
```

前端通过 Vite 代理访问 `8080` 网关。完整配置、端口和生产部署说明见 [docs/运行与部署说明.md](docs/运行与部署说明.md)。

## 文档

- [运行与部署说明](docs/运行与部署说明.md)
- [服务与接口说明](docs/服务与接口说明.md)
- [安全说明](SECURITY.md)

## 质量检查

```powershell
mvn.cmd clean verify
Set-Location .\frontend
npm.cmd ci
npm.cmd run build
```

## 安全说明

不要提交真实数据库密码、JWT 密钥、第三方 API Key 或服务令牌。配置通过环境变量注入；发现已泄露的凭据时必须立即轮换。

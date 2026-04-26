# GatePilot Console

GatePilot Console 是独立 Vue 前端模块，只调用 `gatepilot-apiserver` REST/JSON API，不直接访问 proxy、数据库或后端配置文件。

## 本地运行

```bash
npm install
npm run dev
```

默认地址：

```text
http://127.0.0.1:5174
```

## 设计约束

- 默认中文优先，`zh-CN` 是首个 locale。
- 风格走成熟运维控制台路线，保持克制、清晰、便于排障。
- 页面请求收口到 `src/api`。
- 页面文案收口到 `src/i18n`。
- 新页面先补页面目标、参考系统、主流程和状态设计，再写代码。

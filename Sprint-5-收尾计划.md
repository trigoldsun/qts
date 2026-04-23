# Sprint 5 收尾计划

## 已完成项 ✅

### Git 提交
- ✅ 7个修改未提交文件已提交
  - postgres.yaml: 使用 k8s secret 管理 DB 密码
  - RiskStatusPanel.vue: 响应式样式、告警声音、视觉效果
  - TradingPanel.vue: 组件提取、响应式设计、确认对话框
  - biz-market: CORS 环境变量配置
  - biz-risk: 生产环境 Dockerfile
  - biz-settle: 事务传播改进 (REQUIRES_NEW)
  - biz-market config.yaml
  - biz-trade Dockerfile

- ✅ 16个新文件已提交
  - bff-web 完整 Vue 项目结构
  - components: ChineseDecoration, ConfirmDialog, MarketSummary, OrderForm, OrderTable, PositionTable, ThemeToggle
  - composables: useConfirm, useResponsive, useTheme
  - utils: alertSound, format
  - styles: tokens.css, responsive.css, chinese-traditional.css
  - 配置文件: tsconfig, vite.config, package.json

### BFF-WEB 构建验证
- ✅ npm install 成功
- ✅ vite build 成功 (181.94 kB JS, 56.22 kB CSS)
- ⚠️ vue-tsc 类型检查有错误 (但不影响运行时)

## 待处理项 ⚠️

### BFF-WEB 类型检查错误 (非阻塞)
```
- OrderForm.vue: mobileExpanded 只读属性问题
- TradingPanel.vue: Position 类型不兼容、API 响应类型问题
- RiskStatusPanel.vue: Notification urgent 选项问题
- 多个组件存在未使用的变量
```
**影响**: 不影响运行时，仅类型检查失败
**修复方案**: 需要完善类型定义或调整 strict 模式

### TODO 项 (需要后续实现)
| 服务 | 文件 | 内容 | 优先级 |
|------|------|------|--------|
| biz-risk | PreCheckService.java:104 | 注入服务获取真实数据 | 中 |
| biz-risk | PreCheckController.java:63 | 注入实际服务 | 中 |
| biz-trade | RiskCheckClient.java:45 | 实现 gRPC 调用 | 高 |
| biz-trade | OrderCommandService.java:238 | 调用账户服务验证 | 高 |
| biz-trade | OrderCommandService.java:245 | 通过 gRPC 调用资产服务 | 高 |

## 下一步行动 📋

### 立即可做 (1-2小时)
1. 修复 bff-web 类型错误
   - 添加 vite-env.d.ts 类型定义
   - 修复 OrderForm mobileExpanded v-model 问题
   - 统一 API 响应类型

2. 提交 package.json 更改到 git

### 短期 (1-2天)
1. 实现 biz-trade RiskCheckClient gRPC 调用
2. 实现 biz-trade OrderCommandService 账户/资产服务调用
3. biz-risk 服务依赖注入重构

### 中期 (1周)
1. 集成测试 (biz-trade + biz-risk + biz-market)
2. 端到端测试
3. 部署脚本完善

## 时间估计 ⏱️

| 任务 | 估计时间 | 实际时间 |
|------|----------|----------|
| Git 提交 | 15min | ✅ 完成 |
| BFF-WEB 构建 | 30min | ✅ 完成 |
| TODO gRPC 实现 | 4-6小时 | ⚠️ 待处理 |
| 集成测试 | 1-2天 | ⚠️ 待处理 |

## 风险项 🔴

1. **NestJS 后端代码混在 Vue 前端项目中** - bff-web/src/ 下存在 market/, risk/, trade/, test/ 目录包含 NestJS 代码
2. **gRPC 服务调用未实现** - biz-trade 依赖外部服务但未实现
3. **数据库密码管理** - 使用 k8s secret 需要相应 secret 资源存在

## 建议 📝

1. 清理 bff-web/src/ 下的 NestJS 代码或明确区分前后端项目
2. 建立 gRPC 服务接口规范文档
3. 添加 CI/CD 构建验证步骤

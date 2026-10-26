# 贡献指南

提交修改前请先创建 Issue 说明问题、复现步骤或需求范围；PR 说明修改原因、影响模块和实际执行的验证命令。

```powershell
mvn.cmd -B verify
npm.cmd --prefix frontend ci
npm.cmd --prefix frontend run build
```

同时调整接口、环境变量或启动流程时，请同步更新 `docs/`。不要提交 `target/`、`node_modules/`、`dist/`、日志、真实凭据或个人数据。新增依赖时提交对应锁文件并说明用途。

仓库尚未指定项目开源许可证，贡献或复用前请先与维护者约定授权范围。

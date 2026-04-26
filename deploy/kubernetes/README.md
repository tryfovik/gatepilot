# GatePilot Kubernetes 基础部署

这份清单只描述 GatePilot 自己的控制面和数据面，不包含 HAProxy、Keepalived、Nginx 或 Ingress。

推荐入口链路：

```text
Client -> VIP(HAProxy + Keepalived) -> Nginx 集群 -> Service/gatepilot-proxy -> proxy + agent
```

关键边界：

- VIP、HAProxy、Keepalived、Nginx 只做入口高可用和粗粒度转发
- 项目路由、染色、灰度、蓝绿、限流、熔断和审计由 GatePilot proxy 执行
- apiserver 使用数据库作为配置事实来源
- 基础清单把 apiserver 和 controller-manager 放在控制面进程内装配，数据面 proxy 独立扩容
- controller-manager 默认开启 getboot-lock database 锁，控制面多副本不会静默退化成无锁调度
- proxy Deployment 可以直接扩副本，agent 使用 Pod 名注册为节点
- HPA 只扩 `gatepilot-proxy`，不改变 Java 转发代码

部署前需要替换镜像和数据库 Secret：

```bash
kubectl create secret generic gatepilot-database \
  -n gatepilot-system \
  --from-literal=url='jdbc:mysql://mysql.example.com:3306/gatepilot' \
  --from-literal=username='gatepilot' \
  --from-literal=password='change-me'

kubectl apply -k deploy/kubernetes/base
```

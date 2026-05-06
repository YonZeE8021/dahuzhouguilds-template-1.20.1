# DahuzhouGuilds 模组测试要求（精简版）

> Minecraft 1.20.1 (Fabric) | 模组 ID: `dahuzhouguilds`

---

## 一、公会核心功能

### 1. 创建公会 `/guild create <name> <color>`
- ✅ 成功创建公会，创建者成为Guild Master
- ❌ 名称超13字符 → 提示错误
- ❌ 无效颜色 → 提示错误
- ❌ 已在公会中 / 名称重复 → 提示错误
- 创建后自动生成计分板队伍和银行文件

### 2. 解散公会 `/guild disband`
- ❌ 非会长使用 → 提示"只有会长才能解散"
- ✅ 会长使用 → 显示[YES]/[NO]确认按钮
- 确认后：删除公会文件、银行文件、计分板队伍；通知盟友；清除所有成员前缀

### 3. 查看信息 `/guild info [名称/玩家]`
- 显示：公会名、MOTD、家园状态、创建日期、会长、成员列表（在线/离线）、盟友列表及PvP状态

### 4. 成员管理
- **邀请** `/guild invite <玩家>` (需canInvite权限) → 被邀请者收到[ACCEPT]按钮
- **接受** `/guild accept` → 默认成为Initiate身份
- **退出** `/guild quit` → 清除队伍和前缀，通知成员
- **踢出** `/guild kick <玩家>` (需canKick权限) → 不可踢会长
- **晋升** `/guild promote <玩家>` (需canPromote权限) Initiate→Member→Officer
- **降职** `/guild demote <玩家>` (需canDemote权限) Officer→Member→Initiate

---

## 二、公会聊天

- `/guild chat` → 切换公会频道，再次使用切回全局
- 公会消息格式：`[公会名] [身份] 玩家名: 消息`（仅公会成员可见）
- 公会/联盟聊天由Mixin拦截并重定向

---

## 三、公会传送

- `/guild sethome` (需canSetHome) → 设置家园
- `/guild home` (需canUseHome) → 延迟传送（默认6秒）
- 延迟期间：移动取消、受伤取消、死亡取消（均可配置）

---

## 四、联盟系统

### 4.1 联盟管理
- `/guild ally <公会名>` → 会长向对方发起联盟请求
- 对方会长点击[ACCEPT]接受，[DENY]拒绝
- `/guild allyrevoke <shortId>` → 撤销联盟

### 4.2 联盟聊天 `/ally chat <公会名>`
- 需双方互为盟友且聊天桥接激活
- 消息格式：`[ALLY] 玩家名: 消息`
- 发送给双方公会所有在线成员

### 4.3 联盟PvP `/ally togglepvp <公会名>`
- 会长启用/禁用与盟友的PvP
- 双方PvP均禁用时：互相攻击被阻止，提示"你不能伤害盟友公会成员"

---

## 五、银行系统

### 5.1 基础操作
- `/guild bank <页码>` → 打开银行GUI（45格存储+9格页面选择器）
- 页面选择器：绿色=当前页，黑色=可用页，红色=锁定页
- 存取权限：canDepositTabX / canWithdrawTabX

### 5.2 标签页解锁
- `/guild donate` → 手持下界合金锭捐赠（需canDonate权限）
- `/guild progress` → 查看解锁进度
- `/guild unlock` → 进度满足时手动解锁
- 费用：第N页 = 8×N 个下界合金锭（N≥1）

| 标签页 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 |
|--------|---|---|---|---|---|---|---|---|---|
| 费用 | 8 | 16 | 24 | 32 | 40 | 48 | 56 | 64 | 72 |

---

## 六、权限系统

| 身份 | 默认权限 |
|------|----------|
| **Guild Master** | 全部权限（包括 Invite/Kick/Promote/Demote/SetHome/UseHome/Bank/TogglePvP/Donate） |
| **Officer** | 与GM相同 |
| **Member** | 仅 Invite✓ UseHome✓ |
| **Initiate** | 全部× |

- `/guild permissions`（仅会长）→ GUI菜单编辑各身份权限
- 会长不受权限系统限制，始终拥有全部权限

---

## 七、PvP与伤害保护

- **盟友PvP保护**：双方禁用PvP时，盟友间伤害被取消
- 受伤时：取消传送、关闭银行界面

---

## 八、外部集成

| 功能 | 说明 |
|------|------|
| **EssentialsX昵称** | 启用后自动设置 `[公会名] 玩家名`（公会颜色），离开时清除 |
| **HungerBound昵称** | 启用后根据死亡次数显示颜色：0次=🟢绿色、1次=🟡黄色、≥2次=🔴红色 |

---

## 九、数据持久化

| 数据类型 | 文件路径 | 触发保存 |
|----------|----------|----------|
| 公会数据 | `<world>/guilds/guild_<8位UUID>.json` | 创建/修改/解散/服务器关闭时 |
| 银行数据 | `<world>/guilds/guild_<8位UUID>_bank.json` | 变更/页面切换/断开连接/服务器关闭时 |
| 联盟桥接 | 内存重建 | 服务器启动时恢复 |

---

## 十、配置文件

路径：`config/guilds_remastered_config.json`

```json
{
  "enable_Essentials_Command_Guild_Prefix": false,
  "enable_HungerBound_Integration": false,
  "guild_Home_Teleport_Delay_Seconds": 6,
  "cancel_teleport_on_move": true,
  "cancel_teleport_on_damage": true
}
```

---

## 附录：命令速查

| 命令 | 功能 | 权限 |
|------|------|------|
| `/guild create <name> <color>` | 创建公会 | 无公会 |
| `/guild info [name]` | 查看信息 | 无限制 |
| `/guild invite <player>` | 邀请玩家 | canInvite |
| `/guild accept` | 接受邀请 | 无限制 |
| `/guild quit` | 退出公会 | 成员 |
| `/guild kick <player>` | 踢出成员 | canKick |
| `/guild promote <player>` | 晋升 | canPromote |
| `/guild demote <player>` | 降职 | canDemote |
| `/guild disband` | 解散公会 | 会长 |
| `/guild rename <name> <color>` | 重命名 | 会长 |
| `/guild chat` | 切换公会聊天 | 成员 |
| `/guild sethome` | 设置家园 | canSetHome |
| `/guild home` | 传送家园 | canUseHome |
| `/guild motd <msg>` | 设置MOTD | 会长/Officer |
| `/guild bank <page>` | 打开银行 | canUseBankTabX |
| `/guild donate` | 捐赠下界合金锭 | canDonate |
| `/guild progress` | 解锁进度 | 成员 |
| `/guild unlock` | 手动解锁标签页 | 成员 |
| `/guild permissions` | 权限GUI | 会长 |
| `/guild ally <guild>` | 联盟请求 | 会长 |
| `/guild allyrevoke <shortId>` | 撤销联盟 | 会长 |
| `/guild toggle_friendlyfire` | 友军伤害开关 | canTogglePvP |
| `/ally chat <guild>` | 联盟聊天 | 成员 |
| `/ally togglepvp <guild>` | 联盟PvP | 会长 |

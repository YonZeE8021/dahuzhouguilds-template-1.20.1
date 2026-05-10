# 更新日志

## 1.2.4

### 修复

- 公会存档（`world/.../guilds/*.json`）、公会银行（`*_bank.json`）与 `config/guilds_remastered_config.json` 的读写统一为 **UTF-8**，不再依赖 JVM 默认编码（例如中文 Windows 下的 GBK）。可避免中文等内容乱码，以及启动流程中批量保存把错误解码写回磁盘。**若某次启动后存档已被错误编码覆盖**，需从备份恢复 `guilds`（及必要时配置）后再用本版本启动。

### 变更

- **公会对外编号**改为 **四位纯数字**（0000–9999；展示为四位，指令可写 1～4 位数字并自动补零）。`/guild info` 与 `/guildadmin list` 等展示的「公会 ID」为该编号。指令中的公会标识仍支持 **完整 UUID** 与旧版 **`guild_` + UUID 前 8 位十六进制**。
- 公会 JSON 增加 **`uuid`**（内部 UUID）与 **`numericPublicId`**；首次加载会为旧数据分配编号，并迁移盟友映射键、公会银行文件名（例如自 `guild_<8位hex>_bank.json` 至 `guild_<四位数字>_bank.json`）等。**单世界公会数量超过 10000 时**无法为新增公会分配唯一四位编号。

### 本地化

- 更新公会 ID 相关说明与错误提示（`zh_cn.json` / `en_us.json`）。

---

## 1.2.0

### 新增

- **转让会长**：`/guild transfer <玩家名>`，由现任会长将职位转让给公会内其他成员；原会长变为军官（Officer），新会长为 Guild Master。支持 Essentials / HungerBound 前缀与队伍刷新。
- **配置项** `require_Op_To_Transfer_Guild`（默认 `true`）：为 `true` 时执行转让需权限等级 2（OP）；为 `false` 时会长无需 OP 即可使用（仍须满足会长身份等规则）。配置文件为 `config/guilds_remastered_config.json`，缺键会自动合并写入。
- **管理维护指令** `/guildadmin`（权限等级 2）：
  - `reload`：从存档重新加载全部公会数据
  - `list`：列出所有公会名称与 UUID
  - `info <guildId>`：查看指定公会详情（与玩家 `/guild info` 相同展示）
  - `disband <guildId>`：强制解散公会
  - `kick <guildId> <玩家名>`：踢出成员（不可踢会长，需先 `transfer`）
  - `transfer <guildId> <玩家名>`：强制将会长转让给公会内指定玩家
  - `addmember <guildId> <玩家名>`：将在线玩家以「新丁」加入指定公会
  - `removeally <guildId> <allyGuildId>`：强制解除双方结盟并移除盟友聊天桥
  - `clearinvites` / `clearinvites <guildId>`：清除全部待处理邀请，或仅清除指向某公会的邀请
  - `clearallyrequests`：清除内存中全部待处理结盟请求
  - `saveall`：将所有公会写回磁盘
  - `setmotd <guildId> <消息…>`：强制设置公会公告
  - `deletebank <guildId>`：删除该公会银行存档（若存在）
- **公会数据**：`GuildDataManager.getGuildByIdInput` 用于解析指令中的公会 ID；`clearAllInvites`、`clearAllPendingAllyRequests` 供维护指令使用。
- **解散逻辑复用**：`GuildCommand.disbandGuildCompletely` 供玩家确认解散与管理端 `disband` 共用。

### 变更（破坏性）

- 以下指令中，原「公会名称」或依赖名称/玩家解析的参数已改为 **公会 ID**（完整 UUID，可在 **`/guild info`（无参数，查看自己公会）** 输出中的「公会 ID」一行复制；仍兼容旧式 `guild_` + 8 位十六进制短 ID）：
  - `/guild info <guildId>`（不再支持用公会名或玩家名查询）
  - `/guild ally <guildId>`
  - `/guild allyrevoke <guildId>`（聊天内解除结盟按钮改为使用完整 UUID；补全建议优先为 UUID）
  - `/ally chat <guildId>`
  - `/ally togglepvp <guildId>`
- **`/guild info` 展示**：增加一行公会完整 UUID，便于复制到上述指令。

### 本地化

- 新增与调整 `zh_cn.json` / `en_us.json` 中转让、公会 ID 错误提示、`guildadmin` 相关文案及 `info.guild_id_label` 等键。

---

## 1.1.1 及更早

见仓库历史与标签；本文件自 1.2.0 起按版本维护。

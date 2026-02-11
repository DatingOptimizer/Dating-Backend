# 错误码文档

## 错误码格式

错误码采用分类编号设计，格式为 `[模块前缀][错误类型][序号]`

- **模块前缀**: BIO(1xxx), PHOTO(2xxx), CONV(3xxx), CLAUDE(4xxx), SYS(9xxx)
- **错误类型**: 业务错误(0-4), 外部服务错误(5-7), 系统错误(8-9)

## 错误响应格式

```json
{
  "code": 4002,
  "status": 429,
  "error": "CLAUDE_RATE_LIMIT",
  "message": "Claude API rate limit exceeded",
  "path": "/api/v1/profile/bio/rewrite",
  "timestamp": "2025-01-15T10:30:00",
  "retryable": true
}
```

## 错误码列表

### 通用错误 (1000-1099)

| 错误码 | 错误名称 | HTTP 状态码 | 描述 | 可重试 |
|--------|---------|------------|------|--------|
| 0 | SUCCESS | 200 | 请求成功 | - |
| 1000 | BAD_REQUEST | 400 | 请求参数无效 | ❌ |
| 1001 | UNAUTHORIZED | 401 | 未授权访问 | ❌ |
| 1002 | FORBIDDEN | 403 | 访问被禁止 | ❌ |
| 1003 | NOT_FOUND | 404 | 资源未找到 | ❌ |
| 1004 | VALIDATION_ERROR | 400 | 参数验证失败 | ❌ |

### Bio 相关错误 (1100-1199)

| 错误码 | 错误名称 | HTTP 状态码 | 描述 | 可重试 |
|--------|---------|------------|------|--------|
| 1100 | BIO_TOO_SHORT | 400 | Bio 文本过短 (最少10字符) | ❌ |
| 1101 | BIO_TOO_LONG | 400 | Bio 文本过长 (最多500字符) | ❌ |
| 1102 | BIO_INVALID_FORMAT | 400 | Bio 包含非法字符或格式 | ❌ |
| 1150 | BIO_GENERATION_FAILED | 500 | Bio 重写生成失败 | ✅ |

### Photo 相关错误 (2000-2099)

| 错误码 | 错误名称 | HTTP 状态码 | 描述 | 可重试 |
|--------|---------|------------|------|--------|
| 2000 | PHOTO_INVALID_URL | 400 | 照片 URL 格式无效 | ❌ |
| 2001 | PHOTO_TOO_FEW | 400 | 照片数量不足 (最少2张) | ❌ |
| 2002 | PHOTO_TOO_MANY | 400 | 照片数量过多 (最多10张) | ❌ |
| 2050 | PHOTO_DOWNLOAD_FAILED | 400 | 照片下载失败 | ✅ |
| 2051 | PHOTO_ANALYSIS_FAILED | 500 | 照片分析失败 | ✅ |

### Conversation 相关错误 (3000-3099)

| 错误码 | 错误名称 | HTTP 状态码 | 描述 | 可重试 |
|--------|---------|------------|------|--------|
| 3000 | CONV_INVALID_INPUT | 400 | 对话输入无效 | ❌ |
| 3001 | CONV_CONTEXT_TOO_LONG | 400 | 上下文信息过长 | ❌ |
| 3050 | CONV_GENERATION_FAILED | 500 | 对话开场白生成失败 | ✅ |

### Claude API 相关错误 (4000-4099)

| 错误码 | 错误名称 | HTTP 状态码 | 描述 | 可重试 |
|--------|---------|------------|------|--------|
| 4000 | CLAUDE_API_KEY_MISSING | 500 | Claude API 密钥未配置 | ❌ |
| 4001 | CLAUDE_API_KEY_INVALID | 401 | Claude API 密钥无效 | ❌ |
| 4002 | CLAUDE_RATE_LIMIT | 429 | Claude API 速率限制 | ✅ |
| 4003 | CLAUDE_QUOTA_EXCEEDED | 402 | Claude API 配额超限 | ❌ |
| 4004 | CLAUDE_INVALID_REQUEST | 400 | Claude API 请求无效 | ❌ |
| 4005 | CLAUDE_TIMEOUT | 408 | Claude API 请求超时 | ✅ |
| 4006 | CLAUDE_OVERLOADED | 503 | Claude API 服务过载 | ✅ |
| 4007 | CLAUDE_SERVER_ERROR | 500 | Claude API 服务器错误 | ✅ |
| 4008 | CLAUDE_CONNECTION_ERROR | 503 | Claude API 连接失败 | ✅ |
| 4009 | CLAUDE_RESPONSE_PARSE_ERROR | 500 | Claude API 响应解析失败 | ❌ |

### 系统错误 (9000-9099)

| 错误码 | 错误名称 | HTTP 状态码 | 描述 | 可重试 |
|--------|---------|------------|------|--------|
| 9000 | SYSTEM_ERROR | 500 | 内部系统错误 | ❌ |
| 9001 | DATABASE_ERROR | 500 | 数据库操作失败 | ❌ |
| 9002 | FILE_UPLOAD_SIZE_EXCEEDED | 413 | 文件上传大小超限 | ❌ |
| 9003 | EXTERNAL_SERVICE_ERROR | 503 | 外部服务不可用 | ✅ |
| 9004 | RETRY_EXHAUSTED | 503 | 重试次数已用尽 | ❌ |

## 重试机制

### Claude API 重试策略

**自动重试的错误码:**
- `4002` CLAUDE_RATE_LIMIT (速率限制)
- `4005` CLAUDE_TIMEOUT (请求超时)
- `4006` CLAUDE_OVERLOADED (服务过载)
- `4007` CLAUDE_SERVER_ERROR (服务器错误)
- `4008` CLAUDE_CONNECTION_ERROR (连接错误)

**重试配置:**
- 最大重试次数: 3 次
- 初始延迟: 1 秒
- 延迟倍数: 2 (指数退避)
- 最大延迟: 10 秒

**重试时间表:**
- 第 1 次重试: 1 秒后
- 第 2 次重试: 2 秒后
- 第 3 次重试: 4 秒后

### 使用示例

#### 1. 抛出业务异常

```java
// 参数验证失败
if (bioText.length() < 10) {
    throw new BusinessException(ErrorCode.BIO_TOO_SHORT);
}

// 自定义错误消息
if (photos.size() > 10) {
    throw new BusinessException(
        ErrorCode.PHOTO_TOO_MANY,
        "You provided " + photos.size() + " photos, but maximum is 10"
    );
}
```

#### 2. 抛出 Claude API 异常

```java
// 根据 HTTP 状态码创建异常
catch (HttpClientErrorException e) {
    throw ClaudeApiException.fromHttpStatus(
        e.getStatusCode().value(),
        "Failed to call Claude API: " + e.getMessage()
    );
}

// 直接抛出特定错误
if (apiKey == null || apiKey.isEmpty()) {
    throw new ClaudeApiException(ErrorCode.CLAUDE_API_KEY_MISSING);
}
```

#### 3. 使用重试模板

```java
@Service
public class ClaudeApiService {

    @Autowired
    private RetryTemplate claudeApiRetryTemplate;

    public String callClaudeApi(String prompt) {
        return claudeApiRetryTemplate.execute(context -> {
            // 调用 Claude API 的代码
            // 如果抛出可重试的 ClaudeApiException，会自动重试
            return makeApiCall(prompt);
        });
    }
}
```

#### 4. 使用注解方式重试

```java
@Service
public class BioService {

    @Retryable(
        value = ClaudeApiException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    public BioRewriteResponse rewriteBio(BioRewriteRequest request) {
        // 方法会在抛出 ClaudeApiException 时自动重试
        return claudeApiClient.rewriteBio(request);
    }

    @Recover
    public BioRewriteResponse recover(ClaudeApiException e, BioRewriteRequest request) {
        // 重试耗尽后的降级处理
        log.error("Failed to rewrite bio after retries", e);
        throw new BusinessException(ErrorCode.RETRY_EXHAUSTED,
            "Failed to generate bio rewrite after multiple attempts");
    }
}
```

## 前端处理建议

### 1. 根据 retryable 字段决定是否重试

```javascript
async function callApi() {
  try {
    const response = await fetch('/api/v1/profile/bio/rewrite', {
      method: 'POST',
      body: JSON.stringify(data)
    });

    if (!response.ok) {
      const error = await response.json();

      // 如果错误可重试，显示重试按钮
      if (error.retryable) {
        showRetryButton();
      } else {
        showErrorMessage(error.message);
      }
    }
  } catch (error) {
    // 网络错误，显示重试按钮
    showRetryButton();
  }
}
```

### 2. 根据错误码显示友好提示

```javascript
const ERROR_MESSAGES = {
  1100: '您的个人简介太短了，请至少输入 10 个字符',
  1101: '您的个人简介太长了，请不要超过 500 个字符',
  2001: '请至少上传 2 张照片才能进行排序',
  4002: 'AI 服务繁忙，请稍后再试',
  4003: 'AI 服务配额已用完，请联系管理员'
};

function showUserFriendlyError(errorCode) {
  const message = ERROR_MESSAGES[errorCode] || '操作失败，请重试';
  alert(message);
}
```

## 监控和告警

### 需要监控的错误码

**高优先级 (立即告警):**
- `4000` CLAUDE_API_KEY_MISSING
- `4001` CLAUDE_API_KEY_INVALID
- `4003` CLAUDE_QUOTA_EXCEEDED
- `9000` SYSTEM_ERROR
- `9001` DATABASE_ERROR

**中优先级 (频率告警):**
- `4002` CLAUDE_RATE_LIMIT (每小时 > 10次)
- `4005` CLAUDE_TIMEOUT (每小时 > 20次)
- `4006` CLAUDE_OVERLOADED (每小时 > 10次)

**低优先级 (仅记录):**
- 业务验证错误 (1xxx, 2xxx, 3xxx)
- 客户端参数错误 (1000-1004)

## 版本历史

| 版本 | 日期 | 变更说明 |
|------|------|---------|
| 1.0.0 | 2025-01-15 | 初始版本，定义所有错误码 |

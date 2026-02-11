# 错误处理快速开始

## 📋 已创建的文件

### 核心类
1. **ErrorCode.java** - 错误码枚举（包含所有自定义错误码）
2. **ClaudeApiException.java** - Claude API 专用异常
3. **BusinessException.java** - 业务异常基类
4. **RetryConfig.java** - 重试配置（指数退避）
5. **ErrorResponse.java** (已更新) - 错误响应 DTO
6. **GlobalExceptionHandler.java** (已更新) - 全局异常处理器

### 文档
1. **ERROR_CODES.md** - 完整错误码文档
2. **RETRY_USAGE_EXAMPLE.md** - 重试机制使用示例
3. **ERROR_HANDLING_QUICKSTART.md** - 本文件

### 依赖更新
- **pom.xml** - 添加了 Spring Retry 和 AOP 依赖

---

## 🚀 快速使用

### 1. 抛出业务异常

```java
// 简单使用
throw new BusinessException(ErrorCode.BIO_TOO_SHORT);

// 自定义消息
throw new BusinessException(
    ErrorCode.PHOTO_TOO_MANY,
    "You provided " + photos.size() + " photos"
);
```

### 2. 抛出 Claude API 异常

```java
// 根据 HTTP 状态码自动创建
throw ClaudeApiException.fromHttpStatus(429, "Rate limit exceeded");

// 直接使用错误码
throw new ClaudeApiException(ErrorCode.CLAUDE_TIMEOUT);
```

### 3. 使用重试机制（推荐方式）

```java
@Service
@RequiredArgsConstructor
public class MyService {

    private final RetryTemplate claudeApiRetryTemplate;

    public String callApi(String input) {
        return claudeApiRetryTemplate.execute(context -> {
            // 你的 Claude API 调用逻辑
            // 如果抛出可重试的异常，会自动重试
            return makeApiCall(input);
        });
    }
}
```

### 4. 使用注解方式重试

```java
@Service
public class MyService {

    @Retryable(
        value = ClaudeApiException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String callApi(String input) {
        // 抛出 ClaudeApiException 时会自动重试
        return makeApiCall(input);
    }

    @Recover
    public String recover(ClaudeApiException e, String input) {
        // 重试失败后的降级逻辑
        log.error("Failed after retries", e);
        throw new BusinessException(ErrorCode.RETRY_EXHAUSTED);
    }
}
```

---

## 📊 错误响应格式

客户端收到的错误响应：

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

---

## 🔢 常用错误码

| 错误码 | 名称 | 说明 | 可重试 |
|--------|------|------|--------|
| 1000 | BAD_REQUEST | 请求参数无效 | ❌ |
| 1100 | BIO_TOO_SHORT | Bio 太短 | ❌ |
| 1101 | BIO_TOO_LONG | Bio 太长 | ❌ |
| 2001 | PHOTO_TOO_FEW | 照片太少 | ❌ |
| 2002 | PHOTO_TOO_MANY | 照片太多 | ❌ |
| 4001 | CLAUDE_API_KEY_INVALID | API 密钥无效 | ❌ |
| 4002 | CLAUDE_RATE_LIMIT | 速率限制 | ✅ |
| 4005 | CLAUDE_TIMEOUT | 请求超时 | ✅ |
| 4006 | CLAUDE_OVERLOADED | 服务过载 | ✅ |
| 9000 | SYSTEM_ERROR | 系统错误 | ❌ |

完整错误码列表请查看 [ERROR_CODES.md](ERROR_CODES.md)

---

## ⚙️ 重试配置说明

### Claude API 重试策略

- **最大重试次数**: 3 次
- **初始延迟**: 1 秒
- **延迟倍数**: 2 (指数退避)
- **最大延迟**: 10 秒

**重试时间表:**
- 第 1 次失败 → 1 秒后重试
- 第 2 次失败 → 2 秒后重试
- 第 3 次失败 → 4 秒后重试

### 可重试的错误

只有以下错误会自动重试：
- `CLAUDE_RATE_LIMIT` (429)
- `CLAUDE_TIMEOUT` (408)
- `CLAUDE_OVERLOADED` (503)
- `CLAUDE_SERVER_ERROR` (500/502/504)
- `CLAUDE_CONNECTION_ERROR` (连接失败)

---

## 📝 实际使用示例

### Service 层示例

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class BioService {

    private final RetryTemplate claudeApiRetryTemplate;
    private final RestTemplate restTemplate;

    @Value("${claude.api.key}")
    private String apiKey;

    public BioRewriteResponse rewriteBio(BioRewriteRequest request) {
        // 1. 参数验证 - 抛出 BusinessException
        validateBioRequest(request);

        // 2. 带重试的 API 调用 - 自动处理 ClaudeApiException
        return claudeApiRetryTemplate.execute(context -> {
            log.info("Calling Claude API (attempt {})", context.getRetryCount() + 1);
            return callClaudeApi(request);
        });
    }

    private void validateBioRequest(BioRewriteRequest request) {
        if (request.getBioText() == null || request.getBioText().length() < 10) {
            throw new BusinessException(ErrorCode.BIO_TOO_SHORT);
        }
        if (request.getBioText().length() > 500) {
            throw new BusinessException(ErrorCode.BIO_TOO_LONG);
        }
    }

    private BioRewriteResponse callClaudeApi(BioRewriteRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 构建请求
            Map<String, Object> body = buildApiRequest(request);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // 调用 API
            ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.anthropic.com/v1/messages",
                entity,
                Map.class
            );

            // 解析响应
            return parseResponse(response.getBody());

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // HTTP 错误 → ClaudeApiException (可能触发重试)
            throw ClaudeApiException.fromHttpStatus(
                e.getStatusCode().value(),
                e.getMessage()
            );
        } catch (ResourceAccessException e) {
            // 连接错误 → ClaudeApiException (触发重试)
            throw new ClaudeApiException(
                ErrorCode.CLAUDE_CONNECTION_ERROR,
                "Failed to connect to Claude API",
                e
            );
        }
    }

    private Map<String, Object> buildApiRequest(BioRewriteRequest request) {
        return Map.of(
            "model", "claude-3-sonnet-20240229",
            "messages", List.of(Map.of(
                "role", "user",
                "content", "Rewrite this bio: " + request.getBioText()
            )),
            "max_tokens", 500
        );
    }

    private BioRewriteResponse parseResponse(Map<String, Object> body) {
        try {
            List<Map<String, Object>> content =
                (List<Map<String, Object>>) body.get("content");
            String text = (String) content.get(0).get("text");
            return new BioRewriteResponse(text);
        } catch (Exception e) {
            throw new ClaudeApiException(
                ErrorCode.CLAUDE_RESPONSE_PARSE_ERROR,
                "Failed to parse API response",
                e
            );
        }
    }
}
```

---

## 🧪 测试

### 单元测试示例

```java
@SpringBootTest
class BioServiceTest {

    @Autowired
    private BioService bioService;

    @Test
    void testBioTooShort() {
        BioRewriteRequest request = new BioRewriteRequest("short");

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> bioService.rewriteBio(request)
        );

        assertEquals(ErrorCode.BIO_TOO_SHORT, exception.getErrorCode());
    }

    @Test
    void testRetryOnTimeout() {
        // 测试重试机制
        // ...
    }
}
```

---

## 📚 更多信息

- **完整错误码列表**: [ERROR_CODES.md](ERROR_CODES.md)
- **重试机制详细示例**: [RETRY_USAGE_EXAMPLE.md](RETRY_USAGE_EXAMPLE.md)

---

## ✅ 下一步

1. **运行 `mvn clean install`** 安装新依赖
2. **在 Service 中使用错误码** 替换原有的异常抛出
3. **测试重试机制** 模拟 Claude API 失败场景
4. **配置监控告警** 对关键错误码进行监控

---

## 💡 最佳实践

### ✅ DO
- 使用明确的错误码而不是通用异常
- 为可重试的错误使用 RetryTemplate
- 在日志中记录错误码和上下文信息
- 在文档中说明每个错误的处理方式

### ❌ DON'T
- 不要捕获异常后不处理
- 不要对不可重试的错误使用重试
- 不要在业务异常中包含敏感信息
- 不要使用魔法数字，始终使用 ErrorCode 枚举

# 重试机制使用示例

## 目录
1. [使用 RetryTemplate (推荐)](#使用-retrytemplate-推荐)
2. [使用 @Retryable 注解](#使用-retryable-注解)
3. [手动重试逻辑](#手动重试逻辑)
4. [测试重试机制](#测试重试机制)

---

## 使用 RetryTemplate (推荐)

### 基本用法

```java
@Service
@RequiredArgsConstructor
public class ClaudeApiService {

    private final RetryTemplate claudeApiRetryTemplate;
    private final RestTemplate restTemplate;

    @Value("${claude.api.url}")
    private String claudeApiUrl;

    @Value("${claude.api.key}")
    private String apiKey;

    /**
     * 带重试的 Claude API 调用
     */
    public String callClaudeApi(String prompt) {
        return claudeApiRetryTemplate.execute(context -> {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("x-api-key", apiKey);
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> requestBody = Map.of(
                    "model", "claude-3-sonnet-20240229",
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "max_tokens", 1024
                );

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<Map> response = restTemplate.postForEntity(
                    claudeApiUrl + "/v1/messages",
                    entity,
                    Map.class
                );

                if (!response.getStatusCode().is2xxSuccessful()) {
                    throw ClaudeApiException.fromHttpStatus(
                        response.getStatusCode().value(),
                        "API call failed"
                    );
                }

                return extractContent(response.getBody());

            } catch (HttpClientErrorException | HttpServerErrorException e) {
                throw ClaudeApiException.fromHttpStatus(
                    e.getStatusCode().value(),
                    e.getMessage()
                );
            } catch (ResourceAccessException e) {
                throw new ClaudeApiException(
                    ErrorCode.CLAUDE_CONNECTION_ERROR,
                    "Failed to connect to Claude API",
                    e
                );
            }
        });
    }

    private String extractContent(Map<String, Object> responseBody) {
        // 解析响应内容
        List<Map<String, Object>> content = (List<Map<String, Object>>) responseBody.get("content");
        if (content != null && !content.isEmpty()) {
            return (String) content.get(0).get("text");
        }
        throw new ClaudeApiException(ErrorCode.CLAUDE_RESPONSE_PARSE_ERROR);
    }
}
```

### 带回调的重试

```java
@Service
@RequiredArgsConstructor
public class PhotoRankingService {

    private final RetryTemplate claudeApiRetryTemplate;

    public PhotoRankingResponse rankPhotos(PhotoRankingRequest request) {
        return claudeApiRetryTemplate.execute(
            // 主要逻辑
            context -> {
                log.info("Ranking photos, attempt: {}", context.getRetryCount() + 1);
                return callClaudeApiForRanking(request);
            },
            // 重试耗尽后的恢复逻辑
            context -> {
                log.error("Photo ranking failed after {} attempts", context.getRetryCount());
                throw new BusinessException(
                    ErrorCode.PHOTO_ANALYSIS_FAILED,
                    "Failed to rank photos after multiple attempts"
                );
            }
        );
    }

    private PhotoRankingResponse callClaudeApiForRanking(PhotoRankingRequest request) {
        // 实际的 API 调用逻辑
        // ...
    }
}
```

---

## 使用 @Retryable 注解

### 配置 @EnableRetry

```java
@Configuration
@EnableRetry
public class RetryConfig {
    // 已在 RetryConfig.java 中配置
}
```

### 基本注解使用

```java
@Service
@Slf4j
public class BioService {

    @Autowired
    private ClaudeApiClient claudeApiClient;

    /**
     * 使用注解方式实现重试
     * - maxAttempts: 最大尝试次数 (包括首次调用)
     * - backoff: 退避策略
     *   - delay: 初始延迟 (毫秒)
     *   - multiplier: 延迟倍数
     *   - maxDelay: 最大延迟 (毫秒)
     */
    @Retryable(
        value = {ClaudeApiException.class},
        maxAttempts = 3,
        backoff = @Backoff(
            delay = 1000,
            multiplier = 2,
            maxDelay = 10000
        )
    )
    public BioRewriteResponse rewriteBio(BioRewriteRequest request) {
        log.info("Calling Claude API to rewrite bio");

        // 验证输入
        validateBioRequest(request);

        // 调用 Claude API
        return claudeApiClient.rewriteBio(request);
    }

    /**
     * 重试耗尽后的恢复方法
     * - 方法签名必须与原方法匹配（除了返回值可以不同）
     * - 第一个参数必须是异常类型
     */
    @Recover
    public BioRewriteResponse recoverBioRewrite(
            ClaudeApiException e,
            BioRewriteRequest request) {

        log.error("Failed to rewrite bio after retries. Error: {}", e.getMessage());

        // 根据错误类型决定恢复策略
        if (e.getErrorCode() == ErrorCode.CLAUDE_RATE_LIMIT) {
            throw new BusinessException(
                ErrorCode.BIO_GENERATION_FAILED,
                "AI service is temporarily busy, please try again later"
            );
        }

        if (e.getErrorCode() == ErrorCode.CLAUDE_QUOTA_EXCEEDED) {
            throw new BusinessException(
                ErrorCode.BIO_GENERATION_FAILED,
                "AI service quota exceeded, please contact support"
            );
        }

        throw new BusinessException(
            ErrorCode.BIO_GENERATION_FAILED,
            "Failed to generate bio rewrite: " + e.getMessage()
        );
    }

    private void validateBioRequest(BioRewriteRequest request) {
        if (request.getBioText() == null || request.getBioText().length() < 10) {
            throw new BusinessException(ErrorCode.BIO_TOO_SHORT);
        }
        if (request.getBioText().length() > 500) {
            throw new BusinessException(ErrorCode.BIO_TOO_LONG);
        }
    }
}
```

### 条件重试

```java
@Service
public class ConversationStarterService {

    /**
     * 只重试特定的可重试异常
     */
    @Retryable(
        include = {ClaudeApiException.class},
        exclude = {BusinessException.class},
        maxAttempts = 3
    )
    public ConversationStarterResponse generateStarters(ConversationStarterRequest request) {
        // 业务异常不会触发重试
        if (request.getMatchProfile() == null) {
            throw new BusinessException(ErrorCode.CONV_INVALID_INPUT);
        }

        // Claude API 异常会触发重试
        return claudeApiClient.generateConversationStarters(request);
    }
}
```

---

## 手动重试逻辑

### 适用场景

当需要更细粒度的控制时，可以手动实现重试逻辑：

```java
@Service
@Slf4j
public class ManualRetryService {

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_DELAY_MS = 1000;
    private static final double MULTIPLIER = 2.0;

    /**
     * 手动实现指数退避重试
     */
    public String callWithManualRetry(String prompt) {
        int attempt = 0;
        long delay = INITIAL_DELAY_MS;

        while (attempt < MAX_RETRIES) {
            try {
                attempt++;
                log.info("Attempt {} of {}", attempt, MAX_RETRIES);

                return callClaudeApi(prompt);

            } catch (ClaudeApiException e) {
                // 如果不可重试，直接抛出
                if (!e.isRetryable()) {
                    log.error("Non-retryable error: {}", e.getMessage());
                    throw e;
                }

                // 如果已达到最大重试次数，抛出
                if (attempt >= MAX_RETRIES) {
                    log.error("Max retries ({}) exceeded", MAX_RETRIES);
                    throw new ClaudeApiException(
                        ErrorCode.RETRY_EXHAUSTED,
                        "Failed after " + MAX_RETRIES + " attempts",
                        e
                    );
                }

                // 计算延迟并等待
                log.warn("Attempt {} failed, retrying after {}ms. Error: {}",
                    attempt, delay, e.getMessage());

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ClaudeApiException(ErrorCode.SYSTEM_ERROR, ie);
                }

                // 指数增长延迟时间
                delay = (long) (delay * MULTIPLIER);
            }
        }

        throw new ClaudeApiException(ErrorCode.RETRY_EXHAUSTED);
    }

    private String callClaudeApi(String prompt) {
        // 实际的 API 调用
        // ...
        return "response";
    }
}
```

---

## 测试重试机制

### 单元测试

```java
@SpringBootTest
class RetryMechanismTest {

    @Autowired
    private RetryTemplate claudeApiRetryTemplate;

    @Mock
    private ClaudeApiClient mockApiClient;

    @Test
    void testRetryOnRateLimitError() {
        // 模拟前两次调用失败，第三次成功
        when(mockApiClient.callApi(anyString()))
            .thenThrow(new ClaudeApiException(ErrorCode.CLAUDE_RATE_LIMIT))
            .thenThrow(new ClaudeApiException(ErrorCode.CLAUDE_RATE_LIMIT))
            .thenReturn("success");

        // 执行带重试的调用
        String result = claudeApiRetryTemplate.execute(context ->
            mockApiClient.callApi("test")
        );

        // 验证结果
        assertEquals("success", result);
        // 验证调用了3次
        verify(mockApiClient, times(3)).callApi("test");
    }

    @Test
    void testNoRetryOnNonRetryableError() {
        // 模拟不可重试的错误
        when(mockApiClient.callApi(anyString()))
            .thenThrow(new ClaudeApiException(ErrorCode.CLAUDE_API_KEY_INVALID));

        // 验证不会重试
        assertThrows(ClaudeApiException.class, () ->
            claudeApiRetryTemplate.execute(context ->
                mockApiClient.callApi("test")
            )
        );

        // 验证只调用了1次
        verify(mockApiClient, times(1)).callApi("test");
    }

    @Test
    void testMaxRetriesExceeded() {
        // 模拟持续失败
        when(mockApiClient.callApi(anyString()))
            .thenThrow(new ClaudeApiException(ErrorCode.CLAUDE_TIMEOUT));

        // 验证重试3次后抛出异常
        assertThrows(ClaudeApiException.class, () ->
            claudeApiRetryTemplate.execute(context ->
                mockApiClient.callApi("test")
            )
        );

        // 验证调用了3次（最大重试次数）
        verify(mockApiClient, times(3)).callApi("test");
    }
}
```

### 集成测试

```java
@SpringBootTest
@AutoConfigureMockMvc
class RetryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClaudeApiClient mockApiClient;

    @Test
    void testBioRewriteWithRetry() throws Exception {
        // 模拟第一次失败，第二次成功
        when(mockApiClient.rewriteBio(any()))
            .thenThrow(new ClaudeApiException(ErrorCode.CLAUDE_TIMEOUT))
            .thenReturn(new BioRewriteResponse("Rewritten bio"));

        // 发送请求
        mockMvc.perform(post("/api/v1/profile/bio/rewrite")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bioText\":\"Original bio\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rewrittenBio").value("Rewritten bio"));

        // 验证重试了2次
        verify(mockApiClient, times(2)).rewriteBio(any());
    }
}
```

---

## 最佳实践

### 1. 选择合适的重试策略

- **快速失败场景**: 用户输入错误、API Key 无效 → **不重试**
- **瞬时故障**: 网络超时、服务过载 → **重试 2-3 次**
- **限流场景**: Rate limit → **重试 + 指数退避**

### 2. 设置合理的超时时间

```java
@Bean
public RestTemplate restTemplate() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5000);  // 连接超时 5 秒
    factory.setReadTimeout(30000);    // 读取超时 30 秒
    return new RestTemplate(factory);
}
```

### 3. 记录重试日志

```java
retryTemplate.registerListener(new RetryListener() {
    @Override
    public <T, E extends Throwable> void onError(
            RetryContext context,
            RetryCallback<T, E> callback,
            Throwable throwable) {

        log.warn("Retry attempt {}/{} failed: {}",
            context.getRetryCount() + 1,
            maxAttempts,
            throwable.getMessage());
    }
});
```

### 4. 监控重试指标

```java
@Component
public class RetryMetrics {

    private final MeterRegistry meterRegistry;

    public void recordRetry(String operation, int attemptNumber) {
        meterRegistry.counter("api.retry",
            "operation", operation,
            "attempt", String.valueOf(attemptNumber)
        ).increment();
    }
}
```

### 5. 避免雪崩效应

```java
// 添加断路器
@CircuitBreaker(name = "claudeApi", fallbackMethod = "fallback")
@Retryable(...)
public String callClaudeApi(String prompt) {
    // ...
}

public String fallback(String prompt, Exception e) {
    return "Service temporarily unavailable";
}
```

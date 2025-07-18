# 產品需求文檔（PRD）
## 增你強-恩及通WMS系統整合專案

### 1. 專案概述

#### 1.1 專案背景
增你強公司需要與恩及通（JIT）倉儲管理系統（WMS）進行系統整合，透過調用JIT提供的API接口實現倉儲管理的自動化和數據同步。

#### 1.2 專案目標
- 實現與恩及通WMS系統的無縫對接
- 自動化倉儲管理流程，包括入庫、移倉、換料、庫存查詢等
- 確保數據的準確性和實時性
- 提升倉儲管理效率

#### 1.3 技術架構
- **開發框架**: Java Spring Boot
- **數據庫**: Oracle DB
- **通訊協議**: HTTPS
- **數據格式**: JSON (application/json)
- **認證方式**: Token-based authentication
- **API基礎URL**: https://gt-test.njtsys.com/api/

### 2. 功能需求

#### 2.1 身份認證模組

##### 2.1.1 API登錄（入方向）
**功能描述**: 獲取訪問其他API所需的auth-token

**接口規格**:
- **URL**: `/comm/auth/api-login`
- **Method**: POST
- **請求參數**:
  ```json
  {
    "userName": "zcsh_api",
    "password": "qW3$rT9@K6#fL2!P"
  }
  ```
- **響應格式**:
  ```json
  {
    "userName": "API用戶名",
    "appToken": "後續訪問所需的auth-token"
  }
  ```

**實現要求**:
- 實現自動登錄機制
- Token過期自動重新獲取
- Token安全存儲

#### 2.2 入庫管理模組

##### 2.2.1 創建入庫單
**功能描述**: 向WMS系統提交入庫單，創建預定進貨通知單

**接口規格**:
- **URL**: `/wms/b2b-api/create-asn-by-api`
- **Method**: POST
- **主要參數**:
  - 倉庫信息（WhName, StorerAbbrName）
  - 業務類型（BizType）
  - 入庫明細（Lines）
  - 料號信息（SkuInfo）
  - 批次屬性（LotAttr01-20）

**業務邏輯**:
- 支援多儲區管理：
  - ZSH 基通.上海倉
  - ZSH 基通.不良倉
  - ZSH 基通.加工倉
  - SHC 基通.IT
- 支援批次管理和追溯
- 支援自定義屬性擴展

#### 2.3 庫存操作模組

##### 2.3.1 庫內移倉/交易
**功能描述**: 實現庫存在不同賬本或儲區間的移動和交易

**接口規格**:
- **URL**: `/project/b2b-api/inv-move-or-trade`
- **Method**: POST
- **交易類型**:
  - move: 庫內移倉
  - tran: 庫存交易

**業務規則**:
- 移倉時來源賬本與目的賬本需保持一致
- 交易時支援跨賬本操作
- 支援指定批次屬性（Lot, Batch, DateCode, Coo）

##### 2.3.2 庫內換料
**功能描述**: 支援料號轉換，包括組裝、拆解、換料等操作

**接口規格**:
- **URL**: `/project/b2b-api/inv-exchange-sku`
- **Method**: POST
- **換料類型**:
  - Combine: 組裝（1:N）
  - Separate: 拆解（1:N）
  - Exchange: 換料（1:1）

**業務邏輯**:
- Exchange模式：成品與原材料1:1對應
- 其他模式：支援1:N的轉換關係
- 必須指定儲區（ZoneName）

#### 2.4 庫存查詢模組

##### 2.4.1 庫存查詢
**功能描述**: 提供多維度的庫存查詢功能

**接口規格**:
- **URL**: `/project/b2b-api/get-inv-loc-list`
- **Method**: POST
- **查詢條件**:
  - 倉庫名稱（WhName）
  - 儲區名稱（ZoneName）
  - 貨主名稱（StorerAbbrName）
  - 料號（Sku）

**返回信息**:
- 庫存基本信息
- 數量信息（庫存量、可用量、凍結量等）
- 批次屬性（20個自定義屬性）
- 時間信息（入庫日期、庫齡等）

**限制條件**:
- 單次查詢返回上限：5000筆

### 3. 非功能性需求

#### 3.1 性能要求
- API響應時間 < 3秒
- 支援並發請求處理
- 批量數據處理能力（如批量入庫單創建）

#### 3.2 安全要求
- HTTPS加密傳輸
- Token認證機制
- 敏感信息加密存儲
- API訪問日誌記錄

#### 3.3 可靠性要求
- 錯誤重試機制
- 異常處理和錯誤日誌
- 數據一致性保證
- 事務回滾機制

#### 3.4 可維護性要求
- 模組化設計
- 統一的錯誤處理
- 完整的API文檔
- 單元測試覆蓋

### 4. 系統設計建議

#### 4.1 架構設計
```
┌─────────────────┐
│   應用層        │
│  (業務邏輯)     │
├─────────────────┤
│   服務層        │
│  (API封裝)      │
├─────────────────┤
│   基礎層        │
│  (HTTP/認證)    │
└─────────────────┘
```

#### 4.2 核心模組
1. **認證管理器（AuthManager）**
   - Token獲取和刷新
   - 請求頭自動注入

2. **API客戶端（ApiClient）**
   - 統一的請求/響應處理
   - 錯誤處理和重試

3. **業務服務（Services）**
   - 入庫服務（InboundService）
   - 庫存服務（InventoryService）
   - 查詢服務（QueryService）

4. **數據模型（Models）**
   - 請求/響應DTO
   - 業務實體

#### 4.3 錯誤處理
- 統一的錯誤碼定義
- 友好的錯誤提示
- 詳細的錯誤日誌

### 5. 開發計劃

#### 5.1 開發階段
1. **Phase 1: 基礎架構** (1週)
   - 項目初始化
   - 基礎框架搭建
   - 認證模組實現

2. **Phase 2: 核心功能** (2週)
   - 入庫單創建
   - 庫存操作（移倉/交易/換料）
   - 庫存查詢

3. **Phase 3: 測試優化** (1週)
   - 單元測試
   - 集成測試
   - 性能優化

4. **Phase 4: 部署上線** (3天)
   - UAT測試
   - 生產環境部署
   - 監控配置

#### 5.2 里程碑
- M1: 完成認證和基礎架構
- M2: 完成所有API整合
- M3: 通過UAT測試
- M4: 生產環境上線

### 6. 風險評估

#### 6.1 技術風險
- API版本變更風險
- 網絡不穩定
- 並發處理能力

#### 6.2 業務風險
- 數據不一致
- 系統整合複雜度
- 業務流程變更

#### 6.3 緩解措施
- 版本控制和向後兼容
- 實現重試和補償機制
- 完整的測試覆蓋
- 詳細的操作日誌

### 7. 附錄

#### 7.1 數據類型說明
| 類型 | 格式 | 說明 |
|------|------|------|
| bool | true/false | 布爾值 |
| dateTime | YYYY-MM-DDTHH:mm:ss | 時間格式 |
| string | "value" | 字符串 |
| int | 123 | 整數 |
| decimal | 123.45 | 小數 |

#### 7.2 常用代碼示例
```java
// Spring Boot 認證示例
@Service
public class AuthService {
    @Autowired
    private RestTemplate restTemplate;
    
    private static final String LOGIN_URL = "https://gt-test.njtsys.com/api/comm/auth/api-login";
    
    public String login() {
        Map<String, String> request = new HashMap<>();
        request.put("userName", "zcsh_api");
        request.put("password", "qW3$rT9@K6#fL2!P");
        
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
            LOGIN_URL, 
            request, 
            LoginResponse.class
        );
        
        return response.getBody().getAppToken();
    }
}

// API調用示例
@Service
public class WmsApiService {
    @Autowired
    private RestTemplate restTemplate;
    
    private static final String CREATE_ASN_URL = "https://gt-test.njtsys.com/api/wms/b2b-api/create-asn-by-api";
    
    public String createAsn(String token, CreateAsnRequest asnData) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("username", "zcsh_api");
        headers.set("auth-token", token);
        
        HttpEntity<CreateAsnRequest> request = new HttpEntity<>(asnData, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            CREATE_ASN_URL,
            request,
            String.class
        );
        
        return response.getBody();
    }
}

### 8. 聯絡資訊

**專案負責人**: Jeanny Chiu 
**技術聯絡人**: Jeanny Chiu
**JIT接口支援**: Aphil Wu

---

*文檔版本: 1.0*  
*更新日期: 2025-07-18*
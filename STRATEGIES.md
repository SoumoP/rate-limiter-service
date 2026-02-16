# Rate Limiting Strategies - Visual Comparison

## 1. Token Bucket Algorithm

```
Time:    0s    1s    2s    3s    4s    5s
         │     │     │     │     │     │
Tokens:  10 → 11 → 12 → 13 → 14 → 15
         ↓     ↓     ↓     ↓     ↓     ↓
Request: ✓(9)  ✓(8)  ✓(7)  ✓(6)  ✓(5)  ✓(4)

Capacity: 10 tokens
Refill Rate: 1 token/second
```

**Burst Scenario:**
```
Time:    0s              1s
         │               │
Tokens:  10              1
         ↓↓↓↓↓↓↓↓↓↓      ↓
Request: ✓✓✓✓✓✓✓✓✓✓      ✓ (11th request succeeds after 1s)
```

## 2. Fixed Window Counter

```
Window:  [─────── 60s ───────][─────── 60s ───────]
         0s                 60s                 120s
Count:   1 2 3 4 5 ... 100   1 2 3 4 5 ... 100
         ✓ ✓ ✓ ✓ ✓ ... ✓     ✓ ✓ ✓ ✓ ✓ ... ✓
                          ✗                      ✗
```

**Boundary Burst Problem:**
```
Window:  [─────── 60s ───────][─────── 60s ───────]
         0s                 60s                 120s
                        59s  │  61s
Count:                  100  │  100
Request:                ✓✓✓  │  ✓✓✓
                             │
         200 requests in 2 seconds! (at boundary)
```

## 3. Sliding Window Log

```
Window Size: 60 seconds
Current Time: 100s

Request Log (timestamps):
[41, 42, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100]
 ✗   ✗   ✓   ✓   ✓   ✓   ✓   ✓   ✓   ✓   ✓   ✓   ✓   ✓
 │   │   └──────────── Within window ────────────┘
 └───┴─── Removed (too old)

Allowed: 12 requests (within 60s window)
```

**Sliding Behavior:**
```
Time →
  40s: [log: 41,42,45,50,55,60...95,100] → Count: 14
  50s: [log: 50,55,60...95,100,105,110]  → Count: 12
  60s: [log: 60,65,70...110,115,120]     → Count: 11
```

## 4. Sliding Window Counter

```
Previous Window    Current Window
[──── 60s ────]   [──── 60s ────]
Count: 80         Count: 30
       │                 │
       └─────────┬───────┘
                 │
         Weighted Count = (80 × 40%) + 30 = 62
         
Time position: 36s into current window (60%)
Weight of previous window: 40%
```

**Calculation Example:**
```
Window: 0s ────── 60s ────── 120s
              ↑
         Current Time: 84s (40% into window)
         
Previous Window (0-60s): 75 requests
Current Window (60-120s): 25 requests

Weighted Count = 75 × (1 - 0.4) + 25
               = 75 × 0.6 + 25
               = 45 + 25
               = 70 requests
```

## 5. Leaky Bucket Algorithm

```
Bucket Capacity: 10
Leak Rate: 1 request/second

Time:    0s    1s    2s    3s    4s
         │     │     │     │     │
Level:   0→1→0 0→1→0 0→1→0 0→1→0
         ↑ ↓   ↑ ↓   ↑ ↓   ↑ ↓
         │ │   │ │   │ │   │ │
Request: ✓ leak ✓ leak ✓ leak ✓ leak
```

**Bucket Full Scenario:**
```
Time:    0s              1s
         │               │
Level:   0→10            10→9
         ↑↑↑↑↑↑↑↑↑↑      ↓ ↑
         │││││││││└─full │leak,add
Request: ✓✓✓✓✓✓✓✓✓✓      ✗✓
         
10 requests fill bucket, 11th rejected
After 1s: leak 1, add 1
```

## Algorithm Comparison Matrix

| Aspect | Token Bucket | Fixed Window | Sliding Log | Sliding Counter | Leaky Bucket |
|--------|--------------|--------------|-------------|-----------------|--------------|
| **Burst Handling** | ✅ Excellent | ⚠️ Boundary issue | ✅ No bursts | ✅ Controlled | ❌ No bursts |
| **Accuracy** | ✅ High | ⚠️ Medium | ✅ Very High | ✅ High | ✅ High |
| **Memory** | 🟢 O(1) | 🟢 O(1) | 🔴 O(n) | 🟢 O(1) | 🟢 O(1) |
| **Complexity** | 🟡 Medium | 🟢 Low | 🟡 Medium | 🟡 Medium | 🟡 Medium |
| **Reset Timing** | Continuous | Window boundary | Continuous | Continuous | Continuous |

## Use Case Recommendations

### Token Bucket ✅
- **Best for:** General API rate limiting
- **Examples:** 
  - REST APIs with occasional bursts
  - Mobile app API calls
  - Third-party API integrations
- **Why:** Balances strictness with flexibility

### Fixed Window Counter ✅
- **Best for:** Simple counting scenarios
- **Examples:**
  - Daily email limits
  - Hourly report generation
  - Simple quota systems
- **Why:** Easy to implement and understand

### Sliding Window Log ✅
- **Best for:** Critical, precise rate limiting
- **Examples:**
  - Financial transactions
  - Payment processing
  - Security-critical APIs
- **Why:** Most accurate, no boundary issues

### Sliding Window Counter ✅
- **Best for:** Balanced general-purpose limiting
- **Examples:**
  - Multi-tenant SaaS platforms
  - Public APIs with fair usage
  - E-commerce checkouts
- **Why:** Good accuracy without high memory cost

### Leaky Bucket ✅
- **Best for:** Smooth, constant output rate
- **Examples:**
  - Message queue processing
  - Batch job execution
  - Protecting downstream services
- **Why:** Guarantees constant processing rate

## Real-World Scenarios

### Scenario 1: Social Media API
**Challenge:** Handle bursts when users post multiple updates

**Solution:** Token Bucket
- Capacity: 50 posts
- Refill: 10 posts/hour
- Allows burst of 50 posts, then refills gradually

### Scenario 2: Payment Gateway
**Challenge:** Prevent duplicate transactions, ensure accuracy

**Solution:** Sliding Window Log
- Limit: 100 transactions/hour
- Zero tolerance for boundary bursts
- Precise tracking of all transactions

### Scenario 3: Email Service
**Challenge:** Simple daily email limit per user

**Solution:** Fixed Window Counter
- Limit: 1000 emails/day
- Resets at midnight
- Simple to track and display

### Scenario 4: Microservice Protection
**Challenge:** Protect downstream service from overload

**Solution:** Leaky Bucket
- Capacity: 100 requests
- Leak rate: 10 requests/second
- Smooth, constant load on downstream

### Scenario 5: Multi-Tier API
**Challenge:** Different limits for free/premium users

**Solution:** Sliding Window Counter
- Free: 100 requests/hour
- Premium: 10,000 requests/hour
- Accurate, memory efficient for both tiers

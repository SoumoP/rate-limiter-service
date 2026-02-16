#!/bin/bash

# Rate Limiter Service Test Script
# This script demonstrates all rate limiting strategies

BASE_URL="http://localhost:8080"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║       Rate Limiter Service - Strategy Demonstration           ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Check if service is running
echo "🔍 Checking if service is running..."
if ! curl -s "${BASE_URL}/api/health" > /dev/null 2>&1; then
    echo "❌ Service is not running. Please start it first:"
    echo "   mvn spring-boot:run"
    exit 1
fi
echo "✅ Service is running!"
echo ""

# Function to test endpoint
test_endpoint() {
    local endpoint=$1
    local name=$2
    local requests=$3
    
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Testing: $name"
    echo "Endpoint: $endpoint"
    echo "Sending $requests requests..."
    echo ""
    
    for i in $(seq 1 $requests); do
        response=$(curl -s -w "\n%{http_code}" "${BASE_URL}${endpoint}")
        http_code=$(echo "$response" | tail -n 1)
        body=$(echo "$response" | head -n -1)
        
        if [ "$http_code" -eq 200 ]; then
            echo "✅ Request $i: SUCCESS"
        elif [ "$http_code" -eq 429 ]; then
            retry_after=$(echo "$body" | grep -o '"retryAfterSeconds":[0-9]*' | cut -d':' -f2)
            echo "🛑 Request $i: RATE LIMITED (Retry after: ${retry_after}s)"
        else
            echo "❌ Request $i: ERROR (HTTP $http_code)"
        fi
        
        sleep 0.2
    done
    echo ""
}

# Test 1: Token Bucket
test_endpoint "/api/token-bucket" "Token Bucket Strategy" 8

# Test 2: Fixed Window Counter
test_endpoint "/api/fixed-window" "Fixed Window Counter Strategy" 8

# Test 3: Sliding Window Log
test_endpoint "/api/sliding-log" "Sliding Window Log Strategy" 8

# Test 4: Sliding Window Counter
test_endpoint "/api/sliding-counter" "Sliding Window Counter Strategy" 8

# Test 5: Leaky Bucket
test_endpoint "/api/leaky-bucket" "Leaky Bucket Strategy" 8

# Test 6: Per-User Rate Limiting
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Testing: Per-User Rate Limiting"
echo "Testing multiple users with different limits..."
echo ""

for user in alice bob charlie; do
    echo "User: $user"
    for i in $(seq 1 3); do
        response=$(curl -s -w "\n%{http_code}" "${BASE_URL}/api/user/${user}")
        http_code=$(echo "$response" | tail -n 1)
        
        if [ "$http_code" -eq 200 ]; then
            echo "  ✅ Request $i: SUCCESS"
        else
            echo "  🛑 Request $i: RATE LIMITED"
        fi
    done
    echo ""
done

# Test 7: Programmatic API
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Testing: Programmatic API"
echo "Checking rate limit status..."
echo ""

response=$(curl -s -X POST "${BASE_URL}/api/check-limit?key=testuser" \
    -H "Content-Type: application/json" \
    -d '{
        "type": "TOKEN_BUCKET",
        "limit": 5,
        "windowSizeInSeconds": 60,
        "refillRate": 1.0,
        "capacity": 5
    }')

echo "$response" | jq '.' 2>/dev/null || echo "$response"
echo ""

# Test 8: Reset Rate Limiter
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Testing: Reset Rate Limiter"
echo "Resetting rate limiter for a specific key..."
echo ""

response=$(curl -s -X DELETE "${BASE_URL}/api/reset/TOKEN_BUCKET/testuser")
echo "$response" | jq '.' 2>/dev/null || echo "$response"
echo ""

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                   Test Summary                                 ║"
echo "╠════════════════════════════════════════════════════════════════╣"
echo "║ ✅ Token Bucket         - Allows bursts, refills constantly   ║"
echo "║ ✅ Fixed Window         - Simple, resets at boundaries        ║"
echo "║ ✅ Sliding Window Log   - Most accurate, memory intensive     ║"
echo "║ ✅ Sliding Window Ctr   - Balanced approach                   ║"
echo "║ ✅ Leaky Bucket         - Smooth output rate                  ║"
echo "║ ✅ Per-User Limiting    - Individual user quotas              ║"
echo "║ ✅ Programmatic API     - Custom rate limit checks            ║"
echo "║ ✅ Reset Functionality  - Clear rate limit state              ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
echo "💡 Tip: Wait a few seconds and run again to see token refill!"

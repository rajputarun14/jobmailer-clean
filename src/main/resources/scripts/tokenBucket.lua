local key = KEYS[1]

local capacity = tonumber(ARGV[1])
local refillRate = tonumber(ARGV[2])
local currentTime = tonumber(ARGV[3])
local requestedTokens = tonumber(ARGV[4])

local bucket = redis.call("HMGET", key,
        "tokens",
        "lastRefillTime")

local tokens = tonumber(bucket[1])
local lastRefillTime = tonumber(bucket[2])

if tokens == nil then
    tokens = capacity
    lastRefillTime = currentTime
end

local deltaTime =
    math.max(0, currentTime - lastRefillTime)

local refill =
    deltaTime * refillRate

tokens =
    math.min(capacity, tokens + refill)

local allowed = tokens >= requestedTokens

if allowed then
    tokens = tokens - requestedTokens
end

redis.call("HMSET", key,
    "tokens", tokens,
    "lastRefillTime", currentTime)

redis.call("EXPIRE", key, 3600)

return allowed
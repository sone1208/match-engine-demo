---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by HoWoo.
--- DateTime: 2023/9/2 23:48
---
local redisOrder = cjson.decode(redis.call("hget", "\"ORDER\"", KEYS[1]))
local order = redisOrder[2]

if order.status == 1 or order.status == 4
then
    order.status = ARGV[1]
    redisOrder[2] = order

    local json_order = cjson.encode(redisOrder)
    redis.call("hset", "\"ORDER\"", order.orderid, json_order)
    return true
else
    return false
end
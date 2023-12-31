---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by HoWoo.
--- DateTime: 2023/9/2 23:48
---

local redisSellOrder = cjson.decode(redis.call("hget", "\"ORDER\"", KEYS[1]))
local redisBuyOrder = cjson.decode(redis.call("hget", "\"ORDER\"", KEYS[2]))

local sellOrder = redisSellOrder[2]
local buyOrder = redisBuyOrder[2]

if (sellOrder.status == 1 or sellOrder.status == 4) and (buyOrder.status == 1 or buyOrder.status == 4)
then
    sellOrder.status = 2
    buyOrder.status = 2

    redisSellOrder[2] = sellOrder
    redisBuyOrder[2] = buyOrder

    local json_sellOrder = cjson.encode(redisSellOrder)
    local json_buyOrder = cjson.encode(redisBuyOrder)
    redis.call("hset", "\"ORDER\"", sellOrder.orderid, json_sellOrder)
    redis.call("hset", "\"ORDER\"", buyOrder.orderid, json_buyOrder)
    return true
else
    return false
end
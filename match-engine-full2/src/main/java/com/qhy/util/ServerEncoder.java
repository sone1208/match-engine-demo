package com.qhy.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.qhy.pojo.Results;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;

/**
 * websocket使用发送对象函数sendObject时必要的encoder设置
 */
public class ServerEncoder implements Encoder.Text<Results>{
    @Override
    public String encode(Results results) throws EncodeException {
        try {
            /*
             * 这里是重点，只需要返回Object序列化后的json字符串就行
             * 你也可以使用gosn，fastJson来序列化。
             */
            JsonMapper jsonMapper = new JsonMapper();
            return jsonMapper.writeValueAsString(results);

        } catch ( JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}

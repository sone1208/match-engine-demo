/**
 * 参数说明：
 *  webSocketURL：String    webSocket服务地址    eg: ws://127.0.0.1:8888/websocket (后端接口若为restful风格可以带参数)
 *  callback：为带一个参数的回调函数
 *  message：String 要传递的参数值（不是一个必要的参数）
 */
export default{
    // 初始化webSocket
    webSocketInit(webSocketURL){      // ws://127.0.0.1:8888/websocket
        this.webSocket = new WebSocket(webSocketURL);
        this.webSocket.onopen = this.onOpenwellback;
        this.webSocket.onmessage = this.onMessageCallback;
        this.webSocket.onerror = this.onErrorCallback;
        this.webSocket.onclose = this.onCloseCallback;
    },

    // 自定义回调函数
    setOpenCallback(callback){ //  与服务端连接打开回调函数
        this.webSocket.onopen = callback;
    },
    setMessageCallback(callback){   //  与服务端发送消息回调函数
        this.webSocket.onmessage = callback;
    },
    setErrorCallback(callback){ //  与服务端连接异常回调函数
        this.webSocket.onerror = callback;
    },
    setCloseCallback(callback){ //  与服务端连接关闭回调函数
        this.webSocket.onclose = callback;
    },

    close(){    // 关闭连接
        this.webSocket.close();
    },
    sendMessage(message){   // 发送消息函数
        this.webSocket.send(message);
    },
}

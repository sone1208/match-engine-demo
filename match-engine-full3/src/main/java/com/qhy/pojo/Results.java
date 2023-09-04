package com.qhy.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Results<T> {
    private String topic;
    private T resData;

    public Results() {
        this.topic = "";
    }

    public Results(T resData) {
        this.topic = "";
        this.resData = resData;
    }
}

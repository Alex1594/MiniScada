package com.CIMModel;

//基类：设备类
// Equipment类 表示所有的设备，可能包含一些通用属性，比如设备ID和名称。
public abstract class Equipment extends PowerSystemResource {
    private String id;
    private String name;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

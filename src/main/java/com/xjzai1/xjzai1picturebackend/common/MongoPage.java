package com.xjzai1.xjzai1picturebackend.common;

import lombok.Data;
import java.util.List;

/**
 * MongoDB 分页结果封装类
 */
@Data
public class MongoPage<T> {
    /**
     * 当前页数据
     */
    private List<T> records;
    
    /**
     * 总记录数
     */
    private long total;
    
    /**
     * 当前页码
     */
    private long current;
    
    /**
     * 每页大小
     */
    private long size;
    
    public MongoPage() {
    }
    
    public MongoPage(long current, long size) {
        this.current = current;
        this.size = size;
    }
    
    public MongoPage(long current, long size, long total) {
        this.current = current;
        this.size = size;
        this.total = total;
    }
    
    public MongoPage(List<T> records, long total, long current, long size) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
    }
}


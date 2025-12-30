package com.xjzai1.xjzai1picturebackend.utils;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Date;

/**
 * MongoDB 查询工具类
 */
public class MongoQueryUtils {

    /**
     * 添加相等条件
     */
    public static void addEq(Criteria criteria, String field, Object value) {
        if (ObjUtil.isNotEmpty(value)) {
            criteria.and(field).is(value);
        }
    }

    /**
     * 添加模糊查询条件
     */
    public static void addLike(Criteria criteria, String field, String value) {
        if (StrUtil.isNotBlank(value)) {
            criteria.and(field).regex(value, "i");
        }
    }

    /**
     * 添加范围查询条件
     */
    public static void addRange(Criteria criteria, String field, Date start, Date end) {
        if (start != null || end != null) {
            Criteria rangeCriteria = Criteria.where(field);
            if (start != null) {
                rangeCriteria.gte(start);
            }
            if (end != null) {
                rangeCriteria.lt(end);
            }
            criteria.andOperator(rangeCriteria);
        }
    }

    /**
     * 添加排序
     */
    public static void addSort(Query query, String sortField, String sortOrder) {
        if (StrUtil.isNotBlank(sortField)) {
            Sort.Direction direction = "ascend".equals(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
            query.with(Sort.by(direction, sortField));
        }
    }

    /**
     * 添加分页
     */
    public static void addPagination(Query query, long current, long pageSize) {
        query.with(PageRequest.of((int)current - 1, (int)pageSize));
    }

    /**
     * 添加逻辑删除过滤
     */
    public static void addLogicDelete(Criteria criteria) {
        criteria.and("isDelete").is(0);
    }
}


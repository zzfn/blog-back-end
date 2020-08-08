package com.api.jello.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author chenchen
 */
@Data
@TableName(value = "DYNAMIC_PROPERTY")
public class DynamicProperty extends BaseEntity {

    /**
     * 表id
     */
    private String tableId;
    /**
     * 字段名
     */
    private String propertyName;
    /**
     * 字段类型
     */
    private String propertyType;
}
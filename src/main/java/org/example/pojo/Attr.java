package org.example.pojo;

/**
 * @Description
 * @Author Efar <efarxs@163.com>
 * @Version V1.0.0
 * @Since 1.0
 * @Date 2023/12/23
 */

public class Attr {
    /**
     * 字段名
     */
    String name;
    /**
     * 字段类型
     */
    String type;
    /**
     * 约束条件
     */
    String constraint;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getConstraint() {
        return constraint;
    }

    public void setConstraint(String constraint) {
        this.constraint = constraint;
    }

    @Override
    public String toString() {
        return "Attr{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", constraint='" + constraint + '\'' +
                '}';
    }
}

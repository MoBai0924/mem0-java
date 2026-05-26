package cn.hsine.mem0.core.entityextractor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 实体关系三元组
 * subject, predicate, object
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityItem extends BaseEntity {
    //主体
    private String subject;
    // 关系
    private String predicate;
    // 客体
    private String object;
}

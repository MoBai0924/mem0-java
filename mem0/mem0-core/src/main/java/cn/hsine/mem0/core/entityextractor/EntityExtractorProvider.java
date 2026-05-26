package cn.hsine.mem0.core.entityextractor;

import java.util.List;

public interface EntityExtractorProvider<T extends  BaseEntity> {

    List<T> singleTextExtract(String text);

    List<List<T>> extractEntitiesBatch(List<String> textList);

}

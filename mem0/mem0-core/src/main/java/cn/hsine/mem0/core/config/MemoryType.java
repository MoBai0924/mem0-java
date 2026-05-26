package cn.hsine.mem0.core.config;

/**
 * Memory type enum, ported from Python mem0/configs/enums.py.
 *
 * @author MoBai

 */
public enum MemoryType {
    SEMANTIC("semantic_memory"),
    EPISODIC("episodic_memory"),
    PROCEDURAL("procedural_memory");

    private final String value;

    MemoryType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static MemoryType fromValue(String value) {
        for (MemoryType type : values()) {
            if (type.value.equals(value)) return type;
        }
        throw new IllegalArgumentException("Unknown memory type: " + value);
    }
}

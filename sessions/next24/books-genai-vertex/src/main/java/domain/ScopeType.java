package domain;

public enum ScopeType {
    PUBLIC("public"),
    PRIVATE("private");

    private final String value;

    ScopeType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ScopeType fromValue(String value) {
        for (ScopeType scope : ScopeType.values()) {
            if (scope.value.equalsIgnoreCase(value)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Unknown enum value : " + value);
    }
}

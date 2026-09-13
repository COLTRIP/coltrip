package com.coltrip.backend.domain.spot;

public enum Mode {
    COZY("아늑"),
    NATURAL("자연"),
    URBAN("도시"),
    VINTAGE("빈티지"),
    EXOTIC("이국"),
    VIBRANT("활기"),
    SENSORY("감각"),
    TRANQUIL("고요");

    private final String label;

    Mode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

package com.just_for_fun.dotlist.Task;

enum TextStyle {
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKETHROUGH,
    COLOR
}

public class NoteFormatting {
    private final int start;
    private final int end;
    private final TextStyle style;
    private final int color;

    public NoteFormatting(int start, int end, TextStyle style) {
        this(start, end, style, 0); // Default color (0 means no color)
    }


    public NoteFormatting(int start, int end, TextStyle style, int color) {
        this.start = start;
        this.end = end;
        this.style = style;
        this.color = color;
    }

    public int getStart() { return start; }
    public int getEnd() { return end; }
    public TextStyle getStyle() { return style; }
    public int getColor() { return color; }

    // Add equals and hashCode for proper comparison
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        NoteFormatting that = (NoteFormatting) obj;
        return start == that.start && end == that.end && style == that.style;
    }
}
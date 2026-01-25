package org.neatore.caliback.object;

import java.util.Objects;

public record SpecialDay(String name, String date, String type) {
    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof SpecialDay o_)) return false;

        // type을 비교하면 날짜가 중복되더라도 type이 달라서 Set에 그대로 들어갈 수 있으므로 비교하면 안됨
        // 어린이 날 -> 어린이날
        return o_.name.replaceAll("\\s+", "").equalsIgnoreCase(this.name.replaceAll(" ", "")) && o_.date.equals(this.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.replaceAll("\\s+", ""), date);
    }
}

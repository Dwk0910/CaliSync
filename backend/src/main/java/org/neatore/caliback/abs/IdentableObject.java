package org.neatore.caliback.abs;

public abstract class IdentableObject {
    public abstract String getId();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        else if (o instanceof IdentableObject obj) {
            return obj.getId().equals(getId());
        } else return false;
    }
}

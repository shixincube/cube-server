/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.util.calc;

import java.util.ArrayList;
import java.util.List;

public class FrameStructureDescription {

    public List<FrameStructure> frameStructures;

    public FrameStructureDescription() {
        this.frameStructures = new ArrayList<>();
    }

    public void addFrameStructure(FrameStructure structure) {
        if (this.frameStructures.contains(structure)) {
            return;
        }
        this.frameStructures.add(structure);
    }

    public boolean isTop() {
        return this.frameStructures.contains(FrameStructure.TopSpace);
    }

    public boolean isBottom() {
        return this.frameStructures.contains(FrameStructure.BottomSpace);
    }

    public boolean isLeft() {
        return this.frameStructures.contains(FrameStructure.LeftSpace);
    }

    public boolean isRight() {
        return this.frameStructures.contains(FrameStructure.RightSpace);
    }

    public boolean isCenterTopLeft() {
        return this.frameStructures.contains(FrameStructure.CenterTopLeftSpace);
    }

    public boolean isCenterTopRight() {
        return this.frameStructures.contains(FrameStructure.CenterTopRightSpace);
    }

    public boolean isCenterBottomLeft() {
        return this.frameStructures.contains(FrameStructure.CenterBottomLeftSpace);
    }

    public boolean isCenterBottomRight() {
        return this.frameStructures.contains(FrameStructure.CenterBottomRightSpace);
    }

    public boolean isTopLeftCorner() {
        return this.frameStructures.contains(FrameStructure.TopLeftCorner);
    }

    public boolean isTopRightCorner() {
        return this.frameStructures.contains(FrameStructure.TopRightCorner);
    }

    public boolean isBottomLeftCorner() {
        return this.frameStructures.contains(FrameStructure.BottomLeftCorner);
    }

    public boolean isBottomRightCorner() {
        return this.frameStructures.contains(FrameStructure.BottomRightCorner);
    }

    public boolean isNotInCorner() {
        return this.frameStructures.contains(FrameStructure.NotInCorner) ||
                (!this.isTopLeftCorner() && !this.isTopRightCorner() && !this.isBottomLeftCorner() && !this.isBottomRightCorner());
    }

    public boolean isInCorner() {
        return this.isTopLeftCorner() || this.isTopRightCorner() || this.isBottomLeftCorner() ||
                this.isBottomRightCorner();
    }
}

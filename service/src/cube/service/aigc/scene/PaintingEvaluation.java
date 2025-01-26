/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cube.aigc.psychology.Painting;
import cube.aigc.psychology.Reference;
import cube.aigc.psychology.composition.SpaceLayout;
import cube.vision.Size;

public class PaintingEvaluation {

    private Painting painting;

    private Size canvasSize;

    private SpaceLayout spaceLayout;

    private Reference reference;

    public PaintingEvaluation(Painting painting) {
        this.painting = painting;
        this.canvasSize = painting.getCanvasSize();
        this.spaceLayout = new SpaceLayout(painting);
        this.reference = Reference.Normal;
    }

    private double evalNormal() {
        
        return 0;
    }
}

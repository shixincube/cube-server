/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cube.aigc.psychology.Attribute;
import cube.aigc.psychology.Theme;
import cube.aigc.psychology.composition.Scale;
import cube.common.entity.AIGCChannel;
import cube.common.entity.FileLabel;
import cube.common.state.AIGCStateCode;

import java.util.List;

public class ComprehensiveReportWorker implements Runnable {

    private AIGCStateCode state = AIGCStateCode.Processing;

    public ComprehensiveReportWorker(AIGCChannel channel, Theme theme, List<Attribute> attributes,
                                     List<FileLabel> fileLabels, List<Scale> scales,
                                     ComprehensiveReportListener listener) {

    }

    @Override
    public void run() {

    }
}

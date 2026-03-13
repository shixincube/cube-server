/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.aigc.scene;

import cube.aigc.psychology.ComprehensiveReport;
import cube.aigc.psychology.Theme;
import cube.aigc.psychology.composition.Comprehensive;
import cube.common.entity.AIGCChannel;
import cube.service.aigc.AIGCService;

import java.util.List;

public class ComprehensiveReportWorker implements Runnable {

    private AIGCService service;

    private AIGCChannel channel;

    private ComprehensiveReportListener listener;

    private ComprehensiveReport report;

    public ComprehensiveReportWorker(AIGCService service, AIGCChannel channel, Theme theme,
                                     List<Comprehensive> comprehensives, ComprehensiveReportListener listener) {
        this.service = service;
        this.channel = channel;
        this.listener = listener;
        this.report = new ComprehensiveReport(theme, comprehensives);
    }

    public ComprehensiveReport getReport() {
        return this.report;
    }

    @Override
    public void run() {

    }
}

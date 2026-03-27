/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2026 Ambrose Xu.
 */

package cube.service.riskmgmt.plugin;

import cube.plugin.HookResult;
import cube.plugin.Plugin;
import cube.plugin.PluginContext;
import cube.service.riskmgmt.RiskManagement;

public class AIAutomaticSpeechRecognitionPlugin implements Plugin {

    private final RiskManagement service;

    public AIAutomaticSpeechRecognitionPlugin(RiskManagement service) {
        this.service = service;
    }

    @Override
    public void setup() {

    }

    @Override
    public void teardown() {

    }

    @Override
    public HookResult launch(PluginContext context) {
        return null;
    }
}

/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.guidance;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Guides {

    private final static File sRootPath = new File("assets/guidance/");

    private static Map<String, GuideFlow> sGuideFlowMap = new ConcurrentHashMap<>();

    public static List<GuideFlow> listGuideFlows() {
        File[] files = sRootPath.listFiles();
        if (null != files) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".json")) {
                    GuideFlow guideFlow = sGuideFlowMap.get(file.getName());
                    if (null == guideFlow || guideFlow.getLoadTimestamp() != file.lastModified()) {
                        sGuideFlowMap.put(file.getName(), new GuideFlow(file));
                    }
                }
            }
        }
        List<GuideFlow> result = new ArrayList<>();
        result.addAll(sGuideFlowMap.values());
        return result;
    }
}

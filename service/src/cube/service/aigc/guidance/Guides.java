/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.service.aigc.guidance;

import cell.util.log.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Guides {

    private final static File sRootPath = new File("assets/guidance/");

    private static Map<String, GuideFlow> sGuideFlowMap = new ConcurrentHashMap<>();

    public static List<GuideFlow> listGuideFlows() {
        refresh();
        return new ArrayList<>(sGuideFlowMap.values());
    }

    public static GuideFlow getGuideFlow(String name) {
        refresh();
        for (GuideFlow guideFlow : sGuideFlowMap.values()) {
            if (guideFlow.getName().equalsIgnoreCase(name)) {
                return new GuideFlow(guideFlow.toJSON());
            }
        }
        return null;
    }

    public static GuideFlow matchGuideFlow(List<String> words) {
        List<GuideFlow> list = listGuideFlows();
        int max = 0;
        GuideFlow guideFlow = null;
        for (GuideFlow gf : list) {
            int hit = gf.hitKeywords(words);
            if (hit > max) {
                max = hit;
                guideFlow = gf;
            }
        }
        return guideFlow;
    }

    private static void refresh() {
        File[] files = sRootPath.listFiles();
        if (null != files) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".json")) {
                    GuideFlow track = sGuideFlowMap.get(file.getName());
                    if (null == track || track.getLoadTimestamp() != file.lastModified()) {
                        try {
                            sGuideFlowMap.put(file.getName(), new GuideFlow(file));
                        } catch (Exception e) {
                            Logger.e(Guides.class, "#refresh", e);
                        }
                    }
                }
            }
        }
    }
}

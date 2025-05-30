/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.guidance;

public interface GuideFlowable {

    GuidanceSection getCurrentSection();

    Question getCurrentQuestion();

}

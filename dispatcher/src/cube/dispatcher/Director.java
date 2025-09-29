/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.dispatcher;

import cell.api.Speakable;
import cell.core.net.Endpoint;

import java.util.HashMap;

/**
 * 导演机。
 */
public class Director {

    public final Endpoint endpoint;

    public final Endpoint fileEndpoint;

    public final Scope scope;

    public Speakable speaker;

    protected HashMap<String, Section> sectionMap;

    public Director(Endpoint endpoint, Endpoint fileEndpoint, Scope scope) {
        this.endpoint = endpoint;
        this.fileEndpoint = fileEndpoint;
        this.scope = scope;
        this.sectionMap = new HashMap();

        for (String celletName : scope.cellets) {
            this.sectionMap.put(celletName, new Section());
        }
    }

    public Section getSection(String celletName) {
        return this.sectionMap.get(celletName);
    }


    /**
     * 选择区间。
     */
    public class Section {

        public int begin = 0;

        public int end = 0;

        public int totalWeight = 0;

        public Section() {
        }

        public boolean contains(int value) {
            if (value >= this.begin && value <= this.end) {
                return true;
            }
            else {
                return false;
            }
        }
    }
}

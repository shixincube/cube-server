/*
 * This source file is part of Cube.
 *
 * Copyright (c) 2023-2025 Ambrose Xu.
 */

package cube.aigc.psychology.composition;

import cube.aigc.psychology.Term;
import cube.common.JSONable;
import org.json.JSONObject;

public class DescriptionSuggestion implements JSONable {

    public Term term;

    public String description;

    public String behavior;

    public String suggestion;

    public DescriptionSuggestion(JSONObject json) {
        this.term = Term.parse(json.getString("term"));
        this.description = json.getString("description");
        this.behavior = json.getString("behavior");
        this.suggestion = json.getString("suggestion");
    }

    public DescriptionSuggestion(Term term, String description, String behavior, String suggestion) {
        this.term = term;
        this.description = description;
        this.behavior = behavior;
        this.suggestion = suggestion;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("term", this.term.word);
        json.put("description", this.description);
        json.put("behavior", this.behavior);
        json.put("suggestion", this.suggestion);
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}

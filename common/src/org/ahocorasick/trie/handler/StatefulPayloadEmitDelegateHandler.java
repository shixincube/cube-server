package org.ahocorasick.trie.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.PayloadEmit;

/**
 * Convenience wrapper class that delegates every method to a
 * {@link StatefulPayloadEmitHandler}.
 */
public class StatefulPayloadEmitDelegateHandler implements StatefulPayloadEmitHandler<String> {

    private StatefulEmitHandler handler;

    public StatefulPayloadEmitDelegateHandler(StatefulEmitHandler handler) {
        this.handler = handler;

    }

    private static List<PayloadEmit<String>> asEmits(Collection<Emit> emits) {
        List<PayloadEmit<String>> result = new ArrayList<>();
        for (Emit emit : emits) {
            result.add(new PayloadEmit<String>(emit.getStart(), emit.getEnd(), emit.getKeyword(), null));
        }
        return result;
    }

    @Override
    public boolean emit(PayloadEmit<String> emit) {
        Emit newEmit = new Emit(emit.getStart(), emit.getEnd(), emit.getKeyword());
        return handler.emit(newEmit);
    }

    @Override
    public List<PayloadEmit<String>> getEmits() {
        List<Emit> emits = this.handler.getEmits();
        return asEmits(emits);
    }
}

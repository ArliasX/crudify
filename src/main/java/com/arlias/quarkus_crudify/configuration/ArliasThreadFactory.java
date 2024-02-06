package com.arlias.quarkus_crudify.configuration;

import java.util.concurrent.ThreadFactory;

public class ArliasThreadFactory implements ThreadFactory {

    private final ThreadGroup group;

    public ArliasThreadFactory(long mainThreadId) {
        this.group = new ThreadGroup(String.valueOf(mainThreadId));
    }

    public ArliasThreadFactory(String uuid) {
        this.group = new ThreadGroup(uuid);
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(group, r);
    }
}

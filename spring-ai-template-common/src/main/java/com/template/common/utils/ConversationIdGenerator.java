package com.template.common.utils;

import cn.hutool.core.util.IdUtil;

public final class ConversationIdGenerator {

    private ConversationIdGenerator() {}

    public static String generate() {
        return "conv_" + IdUtil.fastSimpleUUID();
    }
}

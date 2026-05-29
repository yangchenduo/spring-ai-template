package com.template.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.template.app.entity.ChatHistoryEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistoryEntity> {
}

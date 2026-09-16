package com.ticketwallet.domain.spike;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SpikeItemMapper extends BaseMapper<SpikeItem> {

    /**
     * spike 方言用例②：抬头模糊检索走原生 ILIKE（tech-stack §3.3 待核实项②）。
     * 对照 Flex QueryWrapper.like() 的大小写行为（见 SpikeService#page）。
     */
    @Select("SELECT id, title, kind, amount, created_at FROM spike_items "
            + "WHERE title ILIKE #{pattern} ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<SpikeItem> selectByIlike(@Param("pattern") String pattern,
                                  @Param("limit") int limit,
                                  @Param("offset") int offset);
}

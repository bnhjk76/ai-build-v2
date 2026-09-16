package com.ticketwallet.domain.spike;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * spike 五项冒烟的业务载体：
 * 冒烟③事务（create 提交 / createThenRollback 回滚）、冒烟④分页（Flex PG 方言 LIMIT/OFFSET）、
 * 方言用例②ILIKE（@Select 原生条件对照 QueryWrapper.like 大小写行为）。
 */
@Service
public class SpikeService {

    private final SpikeItemMapper mapper;

    public SpikeService(SpikeItemMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public SpikeItem create(String title, SpikeKind kind, BigDecimal amount) {
        SpikeItem item = new SpikeItem();
        item.setTitle(title);
        item.setKind(kind);
        item.setAmount(amount);
        item.setCreatedAt(java.time.OffsetDateTime.now());
        mapper.insert(item);
        return item;
    }

    /** 冒烟③：插入后抛异常，验证 @Transactional 回滚（调用方捕获后核对行数不变）。 */
    @Transactional
    public void createThenRollback(String title, SpikeKind kind, BigDecimal amount) {
        create(title, kind, amount);
        throw new IllegalStateException("spike: tx-rollback-by-design");
    }

    /** 冒烟④：Flex 分页（PG 方言），titleLike 非空时叠加 QueryWrapper.like（观察大小写行为）。 */
    public Page<SpikeItem> page(int pageNumber, int pageSize, String titleLike) {
        QueryWrapper qw = QueryWrapper.create();
        if (titleLike != null && !titleLike.isBlank()) {
            qw.like("title", "%" + titleLike + "%");
        }
        qw.orderBy("id", false);
        return mapper.paginate(pageNumber, pageSize, qw);
    }

    public List<SpikeItem> ilike(String keyword, int limit, int offset) {
        return mapper.selectByIlike("%" + keyword + "%", limit, offset);
    }

    public long count() {
        return mapper.selectCountByQuery(QueryWrapper.create());
    }
}

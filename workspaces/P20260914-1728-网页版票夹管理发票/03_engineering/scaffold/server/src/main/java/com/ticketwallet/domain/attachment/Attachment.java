package com.ticketwallet.domain.attachment;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/** attachments 表（ER §5.1：user_id 冗余为越权双保险）。 */
@Getter
@Setter
@Table("attachments")
public class Attachment {
    @Id
    private String id;
    private String invoiceId;
    private String userId;
    private String filename;
    private String mimeType;
    private Integer size;
    private String storageKey;
    private OffsetDateTime createdAt;
}

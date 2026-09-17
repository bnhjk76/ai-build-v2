package com.ticketwallet.domain.invoice;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** invoices 表（ER §5.1；totalAmount 冗余=服务端 BigDecimal 重算，不信前端）。 */
@Getter
@Setter
@Table("invoices")
public class Invoice {

    @Id
    private String id;

    private String userId;

    private String invoiceCode;

    private String invoiceNumber;

    private LocalDate issuedDate;

    private String title;

    private BigDecimal amount;

    private BigDecimal taxAmount;

    private BigDecimal totalAmount;

    private InvoiceCategory category;

    private InvoiceMedium medium;

    private InvoiceStatus status;

    private String remark;

    private OffsetDateTime deletedAt;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}

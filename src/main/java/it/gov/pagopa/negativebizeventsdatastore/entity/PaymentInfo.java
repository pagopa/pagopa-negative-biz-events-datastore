package it.gov.pagopa.negativebizeventsdatastore.entity;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInfo {
	private String paymentDateTime;
	private String dueDate;
	private String paymentToken;
	private BigDecimal amount;
	private Long totalNotice;
	private String paymentMethod;
    private String touchpoint;
    private String remittanceInformation;
    private List<MapEntry> metadata;	
}

package it.gov.pagopa.negativebizeventsdatastore.entity;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

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
public class BizEvent {
	private String id;
	private String version;
	private String businessProcess;
	private String useCase;
	private boolean reAwakable;
	private boolean complete;
	private List<String> missingInfo;
	private DebtorPosition debtorPosition;
	private Creditor creditor;
	private Psp psp;
	private Debtor debtor;
	private PaymentInfo paymentInfo;
	private List<Transfer> transferList;
	private Map<String, Object> properties;
	
	// internal management field
	@Builder.Default
	private Long timestamp = ZonedDateTime.now().toInstant().toEpochMilli(); 
}

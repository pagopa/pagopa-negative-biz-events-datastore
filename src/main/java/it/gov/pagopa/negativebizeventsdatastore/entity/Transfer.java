package it.gov.pagopa.negativebizeventsdatastore.entity;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transfer {
	private String idTransfer;
	private String fiscalCodePA;
	private String companyName;
	private BigDecimal amount;
	private String transferCategory;
	private String remittanceInformation;
	private String IBAN;
	private Boolean MBD;
	private List<MapEntry> metadata;
}

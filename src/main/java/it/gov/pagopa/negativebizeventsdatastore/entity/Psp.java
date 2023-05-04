package it.gov.pagopa.negativebizeventsdatastore.entity;

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
public class Psp {
	private String idPsp;
	private String idBrokerPsp;
	private String idChannel;
	private String psp;
	private String pspPartitaIVA;
	private String pspFiscalCode;
	private String channelDescription;
}

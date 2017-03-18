package eu.cryptoeuro.service

import eu.cryptoeuro.rest.model.Balance
import eu.cryptoeuro.rest.model.Currency
import org.springframework.beans.factory.annotation.Autowired

class SupplyServiceSpec extends WireMockBaseSpec {

    @Autowired
    SupplyService supplyService

    def "getTotalSupply: get total supply"() {
        when:
        Balance balance = supplyService.getTotalSupply()
        then:
        balance.amount.compareTo(new Long(0)).equals(1)
        balance.currency.equals(Currency.EUR_CENT)
    }

}

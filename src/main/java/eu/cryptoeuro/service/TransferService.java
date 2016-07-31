package eu.cryptoeuro.service;

import eu.cryptoeuro.dao.TransferRepository;
import eu.cryptoeuro.rest.model.Transfer;
import eu.cryptoeuro.rest.command.CreateTransferCommand;
import eu.cryptoeuro.rest.model.Fee;
import eu.cryptoeuro.rest.model.TransferStatus;
import eu.cryptoeuro.service.exception.AccountNotApprovedException;
import eu.cryptoeuro.service.rpc.EthereumRpcMethod;
import eu.cryptoeuro.service.rpc.JsonRpcCallMap;
import eu.cryptoeuro.service.rpc.JsonRpcResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class TransferService extends BaseService {

    @Autowired
    AccountService accountService;

    public Transfer transfer(CreateTransferCommand transfer){

        checkSourceAccountApproved(transfer.getSourceAccount());

        Map<String, String> params = new HashMap<>();
        params.put("from", SPONSOR);
        params.put("to", CONTRACT);
        params.put("nounce", "1"); // TODO: changeme
        params.put("gas", "0x249F0"); // 150000
        params.put("gasPrice", "0x4A817C800"); // 20000000000
        //params.put("value", "");

        String to = String.format("%64s", transfer.getTargetAccount().substring(2)).replace(" ", "0");
        String amount = String.format("%064X", transfer.getAmount() & 0xFFFFF);

        String fee = String.format("%064X", Fee.amount & 0xFFFFF);
        String nounce = String.format("%064X", 0 & 0xFFFFF);

        String sponsor = String.format("%64s", SPONSOR.substring(2)).replace(" ", "0");

        String v = String.format("%016X", transfer.getV() & 0xFFFFF);;
        String r = "";
        String s = "";

        String data = "0x" + HashUtils.keccak256(
                "signedTransfer(address,uint256,uint256,uint256,\n" +
                "address,\n" +
                "uint8,bytes32,bytes32)").substring(0, 8)
                + to + amount + fee +
                nounce + sponsor +
                v + r + s;

        params.put("data", data);
        //params.put("nonce", "");

        JsonRpcCallMap call = new JsonRpcCallMap(EthereumRpcMethod.sendTransaction, Arrays.asList(params));

        log.info("JSON:\n"+call.toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        HttpEntity<String> request = new HttpEntity<String>(call.toString(), headers);
        JsonRpcResponse response = restTemplate.postForObject(URL, request, JsonRpcResponse.class);

        log.info("Send transaction response: " + response.getResult());

        response.getResult();

        Transfer transferResponse = new Transfer();
        transferResponse.setId(response.getResult());
        transferResponse.setStatus(TransferStatus.PENDING);
        transferResponse.setAmount(transfer.getAmount());
        transferResponse.setTargetAccount(transfer.getTargetAccount());
        transferResponse.setSourceAccount(transfer.getSourceAccount());
        transferResponse.setFee(transfer.getFee());

        return transferResponse;
    }

    public Transfer get(Long id){
        return null;
    }

    public Iterable<Transfer> getAll(){
        return null;
    }

    private void checkSourceAccountApproved(String account) {
        if(!accountService.isApproved(account)) {
            throw new AccountNotApprovedException();
        }
    }

    public String sendTransaction(Optional<String> account, Optional<Long> amount) {
        // DOCS: https://github.com/ethcore/parity/wiki/JSONRPC#eth_sendtransaction
        Map<String, String> params = new HashMap<>();
        params.put("from", "0x4FfAaD6B04794a5911E2d4a4f7F5CcCEd0420291"); // erko main account
        params.put("to", "0xAF8ce136A244dB6f13a97e157AC39169F4E9E445"); // viimane 0.21 contract deploy
        //params.put("gas", "0x76c0"); // 30400, 21000
        //params.put("gasPrice", "0x9184e72a000"); // 10000000000000
        //params.put("value", "");

        String targetArgument = "000000000000000000000000" + account.orElse("0x52C312631d5593D9164A257abcD5c58d14B96600").substring(2); // erko wallet contract aadress
        // TODO use amount variable
        String amountArgument = "0000000000000000000000000000000000000000000000000000000000000065"; // 101 raha
        String data = "0x" + HashUtils.keccak256("mintToken(address,uint256)").substring(0, 8) + targetArgument + amountArgument;

        params.put("data", data);
        //params.put("nonce", "");

        JsonRpcCallMap call = new JsonRpcCallMap(EthereumRpcMethod.sendTransaction, Arrays.asList(params));

        log.info("JSON:\n"+call.toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        HttpEntity<String> request = new HttpEntity<String>(call.toString(), headers);
        JsonRpcResponse response = restTemplate.postForObject(URL, request, JsonRpcResponse.class);

        log.info("Send transaction response: " + response.getResult());

        /*
        byte[] bytes = DatatypeConverter.parseHexBinary(response.getResult().substring(2));
        String convertedResult = new String(bytes, StandardCharsets.UTF_8);
        log.info("Converted to string: " + convertedResult);

        long longResult = Long.parseLong(response.getResult().substring(2), 16);
        log.info("Converted to long: " + longResult);
        */

        return response.getResult();
    }

}

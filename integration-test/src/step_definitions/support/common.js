const axios = require("axios");


function get(url, headers) {
    return axios.get(url, {headers})
        .then(res => {
            return res;
        })
        .catch(error => {
            return error.response;
        });
}

function post(url, body, headers) {
    return axios.post(url, body, {headers})
        .then(res => {
            return res;
        })
        .catch(error => {
            return error.response;
        });
}

function put(url, body, headers) {
    return axios.put(url, body, {headers})
        .then(res => {
            return res;
        })
        .catch(error => {
            return error.response;
        });
}

function del(url, headers) {
    return axios.delete(url, {headers})
        .then(res => {
            return res;
        })
        .catch(error => {
            return error.response;
        });
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function createNegativeBizEvent(id, reAwakable) {
    json_event = {
         "id": id,
         "version": "2",
         "businessProcess": "mod3CancelV1ActorPerRequest",
         "useCase": "13",
         "reAwakable": reAwakable,
         "complete": false,
         "missingInfo": [
             "creditor.officeName",
             "paymentInfo.dueDate",
             "paymentInfo.paymentMethod",
             "paymentInfo.metadata"
         ],
         "debtorPosition": {
             "modelType": "2",
             "noticeNumber": "002696320585123674",
             "iuv": "696320585123674"
         },
         "creditor": {
             "idPA": "12345678900",
             "idBrokerPA": "intPaTest",
             "idStation": "stazioneTestOld",
             "companyName": "pa DEV ragsoc"
         },
         "psp": {
             "idPsp": "pspTest",
             "idBrokerPsp": "intPspTest",
             "idChannel": "canaleTest",
             "psp": "Test-PSP",
             "pspPartitaIVA": "vat",
             "pspFiscalCode": "pspTest",
             "channelDescription": "NA"
         },
         "debtor": {
             "fullName": "Utente Test",
             "entityUniqueIdentifierType": "F",
             "entityUniqueIdentifierValue": "TTTTTT11T11T123T"
         },
         "paymentInfo": {
             "paymentDateTime": "2023-05-03T10:07:22.703673",
             "paymentToken": "289f3aa8a3084c649f3d09328bba80e2",
             "amount": 100,
             "totalNotice": 1,
             "touchpoint": "NA",
             "remittanceInformation": "pagamento multibeneficiario"
         },
         "transferList": [
             {
                 "idTransfer": "1",
                 "fiscalCodePA": "12345678900",
                 "companyName": "pa DEV ragsoc",
                 "amount": 100,
                 "transferCategory": "9/tipodovuto_7",
                 "remittanceInformation": "CAUSALE 1CAUSALE 2",
                 "IBAN": "IT96R0123454321000000012345",
                 "MBD": false
             }
         ],
         "properties": {
             "id": "3TmsQyYmlXl0zbtU",
             "event-type": "test-event",
             "priority": "3TmsQyYmlXl0zbtU",
             "score": 9
         },
         "timestamp": 1683277698913,
         "_rid": "Wv4HAPIonFUZAAAAAAAAAA==",
         "_self": "dbs/Wv4HAA==/colls/Wv4HAPIonFU=/docs/Wv4HAPIonFUZAAAAAAAAAA==/",
         "_etag": "\"0300ad50-0000-0d00-0000-6454c7820000\"",
         "_attachments": "attachments/",
         "_ts": 1683277698
        }
    return json_event
}


module.exports = {
    get, post, put, del, createNegativeBizEvent, sleep
}

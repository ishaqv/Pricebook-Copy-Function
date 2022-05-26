# pricebook-copy-fun
Running event grid trigger function locally
```
curl --location --request POST 'http://localhost:7071/runtime/webhooks/eventgrid?functionName=pricebookCopy' \
--header 'aeg-event-type: Notification' \
--header 'Content-Type: application/json' \
--data-raw '[{
  "topic": "/subscriptions/{subscriptionid}/resourceGroups/eg0122/providers/Microsoft.Storage/storageAccounts/egblobstore",
  "subject": "/blobServices/default/containers/pricebooks/blobs/circlek-us/2705549/2705549.xml.gz",
  "eventType": "Microsoft.Storage.BlobCreated",
  "eventTime": "2018-01-23T17:02:19.6069787Z",
  "id": "{guid}",
  "data": {
    "api": "PutBlockList",
    "clientRequestId": "{guid}",
    "requestId": "{guid}",
    "eTag": "0x8D562831044DDD0",
    "contentType": "application/octet-stream",
    "contentLength": 2248,
    "blobType": "BlockBlob",
    "url": "https://ngrpdevstorage.blob.core.windows.net/pricebooks/circlek-us/1234567/2021/08/02/1234567-1627892265687.gzip",
    "sequencer": "000000000000272D000000000003D60F",
    "storageDiagnostics": {
      "batchId": "{guid}"
    }
  },
  "dataVersion": "",
  "metadataVersion": "1"
}]'
```
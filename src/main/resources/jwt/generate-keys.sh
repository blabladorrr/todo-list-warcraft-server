#!/bin/sh
openssl genrsa -out rsa-private-key.pem 2048
openssl pkcs8 -topk8 -nocrypt -inform pem -in rsa-private-key.pem -outform pem -out private-key.pem
openssl rsa -pubout -in rsa-private-key.pem -out public-key.pem

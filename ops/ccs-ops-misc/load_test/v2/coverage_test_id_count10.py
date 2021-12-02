import csv
import datetime
import os
import sys
import urllib3
import common.config as config
import common.test_setup as setup
from locust import HttpUser, task

server_public_key = setup.loadServerPublicKey()

'''
If there is no server cert, the warnings are disabled because thousands will appear in the logs and make it difficult
to see anything else.
'''
if not server_public_key:
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

eob_ids = setup.generateAndLoadIds()
client_cert = setup.getClientCert()

class BFDUser(HttpUser):
    @task
    def coverage(self):
        if len(eod_ids) == 0
            print("Ran out of data, stopping test...")
            raise StopLocust()

        id = eob_ids.pop()
        self.client.get(f'/v2/fhir/Coverage?beneficiary={id}&_count=10',
                cert=client_cert,
                verify=server_public_key,
                name='/v2/fhir/Coverage')
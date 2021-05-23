#!/usr/bin/env python
from os import listdir
from os.path import isfile, join
import csv
import json
import persistqueue
import requests
import shutil
import time
import calendar
import math
import datetime
import re

PROCESSING_QUEUE_NAME = 'faridabad-monitoring'

scanpath='upload_faridabad_hi_monitoring'
processedPath='processed_faridabad_hi_monitoring'

csvFileNames = [f for f in listdir(scanpath) if isfile(join(scanpath, f))]
jobQueue = persistqueue.UniqueAckQ(PROCESSING_QUEUE_NAME)
statesmanUrl = "http://localhost:8080"
foxtrotUrl = "http://localhost:8082"
date_fields = ['date_of_isolation']
phones = set()
rows = []

stateWorkflows = {
    "haryana": "94f340e5-76d6-416f-9e95-8b8532e3722d"
}

TEN_DAYS_IN_MS = 10 * 24 * 60 * 60 * 1000
CURRENT_DATE = datetime.date.today()
DAY_START_TIME = (int(datetime.datetime(CURRENT_DATE.year, CURRENT_DATE.month, CURRENT_DATE.day, 0, 0, 0).strftime('%s'))) * 1000

mandatoryFields = ['icmr_id','patient_name', 'mobile_number', 'district', 'age', 'gender', 'state', 'date_of_isolation']

def now():
    return calendar.timegm(time.gmtime()) * 1000

def epoch_time(str_time):
    return (int(time.mktime(time.strptime(str_time, "%d/%m/%Y")))) * 1000

def str_date(time_in_epoch):
    return datetime.datetime.fromtimestamp(time_in_epoch / 1000).strftime('%d/%m/%Y')

def day_diff(from_epoch, till_epoch):
    if(till_epoch < from_epoch):
        return 1
    return int (math.ceil( (float)(till_epoch - from_epoch) / 86400000))

def sanitizeAge(row):
    if(row.has_key("age")):
        age = row['age'].strip().lower()
        age = age.split('.')[0].split(" ")[0].split("y")[0]
        row['age'] = age

def trigger_new_workflow(payload,mobileNumber,wfSource):
    for i in range(3):
        r = requests.post(statesmanUrl + '/callbacks/ingress/raw/'+wfSource, data=payload, headers = {'content-type': 'application/json'})
        if r.status_code == 200:
            print('successfully posted data for mobileNumber: ' + mobileNumber)
            return True
        else:
            print(str(i) + ': could not post data for mobileNumber: ' + mobileNumber + ' status:' + str(r.status_code))
    return False

def update_or_trigger_new_workflow(w, payload, mobileNumber, wfSource):
    for i in range(3):
        r = requests.get(statesmanUrl + '/v1/housekeeping/debug/workflow/'+w, headers = {'content-type': 'application/json'})
        if(r.status_code != 200):
            print(str(i) + ': could not get data for mobileNumber: ' + mobileNumber + ' workflowId '+ w +  ' status:' + str(r.status_code))
        else:
            workflow = r.json()
            wd = workflow['dataObject']['data']
            if(wd["endTime"] >= payload['body']["endTime"]):
                print("Nothing to update for mobileNumber:" + mobileNumber + ' workflowId:'+ w )
                return True
            else:
                wd["endTime"] = payload['body']["endTime"]
                wd["end_date"] = payload['body']["end_date"]
                wd["maxDays"] = day_diff(workflow['created'], wd["endTime"])
                if(workflow['dataObject']['currentState']['name'] == "END" or workflow['dataObject']['currentState']['name'] == "FORCE_END"):
                    return trigger_new_workflow(json.dumps(payload),mobileNumber,wfSource)
                else:
                    r = requests.put(statesmanUrl + '/v1/housekeeping/update/workflow',data = json.dumps(workflow) , headers = {'content-type': 'application/json'})
                    if(r.status_code == 200):
                        print("Updated for mobileNumber:" + mobileNumber + ' workflowId:'+ w  )
                        return True
                    else:
                        print(str(i) + ': could not update data for mobileNumber: ' + mobileNumber + ' workflowId:'+ w +  ' status:' + str(r.status_code))
    return False

def sanatizeKey(key):
    key = " ".join(key.split()).lower().strip().replace(' ', '_')
    return re.sub(r'\W+', '', key)

def sanitize_gender(convrow):
    if convrow.has_key("gender"):
        convrow['gender'] = convrow['gender'].strip().lower()

def sanitize_address(convrow):
    if convrow.has_key("address"):
        convrow['address'] = re.sub(r'\W+', ' ', convrow['address'])

for csvFileName in csvFileNames:
    fqFileName=scanpath + '/' + csvFileName
    fqDestFilename=processedPath + '/' + csvFileName
    try:
        with open(fqFileName) as csvfile:
            reader = csv.DictReader(csvfile)
            for row in reader:
                try:
                    convrow = dict((sanatizeKey(k), v.strip().replace('\n', "").replace('\r',"")) for k,v in row.iteritems())
                    if(convrow.has_key("")):
                        del convrow[""]
                    empty_keys = [k for k,v in convrow.iteritems() if len(str(v)) == 0 or v is None or v.lower() == 'none' ]
                    for k in empty_keys:
                        del convrow[k]
                    if convrow.has_key("date_of_isolation") and not convrow.has_key("end_date"):
                        convrow["end_date"] = str_date(epoch_time(convrow['date_of_isolation']) + TEN_DAYS_IN_MS)
                    invalid_schema = False
                    for f in mandatoryFields:
                        if not convrow.has_key(f):
                            print("ERROR: missing field:"+f + " for row:"+ str(row))
                            invalid_schema = True
                            break
                    if invalid_schema:
                        continue

                    convrow['mobile_number'] = convrow['mobile_number'].split(",")[0].strip()[-10:]

                    if(convrow['mobile_number'] in phones):
                        print("INFO: Already processed the mobile_number:" + convrow['mobile_number'])
                        continue
                    phones.add(convrow['mobile_number'])
                    for date_field in date_fields:
                        if convrow.has_key(date_field):
                            convrow[date_field] = convrow[date_field].replace('.',r'/').replace('-',r'/')
                            try:
                                convrow[date_field] = epoch_time(convrow[date_field])
                            except:
                                print("ERROR: invalid date format for field:"+date_field + " for row:"+ str(row))
                                invalid_schema = True
                                break
                    if(invalid_schema):
                        continue
                    sanitizeAge(convrow)
                    sanitize_gender(convrow)
                    sanitize_address(convrow)
                    convrow['wfSource'] = 'faridabad_csv'
                    convrow['flowType'] = 'faridabad_hi'
                    convrow['state'] = 'haryana'
                    convrow['now'] = DAY_START_TIME
                    convrow['endTime'] = epoch_time(convrow['end_date'])
                    body = { 'id' : convrow['wfSource'] , 'body' : convrow, 'apiPath' : csvFileName }
                    print('Queuing job mobile_number: ' + convrow['mobile_number'] + " name: " + convrow['patient_name'])
                    jobQueue.put(json.dumps(body))
                except Exception as e:
                    print('Error processing row: ' + str(row))
                    print(e)
    except e:
        print('Error processing file: ' + fqFileName)
    print('Items queued for file [' + csvFileName + ']: ' + str(jobQueue.size))
    shutil.move(fqFileName, fqDestFilename)
    print('Moved file ' + fqFileName + ' to ' + fqDestFilename)
print('Total queue size: ' + str(jobQueue.size))

while jobQueue.size > 0:
    payload = jobQueue.get()
    try:
        payloadDict = json.loads(payload)
        mobileNumber = str(payloadDict['body']['mobile_number'])
        state = str(payloadDict['body']['state'])
        wfSource = str(payloadDict['body']['wfSource'])
        if trigger_new_workflow(payload, mobileNumber, wfSource):
            jobQueue.ack(payload)
        else:
            jobQueue.ack_failed(payload)
    except Exception as e:
        print('Error processing job: ' + str(payload))
        jobQueue.ack_failed(json.dumps(payload))


shutil.rmtree(PROCESSING_QUEUE_NAME)
print('Processing complete')
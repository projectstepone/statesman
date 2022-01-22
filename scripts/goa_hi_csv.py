from os import listdir
from os.path import isfile, join
import csv
import json
import persistqueue
import requests
import shutil

scan_path = 'uploads'
processed_path = 'processed'

csv_file_names = [f for f in listdir(scan_path) if isfile(join(scan_path, f))]

job_queue = persistqueue.UniqueAckQ('csv-processor')

for csv_file_name in csv_file_names:
    fq_file_name = join(scan_path, csv_file_name)
    fq_dest_filename = join(processed_path, csv_file_name)

    try:
        with open(fq_file_name) as csv_file:
            reader = csv.DictReader(csv_file)
            for row in reader:
                try:
                    converted_row = dict(("_".join(k.lower().split()), v.strip()) for k, v in row.items())
                    if "" in converted_row.keys():
                        del converted_row[""]

                    converted_row['positive_confirmation_date'] = converted_row['positive_test_confirmation_date']
                    del converted_row['positive_test_confirmation_date']
                    converted_row['wfSource'] = 'goa_hi_monitoring_csv'

                    job_queue.put(json.dumps({"body": converted_row}))
                    print('Queued job: ' + converted_row['icmr_id'])
                except Exception as e:
                    print('Error processing row: ', str(row), e)
    except Exception as e:
        print('Error processing file: ', fq_file_name, e)
    print('Items queued for file [' + csv_file_name + ']: ' + str(job_queue.size))
    shutil.move(fq_file_name, fq_dest_filename)
    print('Moved file ' + fq_file_name + ' to ' + fq_dest_filename)
print('Total queue size: ' + str(job_queue.size))
#
while job_queue.size > 0:
    payload = job_queue.get()

    print(payload)

    icmr_id = str(json.loads(payload)['body']['icmr_id'])

    for i in range(3):
        r = requests.post('http://localhost/callbacks/ingress/raw/goa_hi_monitoring_csv', data=payload,
                          headers={'content-type': 'application/json'})
        if r.status_code == 200:
            print('successfully posted data for icmr id: ' + icmr_id)
            job_queue.ack(payload)
            break
        else:
            print(str(i) + ': could not post data for icmr id: ' + icmr_id + ' status:' + str(r.status_code))
            if i == 2:
                print('Failing for icmr id: ' + icmr_id)
                job_queue.ack_failed(payload)
            else:
                print('Retrying for icmr id: ' + icmr_id)
                job_queue.nack(payload)
                break
shutil.rmtree('csv-processor')
print('Processing complete')

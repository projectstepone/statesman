import json
import requests

with open('../configs/actions/actions.json') as f:
  data = json.load(f)

for x in data:
	response = requests.post("http://localhost:8080/v1/templates/action", json = x, headers = {}, stream=True)
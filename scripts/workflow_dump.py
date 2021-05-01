import json
import requests
import argparse

STATESMAN_HOST = "http://localhost:8080"
GET_WORKFLOW_TEMPLATE_PATH = '/v1/templates/workflow/{}'
GET_WORKFLOW_TRANSITIONS_PATH = '/v1/templates/workflow/{}/transitions'
GET_WORKFLOW_ACTION_PATH = '/v1/templates/action/{}'

def save_workflow_template(templateId):
    workflow_template_response = requests.get('{}{}'.format(STATESMAN_HOST, GET_WORKFLOW_TEMPLATE_PATH.format(templateId)))
    workflow_template = workflow_template_response.json()
    workflow_template_file = open('{}_workflow_template.json'.format(templateId), 'w')
    workflow_template_file.write(json.dumps(workflow_template, indent=2))
    workflow_template_file.close()

def save_workflow_transitions(templateId):
    workflow_transitions_response = requests.get('{}{}'.format(STATESMAN_HOST, GET_WORKFLOW_TRANSITIONS_PATH.format(templateId)))
    workflow_transitions = workflow_transitions_response.json()
    workflow_transitions_file = open('{}_workflow_transitions.json'.format(templateId), 'w')
    workflow_transitions_file.write(json.dumps(workflow_transitions, indent=2))
    workflow_transitions_file.close()
    action_ids = set([transition['action'] for transition in workflow_transitions if 'action' in transition ])
    save_workflow_actions(templateId, action_ids)

def save_workflow_actions(templateId, action_ids):
    workflow_actions_file = open('{}_workflow_actions.json'.format(templateId), 'w')
    already_added_actions = set()
    actions = []
    for action_id in action_ids:
        if action_id not in already_added_actions:
            workflow_action_response = get_workflow_action(action_id)
            workflow_action = workflow_action_response.json()
            actions.append(workflow_action)
            if workflow_action['type'] == 'COMPOUND':
                action_templates = workflow_action['actionTemplates']
                for action_template in action_templates:
                    if action_template not in already_added_actions:
                        workflow_template_action_response = get_workflow_action(action_template)
                        workflow_template_action = workflow_template_action_response.json()
                        actions.append(workflow_template_action)
                        already_added_actions.add(action_template)
            already_added_actions.add(action_id)
    workflow_actions_file.write(json.dumps(actions, indent=2))
    workflow_actions_file.close()

def get_workflow_action(action_id):
    return requests.get('{}{}'.format(STATESMAN_HOST, GET_WORKFLOW_ACTION_PATH.format(action_id)))

parser = argparse.ArgumentParser()
parser.add_argument("-t",   "--templateId", required=True,  help="templateId")
options = parser.parse_args()
templateId = options.templateId
save_workflow_template(templateId)
save_workflow_transitions(templateId)
print('workflow: {} dumped'.format(templateId))
